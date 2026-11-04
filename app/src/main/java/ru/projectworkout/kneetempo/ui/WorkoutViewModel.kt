package ru.projectworkout.kneetempo.ui

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.projectworkout.kneetempo.data.ExerciseInUseException
import ru.projectworkout.kneetempo.data.WorkoutLibraryDatabase
import ru.projectworkout.kneetempo.domain.model.AppDestination
import ru.projectworkout.kneetempo.domain.model.Exercise
import ru.projectworkout.kneetempo.domain.model.WorkoutBlock
import ru.projectworkout.kneetempo.domain.model.WorkoutUiState

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WorkoutLibraryDatabase(application)

    private val _destination = MutableStateFlow<AppDestination>(AppDestination.Home)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    private val _uiState = MutableStateFlow<WorkoutUiState?>(null)
    val uiState: StateFlow<WorkoutUiState?> = _uiState.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _blocks = MutableStateFlow<List<WorkoutBlock>>(emptyList())
    val blocks: StateFlow<List<WorkoutBlock>> = _blocks.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var workoutExercises: MutableList<Exercise> = mutableListOf()
    private var timerJob: Job? = null

    init {
        refreshLibrary()
    }

    fun openExerciseLibrary() {
        clearMessage()
        _destination.value = AppDestination.ExerciseLibrary
    }

    fun createExercise() {
        clearMessage()
        _destination.value = AppDestination.ExerciseEditor(exerciseId = null)
    }

    fun editExercise(exerciseId: String) {
        clearMessage()
        _destination.value = AppDestination.ExerciseEditor(exerciseId)
    }

    fun createBlock() {
        clearMessage()
        _destination.value = AppDestination.BlockEditor(blockId = null)
    }

    fun editBlock(blockId: String) {
        clearMessage()
        _destination.value = AppDestination.BlockEditor(blockId)
    }

    fun goHome() {
        clearMessage()
        _destination.value = AppDestination.Home
    }

    fun saveExercise(exercise: Exercise) {
        val savedExercise =
            exercise.copy(
                id = exercise.id.ifBlank { "custom-${UUID.randomUUID()}" },
            )
        performDatabaseOperation(
            operation = { database.saveExercise(savedExercise) },
            onSuccess = { _destination.value = AppDestination.ExerciseLibrary },
        )
    }

    fun deleteExercise(exerciseId: String) {
        performDatabaseOperation(operation = { database.deleteExercise(exerciseId) })
    }

    fun saveBlock(name: String, exerciseIds: List<String>, blockId: String?) {
        val block =
            WorkoutBlock(
                id = blockId ?: UUID.randomUUID().toString(),
                name = name,
                exerciseIds = exerciseIds,
            )
        performDatabaseOperation(
            operation = { database.saveBlock(block) },
            onSuccess = { _destination.value = AppDestination.Home },
        )
    }

    fun deleteBlock(blockId: String) {
        performDatabaseOperation(operation = { database.deleteBlock(blockId) })
    }

    fun startWorkout(blockId: String) {
        clearMessage()
        viewModelScope.launch {
            try {
                val (block, allExercises) =
                    withContext(Dispatchers.IO) {
                        val selectedBlock = database.getBlocks().firstOrNull { it.id == blockId }
                        selectedBlock to database.getExercises().associateBy(Exercise::id)
                    }
                requireNotNull(block) { "Не удалось найти выбранный блок." }
                val sequence = block.exerciseIds.map { id -> requireNotNull(allExercises[id]) }
                require(sequence.isNotEmpty()) { "В блоке нет упражнений." }
                workoutExercises = sequence.toMutableList()
                prepareExercise(exerciseIndex = 0, setNumber = 1)
                _destination.value = AppDestination.Workout
            } catch (exception: IllegalArgumentException) {
                _message.value = exception.message ?: "Не удалось запустить блок."
            } catch (exception: SQLiteException) {
                _message.value = exception.message ?: "Не удалось прочитать данные тренировки."
            }
        }
    }

    fun skipRest() {
        val state = _uiState.value as? WorkoutUiState.Resting ?: return
        timerJob?.cancel()
        if (state.requiresConfirmation) {
            continueFromExerciseRest(state)
        } else {
            showExercise(state.nextExerciseIndex, state.nextSetNumber)
        }
    }

    fun pauseWorkout() {
        val state =
            when (val current = _uiState.value) {
                is WorkoutUiState.Exercising -> current
                is WorkoutUiState.Preparing -> current.exerciseState
                else -> return
            }
        timerJob?.cancel()
        val upcoming = upcomingExercises(state.exerciseIndex + 1)
        _uiState.value =
            WorkoutUiState.Paused(
                workoutState = state,
                upcomingExercises = upcoming,
                selectedNextExerciseId = upcoming.firstOrNull()?.id,
            )
    }

    fun resumeWorkout() {
        val state = _uiState.value as? WorkoutUiState.Paused ?: return
        if (state.resumeSecondsLeft != null) return
        applyNextExerciseSelection(
            startIndex = state.workoutState.exerciseIndex + 1,
            selectedExerciseId = state.selectedNextExerciseId,
        )
        _uiState.value = state.copy(resumeSecondsLeft = PREPARATION_SECONDS)
        startTimer()
    }

    fun selectNextExercise(exerciseId: String) {
        when (val state = _uiState.value) {
            is WorkoutUiState.Paused -> {
                if (state.resumeSecondsLeft != null || state.upcomingExercises.none { it.id == exerciseId }) return
                _uiState.value = state.copy(selectedNextExerciseId = exerciseId)
            }

            is WorkoutUiState.Resting -> {
                if (!state.requiresConfirmation || state.upcomingExercises.none { it.id == exerciseId }) return
                _uiState.value = state.copy(selectedNextExerciseId = exerciseId)
            }

            else -> Unit
        }
    }

    fun cancelResume() {
        val state = _uiState.value as? WorkoutUiState.Paused ?: return
        if (state.resumeSecondsLeft == null) return
        timerJob?.cancel()
        _uiState.value = state.copy(resumeSecondsLeft = null)
    }

    fun finishWorkout() {
        timerJob?.cancel()
        workoutExercises = mutableListOf()
        _uiState.value = null
        _destination.value = AppDestination.Home
    }

    fun clearMessage() {
        _message.value = null
    }

    override fun onCleared() {
        timerJob?.cancel()
        database.close()
        super.onCleared()
    }

    private fun refreshLibrary() {
        viewModelScope.launch {
            try {
                val (loadedExercises, loadedBlocks) =
                    withContext(Dispatchers.IO) {
                        database.getExercises() to database.getBlocks()
                    }
                _exercises.value = loadedExercises
                _blocks.value = loadedBlocks
            } catch (exception: SQLiteException) {
                _message.value = exception.message ?: "Не удалось загрузить библиотеку тренировок."
            }
        }
    }

    private fun performDatabaseOperation(
        operation: suspend () -> Unit,
        onSuccess: () -> Unit = {},
    ) {
        clearMessage()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { operation() }
                val (loadedExercises, loadedBlocks) =
                    withContext(Dispatchers.IO) {
                        database.getExercises() to database.getBlocks()
                    }
                _exercises.value = loadedExercises
                _blocks.value = loadedBlocks
                onSuccess()
            } catch (exception: ExerciseInUseException) {
                _message.value = exception.message
            } catch (exception: IllegalArgumentException) {
                _message.value = exception.message ?: "Проверьте введенные данные."
            } catch (exception: IllegalStateException) {
                _message.value = exception.message ?: "Это действие недоступно."
            } catch (exception: SQLiteException) {
                _message.value = exception.message ?: "Не удалось сохранить данные."
            }
        }
    }

    private fun showExercise(
        exerciseIndex: Int,
        setNumber: Int,
    ) {
        timerJob?.cancel()
        val exercise = workoutExercises[exerciseIndex]
        _uiState.value =
            WorkoutUiState.Exercising(
                exercise = exercise,
                exerciseIndex = exerciseIndex,
                exerciseCount = workoutExercises.size,
                setNumber = setNumber,
                repetitionNumber = 1,
                phaseNumber = 0,
                phase = exercise.phases.first(),
                secondsLeft = exercise.phases.first().durationSeconds,
            )
        startTimer()
    }

    private fun prepareExercise(
        exerciseIndex: Int,
        setNumber: Int,
    ) {
        showExercise(exerciseIndex, setNumber)
        val exerciseState = _uiState.value as WorkoutUiState.Exercising
        timerJob?.cancel()
        _uiState.value =
            WorkoutUiState.Preparing(
                exerciseState = exerciseState,
                secondsLeft = PREPARATION_SECONDS,
            )
        startTimer()
    }

    private fun startRest(
        seconds: Int,
        nextExerciseIndex: Int,
        nextSetNumber: Int,
        title: String,
        message: String,
        requiresConfirmation: Boolean = false,
    ) {
        val upcoming = if (requiresConfirmation) upcomingExercises(nextExerciseIndex) else emptyList()
        _uiState.value =
            WorkoutUiState.Resting(
                secondsLeft = seconds,
                nextExerciseIndex = nextExerciseIndex,
                nextSetNumber = nextSetNumber,
                title = title,
                message = message,
                upcomingExercises = upcoming,
                selectedNextExerciseId = upcoming.firstOrNull()?.id,
                requiresConfirmation = requiresConfirmation,
            )
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                while (true) {
                    delay(1_000)
                    when (val state = _uiState.value) {
                        is WorkoutUiState.Exercising -> advanceExerciseTimer(state)
                        is WorkoutUiState.Resting -> advanceRestTimer(state)
                        is WorkoutUiState.Preparing -> advancePreparation(state)
                        is WorkoutUiState.Paused -> advanceResumeCountdown(state)
                        WorkoutUiState.Finished -> return@launch
                        null -> return@launch
                    }
                }
            }
    }

    private fun advanceExerciseTimer(state: WorkoutUiState.Exercising) {
        if (state.secondsLeft > 1) {
            _uiState.value = state.copy(secondsLeft = state.secondsLeft - 1)
            return
        }

        val exercise = state.exercise
        when {
            state.phaseNumber < exercise.phases.lastIndex -> {
                val nextPhaseNumber = state.phaseNumber + 1
                val nextPhase = exercise.phases[nextPhaseNumber]
                _uiState.value =
                    state.copy(
                        phaseNumber = nextPhaseNumber,
                        phase = nextPhase,
                        secondsLeft = nextPhase.durationSeconds,
                        phaseTransitionCount = state.phaseTransitionCount + 1,
                    )
            }

            state.repetitionNumber < exercise.repetitions -> {
                val firstPhase = exercise.phases.first()
                _uiState.value =
                    state.copy(
                        repetitionNumber = state.repetitionNumber + 1,
                        phaseNumber = 0,
                        phase = firstPhase,
                        secondsLeft = firstPhase.durationSeconds,
                        phaseTransitionCount = state.phaseTransitionCount + 1,
                    )
            }

            state.setNumber < exercise.sets -> {
                startRest(
                    seconds = SET_REST_SECONDS,
                    nextExerciseIndex = state.exerciseIndex,
                    nextSetNumber = state.setNumber + 1,
                    title = "Подход завершен",
                    message = "Восстановите дыхание перед следующим подходом.",
                )
            }

            state.exerciseIndex < workoutExercises.lastIndex -> {
                startRest(
                    seconds = EXERCISE_REST_SECONDS,
                    nextExerciseIndex = state.exerciseIndex + 1,
                    nextSetNumber = 1,
                    title = "Упражнение завершено",
                    message = "Отдохните или продолжите раньше, если уже готовы.",
                    requiresConfirmation = true,
                )
            }

            else -> {
                timerJob?.cancel()
                _uiState.value = WorkoutUiState.Finished
            }
        }
    }

    private fun advanceRestTimer(state: WorkoutUiState.Resting) {
        if (state.requiresConfirmation && state.secondsLeft <= 1) {
            timerJob?.cancel()
            _uiState.value = state.copy(secondsLeft = 0)
            return
        }
        if (state.secondsLeft > 1) {
            _uiState.value = state.copy(secondsLeft = state.secondsLeft - 1)
        } else {
            showExercise(state.nextExerciseIndex, state.nextSetNumber)
        }
    }

    private fun advancePreparation(state: WorkoutUiState.Preparing) {
        if (state.secondsLeft > 1) {
            _uiState.value = state.copy(secondsLeft = state.secondsLeft - 1)
        } else {
            _uiState.value = state.exerciseState
        }
    }

    private fun advanceResumeCountdown(state: WorkoutUiState.Paused) {
        val secondsLeft = state.resumeSecondsLeft ?: return
        if (secondsLeft > 1) {
            _uiState.value = state.copy(resumeSecondsLeft = secondsLeft - 1)
        } else {
            _uiState.value = state.workoutState
        }
    }

    private fun continueFromExerciseRest(state: WorkoutUiState.Resting) {
        applyNextExerciseSelection(
            startIndex = state.nextExerciseIndex,
            selectedExerciseId = state.selectedNextExerciseId,
        )
        showExercise(state.nextExerciseIndex, state.nextSetNumber)
        val exerciseState = _uiState.value as WorkoutUiState.Exercising
        _uiState.value = WorkoutUiState.Preparing(exerciseState, PREPARATION_SECONDS)
        startTimer()
    }

    private fun upcomingExercises(startIndex: Int): List<Exercise> =
        workoutExercises.drop(startIndex)

    private fun applyNextExerciseSelection(
        startIndex: Int,
        selectedExerciseId: String?,
    ) {
        if (selectedExerciseId == null) return
        val selectedIndex =
            workoutExercises.indexOfFirst { it.id == selectedExerciseId }
        if (selectedIndex < startIndex) return
        val selectedExercise = workoutExercises.removeAt(selectedIndex)
        workoutExercises.add(startIndex, selectedExercise)
    }

    private companion object {
        const val SET_REST_SECONDS = 60
        const val EXERCISE_REST_SECONDS = 90
        const val PREPARATION_SECONDS = 5
    }
}
