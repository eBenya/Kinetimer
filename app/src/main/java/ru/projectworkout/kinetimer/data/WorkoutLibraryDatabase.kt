package ru.projectworkout.kinetimer.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ru.projectworkout.kinetimer.domain.model.Exercise
import ru.projectworkout.kinetimer.domain.model.MovementPhase
import ru.projectworkout.kinetimer.domain.model.WorkoutBlock

class ExerciseInUseException : Exception("Сначала удалите упражнение из своих блоков.")

class WorkoutLibraryDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE exercises (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                technique TEXT NOT NULL,
                sets INTEGER NOT NULL CHECK(sets > 0),
                repetitions INTEGER NOT NULL CHECK(repetitions > 0),
                built_in INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE phases (
                exercise_id TEXT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
                position INTEGER NOT NULL,
                title TEXT NOT NULL,
                duration_seconds INTEGER NOT NULL CHECK(duration_seconds > 0),
                PRIMARY KEY(exercise_id, position)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE workout_blocks (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                built_in INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE block_exercises (
                block_id TEXT NOT NULL REFERENCES workout_blocks(id) ON DELETE CASCADE,
                exercise_id TEXT NOT NULL REFERENCES exercises(id) ON DELETE RESTRICT,
                position INTEGER NOT NULL,
                PRIMARY KEY(block_id, position)
            )
            """.trimIndent(),
        )

        WorkoutRepository.allExercises.forEach { insertExercise(db, it, builtIn = true) }
        WorkoutRepository.builtInBlocks.forEach { block ->
            insertBlock(db, block.copy(exerciseIds = emptyList()))
            block.exerciseIds.forEachIndexed { index, exerciseId ->
                insertBlockExercise(db, block.id, exerciseId, index)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun getExercises(): List<Exercise> {
        val db = readableDatabase
        val exercises = mutableListOf<Exercise>()
        db.query("exercises", null, null, null, null, null, "built_in DESC, name COLLATE NOCASE").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                exercises +=
                    Exercise(
                        id = id,
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        technique =
                            cursor.getString(cursor.getColumnIndexOrThrow("technique"))
                                .split('\n')
                                .filter(String::isNotBlank),
                        sets = cursor.getInt(cursor.getColumnIndexOrThrow("sets")),
                        repetitions = cursor.getInt(cursor.getColumnIndexOrThrow("repetitions")),
                        phases = getPhases(db, id),
                    )
            }
        }
        return exercises
    }

    fun getBlocks(): List<WorkoutBlock> {
        val blocks = mutableListOf<WorkoutBlock>()
        readableDatabase.query(
            "workout_blocks",
            null,
            null,
            null,
            null,
            null,
            "built_in DESC, name COLLATE NOCASE",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                blocks +=
                    WorkoutBlock(
                        id = id,
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        exerciseIds = getBlockExerciseIds(id),
                        isBuiltIn = cursor.getInt(cursor.getColumnIndexOrThrow("built_in")) == 1,
                    )
            }
        }
        return blocks
    }

    fun saveExercise(exercise: Exercise) {
        requireValid(exercise)
        val db = writableDatabase
        db.beginTransaction()
        try {
            val existing = db.query("exercises", arrayOf("built_in"), "id = ?", arrayOf(exercise.id), null, null, null)
            val (exerciseExists, isBuiltIn) =
                existing.use {
                    if (it.moveToFirst()) {
                        true to (it.getInt(it.getColumnIndexOrThrow("built_in")) == 1)
                    } else {
                        false to false
                    }
                }

            val values =
                ContentValues().apply {
                    put("id", exercise.id)
                    put("name", exercise.name.trim())
                    put("technique", exercise.technique.joinToString("\n") { it.trim() })
                    put("sets", exercise.sets)
                    put("repetitions", exercise.repetitions)
                    put("built_in", if (isBuiltIn) 1 else 0)
                }
            if (exerciseExists) {
                db.update("exercises", values, "id = ?", arrayOf(exercise.id))
            } else {
                db.insertOrThrow("exercises", null, values)
            }
            db.delete("phases", "exercise_id = ?", arrayOf(exercise.id))
            exercise.phases.forEachIndexed { index, phase -> insertPhase(db, exercise.id, index, phase) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteExercise(exerciseId: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.query(
                "exercises",
                arrayOf("built_in"),
                "id = ?",
                arrayOf(exerciseId),
                null,
                null,
                null,
            ).use { cursor ->
                if (!cursor.moveToFirst() || cursor.getInt(cursor.getColumnIndexOrThrow("built_in")) == 1) {
                    return
                }
            }
            db.query(
                "block_exercises",
                arrayOf("block_id"),
                "exercise_id = ?",
                arrayOf(exerciseId),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor.moveToFirst()) throw ExerciseInUseException()
            }
            db.delete("exercises", "id = ?", arrayOf(exerciseId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun saveBlock(block: WorkoutBlock) {
        require(block.name.isNotBlank()) { "Введите название блока." }
        require(block.exerciseIds.isNotEmpty()) { "Добавьте в блок хотя бы одно упражнение." }
        require(block.exerciseIds.distinct().size == block.exerciseIds.size) {
            "Одно упражнение нельзя добавить в блок дважды."
        }

        val db = writableDatabase
        db.beginTransaction()
        try {
            val existing = db.query("workout_blocks", arrayOf("built_in"), "id = ?", arrayOf(block.id), null, null, null)
            val isBuiltIn =
                existing.use {
                    it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow("built_in")) == 1
                }
            check(!isBuiltIn) { "Встроенные блоки нельзя редактировать." }

            val values =
                ContentValues().apply {
                    put("id", block.id)
                    put("name", block.name.trim())
                    put("built_in", 0)
                }
            val updated =
                db.update(
                    "workout_blocks",
                    values,
                    "id = ? AND built_in = 0",
                    arrayOf(block.id),
                )
            if (updated == 0) db.insertOrThrow("workout_blocks", null, values)
            db.delete("block_exercises", "block_id = ?", arrayOf(block.id))
            block.exerciseIds.forEachIndexed { index, exerciseId ->
                insertBlockExercise(db, block.id, exerciseId, index)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteBlock(blockId: String) {
        writableDatabase.delete("workout_blocks", "id = ? AND built_in = 0", arrayOf(blockId))
    }

    private fun getPhases(db: SQLiteDatabase, exerciseId: String): List<MovementPhase> {
        val phases = mutableListOf<MovementPhase>()
        db.query(
            "phases",
            arrayOf("title", "duration_seconds"),
            "exercise_id = ?",
            arrayOf(exerciseId),
            null,
            null,
            "position",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                phases +=
                    MovementPhase(
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        durationSeconds = cursor.getInt(cursor.getColumnIndexOrThrow("duration_seconds")),
                    )
            }
        }
        return phases
    }

    private fun getBlockExerciseIds(blockId: String): List<String> {
        val exerciseIds = mutableListOf<String>()
        readableDatabase.query(
            "block_exercises",
            arrayOf("exercise_id"),
            "block_id = ?",
            arrayOf(blockId),
            null,
            null,
            "position",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                exerciseIds += cursor.getString(cursor.getColumnIndexOrThrow("exercise_id"))
            }
        }
        return exerciseIds
    }

    private fun requireValid(exercise: Exercise) {
        require(exercise.id.isNotBlank()) { "Не удалось определить упражнение." }
        require(exercise.name.isNotBlank()) { "Введите название упражнения." }
        require(exercise.sets > 0 && exercise.repetitions > 0) { "Подходы и повторения должны быть больше нуля." }
        require(exercise.phases.isNotEmpty()) { "Добавьте хотя бы одну фазу движения." }
        require(exercise.phases.all { it.title.isNotBlank() && it.durationSeconds > 0 }) {
            "У каждой фазы должно быть название и положительная длительность."
        }
    }

    private fun insertExercise(
        db: SQLiteDatabase,
        exercise: Exercise,
        builtIn: Boolean,
    ) {
        val values =
            ContentValues().apply {
                put("id", exercise.id)
                put("name", exercise.name)
                put("technique", exercise.technique.joinToString("\n"))
                put("sets", exercise.sets)
                put("repetitions", exercise.repetitions)
                put("built_in", if (builtIn) 1 else 0)
            }
        db.insertOrThrow("exercises", null, values)
        exercise.phases.forEachIndexed { index, phase -> insertPhase(db, exercise.id, index, phase) }
    }

    private fun insertPhase(
        db: SQLiteDatabase,
        exerciseId: String,
        position: Int,
        phase: MovementPhase,
    ) {
        val values =
            ContentValues().apply {
                put("exercise_id", exerciseId)
                put("position", position)
                put("title", phase.title)
                put("duration_seconds", phase.durationSeconds)
            }
        db.insertOrThrow("phases", null, values)
    }

    private fun insertBlock(
        db: SQLiteDatabase,
        block: WorkoutBlock,
    ) {
        val values =
            ContentValues().apply {
                put("id", block.id)
                put("name", block.name)
                put("built_in", if (block.isBuiltIn) 1 else 0)
            }
        db.insertOrThrow("workout_blocks", null, values)
    }

    private fun insertBlockExercise(
        db: SQLiteDatabase,
        blockId: String,
        exerciseId: String,
        position: Int,
    ) {
        val values =
            ContentValues().apply {
                put("block_id", blockId)
                put("exercise_id", exerciseId)
                put("position", position)
            }
        db.insertOrThrow("block_exercises", null, values)
    }

    private companion object {
        const val DATABASE_NAME = "knee_tempo.db"
        const val DATABASE_VERSION = 1
    }
}
