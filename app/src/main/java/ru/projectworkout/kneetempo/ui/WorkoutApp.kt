package ru.projectworkout.kneetempo.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.projectworkout.kneetempo.domain.model.AppDestination
import ru.projectworkout.kneetempo.domain.model.WorkoutUiState
import ru.projectworkout.kneetempo.ui.screens.BlockEditorScreen
import ru.projectworkout.kneetempo.ui.screens.ExerciseEditorScreen
import ru.projectworkout.kneetempo.ui.screens.ExerciseLibraryScreen
import ru.projectworkout.kneetempo.ui.screens.FinishedScreen
import ru.projectworkout.kneetempo.ui.screens.HomeScreen
import ru.projectworkout.kneetempo.ui.screens.PausedScreen
import ru.projectworkout.kneetempo.ui.screens.PreparingScreen
import ru.projectworkout.kneetempo.ui.screens.RestScreen
import ru.projectworkout.kneetempo.ui.screens.ExerciseScreen
import ru.projectworkout.kneetempo.ui.screens.SettingsScreen

@Composable
fun WorkoutApp(workoutViewModel: WorkoutViewModel = viewModel()) {
    val destination by workoutViewModel.destination.collectAsStateWithLifecycle()
    val workoutState by workoutViewModel.uiState.collectAsStateWithLifecycle()
    val exercises by workoutViewModel.exercises.collectAsStateWithLifecycle()
    val blocks by workoutViewModel.blocks.collectAsStateWithLifecycle()
    val message by workoutViewModel.message.collectAsStateWithLifecycle()
    val settings by workoutViewModel.settings.collectAsStateWithLifecycle()
    val updateState by workoutViewModel.updateState.collectAsStateWithLifecycle()
    val view = LocalView.current

    DisposableEffect(view, destination, settings.keepScreenOnDuringWorkout) {
        view.keepScreenOn = destination == AppDestination.Workout && settings.keepScreenOnDuringWorkout
        onDispose { view.keepScreenOn = false }
    }

    when (val screen = destination) {
        AppDestination.Home ->
            HomeScreen(
                blocks = blocks,
                message = message,
                onStartBlock = workoutViewModel::startWorkout,
                onCreateBlock = workoutViewModel::createBlock,
                onEditBlock = workoutViewModel::editBlock,
                onDeleteBlock = workoutViewModel::deleteBlock,
                onOpenLibrary = workoutViewModel::openExerciseLibrary,
                onOpenSettings = workoutViewModel::openSettings,
                onClearMessage = workoutViewModel::clearMessage,
            )

        AppDestination.Settings ->
            SettingsScreen(
                appVersion = workoutViewModel.appVersion,
                settings = settings,
                updateState = updateState,
                onSettingsChange = workoutViewModel::updateSettings,
                onCheckForUpdates = workoutViewModel::checkForUpdates,
                onInstallUpdate = workoutViewModel::installUpdate,
                onOpenRelease = workoutViewModel::openRelease,
                onBack = workoutViewModel::goHome,
            )

        AppDestination.ExerciseLibrary ->
            ExerciseLibraryScreen(
                exercises = exercises,
                message = message,
                onCreate = workoutViewModel::createExercise,
                onEdit = workoutViewModel::editExercise,
                onDelete = workoutViewModel::deleteExercise,
                onBack = workoutViewModel::goHome,
                onClearMessage = workoutViewModel::clearMessage,
            )

        is AppDestination.ExerciseEditor ->
            ExerciseEditorScreen(
                exercise = exercises.firstOrNull { it.id == screen.exerciseId },
                message = message,
                onSave = workoutViewModel::saveExercise,
                onBack = workoutViewModel::openExerciseLibrary,
                onClearMessage = workoutViewModel::clearMessage,
            )

        is AppDestination.BlockEditor ->
            BlockEditorScreen(
                block = blocks.firstOrNull { it.id == screen.blockId },
                exercises = exercises,
                message = message,
                onSave = { name, exerciseIds ->
                    workoutViewModel.saveBlock(name, exerciseIds, screen.blockId)
                },
                onBack = workoutViewModel::goHome,
                onClearMessage = workoutViewModel::clearMessage,
            )

        AppDestination.Workout ->
            when (val state = workoutState) {
                is WorkoutUiState.Preparing ->
                    PreparingScreen(state = state, onPause = workoutViewModel::pauseWorkout)

                is WorkoutUiState.Exercising -> {
                    if (settings.soundEnabled) PhaseTone(state = state)
                    ExerciseScreen(
                        state = state,
                        onFinish = workoutViewModel::finishWorkout,
                        onPause = workoutViewModel::pauseWorkout,
                    )
                }

                is WorkoutUiState.Resting ->
                    RestScreen(
                        state = state,
                        onSkip = workoutViewModel::skipRest,
                        onSelectNextExercise = workoutViewModel::selectNextExercise,
                    )

                is WorkoutUiState.Paused ->
                    PausedScreen(
                        state = state,
                        onResume = workoutViewModel::resumeWorkout,
                        onCancelResume = workoutViewModel::cancelResume,
                        onSelectNextExercise = workoutViewModel::selectNextExercise,
                    )

                WorkoutUiState.Finished ->
                    FinishedScreen(onReturn = workoutViewModel::finishWorkout)

                null -> HomeScreen(
                    blocks = blocks,
                    message = message,
                    onStartBlock = workoutViewModel::startWorkout,
                    onCreateBlock = workoutViewModel::createBlock,
                    onEditBlock = workoutViewModel::editBlock,
                    onDeleteBlock = workoutViewModel::deleteBlock,
                    onOpenLibrary = workoutViewModel::openExerciseLibrary,
                    onOpenSettings = workoutViewModel::openSettings,
                    onClearMessage = workoutViewModel::clearMessage,
                )
            }
    }
}

@Composable
private fun PhaseTone(state: WorkoutUiState.Exercising) {
    val context = LocalContext.current
    val toneGenerator =
        remember(context) {
            ToneGenerator(AudioManager.STREAM_MUSIC, 35)
        }

    DisposableEffect(toneGenerator) {
        onDispose { toneGenerator.release() }
    }

    var lastTransitionCount by remember(state.exerciseIndex) {
        mutableStateOf(state.phaseTransitionCount)
    }
    LaunchedEffect(state.exerciseIndex, state.phaseTransitionCount, state.secondsLeft) {
        if (state.phaseTransitionCount > lastTransitionCount) {
            lastTransitionCount = state.phaseTransitionCount
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, PHASE_BEEP_MILLIS)
        } else if (state.secondsLeft in 1..PHASE_COUNTDOWN_TICKS) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, COUNTDOWN_TICK_MILLIS)
        }
    }
}

private const val PHASE_BEEP_MILLIS = 120
private const val COUNTDOWN_TICK_MILLIS = 55
private const val PHASE_COUNTDOWN_TICKS = 3
