package ru.projectworkout.kneetempo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.projectworkout.kneetempo.domain.model.WorkoutUiState

@Composable
fun PausedScreen(
    state: WorkoutUiState.Paused,
    onResume: () -> Unit,
    onCancelResume: () -> Unit,
    onSelectNextExercise: (String) -> Unit,
) {
    val isCountingDown = state.resumeSecondsLeft != null
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (isCountingDown) "Приготовьтесь продолжить" else "Тренировка на паузе",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        if (isCountingDown) {
            Text(
                text = "${state.resumeSecondsLeft}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Продолжение через", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onCancelResume, modifier = Modifier.fillMaxWidth()) {
                Text("Отменить")
            }
        } else {
            val exerciseState = state.workoutState
            Text(
                text = "${exerciseState.exercise.name}\n${exerciseState.phase.title}: ${exerciseState.secondsLeft} с",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (state.upcomingExercises.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Других упражнений в блоке не осталось.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            } else {
                Spacer(Modifier.height(24.dp))
                NextExercisePicker(
                    exercises = state.upcomingExercises,
                    selectedExerciseId = state.selectedNextExerciseId,
                    onSelect = onSelectNextExercise,
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Продолжить")
            }
        }
    }
}
