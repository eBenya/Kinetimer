package ru.projectworkout.kinetimer.ui

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.os.LocaleListCompat
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.projectworkout.kinetimer.R
import ru.projectworkout.kinetimer.data.AppSettingsRepository
import ru.projectworkout.kinetimer.data.ExerciseInUseException
import ru.projectworkout.kinetimer.data.GitHubUpdateRepository
import ru.projectworkout.kinetimer.data.WorkoutLibraryDatabase
import ru.projectworkout.kinetimer.domain.model.AppDestination
import ru.projectworkout.kinetimer.domain.model.AppSettings
import ru.projectworkout.kinetimer.domain.model.Exercise
import ru.projectworkout.kinetimer.domain.model.UpdateState
import ru.projectworkout.kinetimer.domain.model.WorkoutBlock
import ru.projectworkout.kinetimer.domain.model.WorkoutUiState

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WorkoutLibraryDatabase(application)
    private val settingsRepository = AppSettingsRepository(application)
    private val updateRepository = GitHubUpdateRepository(application)
    val appVersion: String = updateRepository.currentVersion()

    private val _settings = MutableStateFlow(settingsRepository.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _destination = MutableStateFlow<AppDestination>(AppDestination.Home)
    val destination: StateFlow<AppDestination> = _destination.asStateFlow()

    private val _uiState = MutableStateFlow<WorkoutUiState?>(null)
    val uiState: StateFlow<WorkoutUiState?> = _uiState.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _blocks = MutableStateFlow<List<WorkoutBlock>>(emptyList())
    val blocks: StateFlow<List<WorkoutBlock>> = _blocks.asStateFlow()

    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

    private var workoutExercises: MutableList<Exercise> = mutableListOf()
    private var timerJob: Job? = null

    init {
        refreshLibrary()
    }

    fun openExerciseLibrary() {
        clearMessage()
        _destination.value = AppDestination.ExerciseLibrary
    }

    fun openSettings() {
        clearMessage()
        _destination.value = AppDestination.Settings
    }

    fun updateSettings(settings: AppSettings) {
        val languageChanged = _settings.value.language != settings.language
        settingsRepository.save(settings)
        _settings.value = settings
        if (languageChanged) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(settings.language.preferenceValue),
            )
        }
    }

    fun checkForUpdates() {
        if (_updateState.value is UpdateState.Checking || _updateState.value is UpdateState.Downloading) return
        _updateState.value = UpdateState.Checking
        viewModelScope.launch {
            try {
                val release = withContext(Dispatchers.IO) { updateRepository.fetchLatestRelease() }
                val currentVersion = updateRepository.currentVersion()
                val versionComparison = compareVersions(release.tag, currentVersion)
                _updateState.value =
                    UpdateState.Result(
                        currentVersion = currentVersion,
                        release = release,
                        updateAvailable =
                            versionComparison?.let { it > 0 }
                                ?: !release.tag.equals(currentVersion, ignoreCase = true),
                        versionComparable = versionComparison != null,
                    )
            } catch (exception: java.io.IOException) {
                _updateState.value = UpdateState.Error(R.string.error_check_updates)
            } catch (exception: org.json.JSONException) {
                _updateState.value = UpdateState.Error(R.string.error_update_server_response)
            } catch (exception: SecurityException) {
                _updateState.value = UpdateState.Error(R.string.error_update_connection)
            }
        }
    }

    fun installUpdate() {
        val result =
            when (val state = _updateState.value) {
                is UpdateState.Result -> state
                is UpdateState.InstallPermissionRequired -> state.result
                else -> return
            }
        if (!result.updateAvailable || _updateState.value is UpdateState.Downloading) return
        _updateState.value = UpdateState.Downloading
        viewModelScope.launch {
            try {
                val apk = withContext(Dispatchers.IO) { updateRepository.downloadApk(result.release) }
                if (updateRepository.openInstaller(apk)) {
                    _updateState.value = result
                } else {
                    _updateState.value =
                        UpdateState.InstallPermissionRequired(
                            result = result,
                            messageResId = R.string.error_update_install_permission,
                        )
                }
            } catch (exception: java.io.IOException) {
                _updateState.value = UpdateState.Error(R.string.error_download_update)
            } catch (exception: SecurityException) {
                _updateState.value = UpdateState.Error(R.string.error_open_installer)
            } catch (exception: android.content.ActivityNotFoundException) {
                _updateState.value = UpdateState.Error(R.string.error_no_installer)
            }
        }
    }

    fun openRelease(url: String) {
        try {
            val intent =
                android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        } catch (exception: android.content.ActivityNotFoundException) {
            _updateState.value = UpdateState.Error(R.string.error_open_release)
        }
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
                if (block == null) {
                    _message.value = R.string.error_block_not_found
                    return@launch
                }
                val sequence = block.exerciseIds.mapNotNull(allExercises::get)
                if (sequence.isEmpty()) {
                    _message.value = R.string.error_block_empty
                    return@launch
                }
                workoutExercises = sequence.toMutableList()
                prepareExercise(exerciseIndex = 0, setNumber = 1)
                _destination.value = AppDestination.Workout
            } catch (exception: IllegalArgumentException) {
                _message.value = R.string.error_start_workout
            } catch (exception: SQLiteException) {
                _message.value = R.string.error_read_workout
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
        val preparationSeconds = _settings.value.preparationSeconds
        if (preparationSeconds == 0) {
            _uiState.value = state.workoutState
            startTimer()
        } else {
            _uiState.value = state.copy(resumeSecondsLeft = preparationSeconds)
            startTimer()
        }
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
                _message.value = R.string.error_load_library
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
                _message.value = R.string.error_exercise_in_use
            } catch (exception: IllegalArgumentException) {
                _message.value = R.string.error_check_input
            } catch (exception: IllegalStateException) {
                _message.value = R.string.error_action_unavailable
            } catch (exception: SQLiteException) {
                _message.value = R.string.error_save_data
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
        val preparationSeconds = _settings.value.preparationSeconds
        if (preparationSeconds == 0) return
        timerJob?.cancel()
        _uiState.value =
            WorkoutUiState.Preparing(
                exerciseState = exerciseState,
                secondsLeft = preparationSeconds,
            )
        startTimer()
    }

    private fun startRest(
        seconds: Int,
        nextExerciseIndex: Int,
        nextSetNumber: Int,
        titleResId: Int,
        messageResId: Int,
        requiresConfirmation: Boolean = false,
    ) {
        if (seconds == 0 && !requiresConfirmation) {
            showExercise(nextExerciseIndex, nextSetNumber)
            return
        }
        val upcoming = if (requiresConfirmation) upcomingExercises(nextExerciseIndex) else emptyList()
        _uiState.value =
            WorkoutUiState.Resting(
                secondsLeft = seconds.coerceAtLeast(0),
                nextExerciseIndex = nextExerciseIndex,
                nextSetNumber = nextSetNumber,
                titleResId = titleResId,
                messageResId = messageResId,
                upcomingExercises = upcoming,
                selectedNextExerciseId = upcoming.firstOrNull()?.id,
                requiresConfirmation = requiresConfirmation,
            )
        if (seconds > 0) startTimer()
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
                    seconds = _settings.value.setRestSeconds,
                    nextExerciseIndex = state.exerciseIndex,
                    nextSetNumber = state.setNumber + 1,
                    titleResId = R.string.workout_set_complete,
                    messageResId = R.string.workout_set_rest_message,
                )
            }

            state.exerciseIndex < workoutExercises.lastIndex -> {
                startRest(
                    seconds = _settings.value.exerciseRestSeconds,
                    nextExerciseIndex = state.exerciseIndex + 1,
                    nextSetNumber = 1,
                    titleResId = R.string.workout_exercise_complete,
                    messageResId = R.string.workout_exercise_rest_message,
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
        val preparationSeconds = _settings.value.preparationSeconds
        if (preparationSeconds == 0) {
            _uiState.value = exerciseState
        } else {
            _uiState.value = WorkoutUiState.Preparing(exerciseState, preparationSeconds)
            startTimer()
        }
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

    private fun compareVersions(
        releaseTag: String,
        currentVersion: String,
    ): Int? {
        val releaseNumbers = releaseTag.removePrefix("v").removePrefix("V").numericVersionParts()
        val currentNumbers = currentVersion.removePrefix("v").removePrefix("V").numericVersionParts()
        if (releaseNumbers == null || currentNumbers == null) return null
        val partCount = maxOf(releaseNumbers.size, currentNumbers.size)
        for (index in 0 until partCount) {
            val releasePart = releaseNumbers.getOrElse(index) { 0 }
            val currentPart = currentNumbers.getOrElse(index) { 0 }
            if (releasePart != currentPart) return releasePart.compareTo(currentPart)
        }
        return 0
    }
    private fun String.numericVersionParts(): List<Int>? {
        if (!matches(Regex("\\d+(\\.\\d+)*"))) return null
        return split('.').map(String::toInt)
    }

}
