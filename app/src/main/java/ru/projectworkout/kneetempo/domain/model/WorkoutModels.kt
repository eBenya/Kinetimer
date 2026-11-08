package ru.projectworkout.kneetempo.domain.model

data class MovementPhase(
    val title: String,
    val durationSeconds: Int,
)

data class Exercise(
    val id: String,
    val name: String,
    val technique: List<String>,
    val sets: Int,
    val repetitions: Int,
    val phases: List<MovementPhase>,
)

data class WorkoutBlock(
    val id: String,
    val name: String,
    val exerciseIds: List<String>,
    val isBuiltIn: Boolean = false,
)

data class AppSettings(
    val soundEnabled: Boolean = true,
    val preparationSeconds: Int = 5,
    val setRestSeconds: Int = 60,
    val exerciseRestSeconds: Int = 90,
    val keepScreenOnDuringWorkout: Boolean = true,
)

data class AppRelease(
    val tag: String,
    val name: String,
    val pageUrl: String,
    val apkUrl: String,
    val apkSha256: String?,
)

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data object Downloading : UpdateState

    data class Result(
        val currentVersion: String,
        val release: AppRelease,
        val updateAvailable: Boolean,
        val versionComparable: Boolean,
    ) : UpdateState

    data class InstallPermissionRequired(
        val result: Result,
        val message: String,
    ) : UpdateState

    data class Error(val message: String) : UpdateState
}

sealed interface AppDestination {
    data object Home : AppDestination

    data object ExerciseLibrary : AppDestination

    data object Settings : AppDestination

    data class ExerciseEditor(val exerciseId: String?) : AppDestination

    data class BlockEditor(val blockId: String?) : AppDestination

    data object Workout : AppDestination
}

sealed interface WorkoutUiState {
    data class Preparing(
        val exerciseState: Exercising,
        val secondsLeft: Int,
    ) : WorkoutUiState

    data class Paused(
        val workoutState: Exercising,
        val resumeSecondsLeft: Int? = null,
        val upcomingExercises: List<Exercise> = emptyList(),
        val selectedNextExerciseId: String? = null,
    ) : WorkoutUiState

    data class Exercising(
        val exercise: Exercise,
        val exerciseIndex: Int,
        val exerciseCount: Int,
        val setNumber: Int,
        val repetitionNumber: Int,
        val phaseNumber: Int,
        val phase: MovementPhase,
        val secondsLeft: Int,
        val phaseTransitionCount: Int = 0,
    ) : WorkoutUiState

    data class Resting(
        val secondsLeft: Int,
        val nextExerciseIndex: Int,
        val nextSetNumber: Int,
        val title: String,
        val message: String,
        val upcomingExercises: List<Exercise> = emptyList(),
        val selectedNextExerciseId: String? = null,
        val requiresConfirmation: Boolean = false,
    ) : WorkoutUiState

    data object Finished : WorkoutUiState
}
