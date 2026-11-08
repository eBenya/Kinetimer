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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.projectworkout.kneetempo.domain.model.WorkoutUiState

@Composable
fun RestScreen(
    state: WorkoutUiState.Resting,
    onSkip: () -> Unit,
    onSelectNextExercise: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "${state.secondsLeft}",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            if (state.secondsLeft == 0) "Отдых завершен" else "секунд отдыха",
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.requiresConfirmation && state.upcomingExercises.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            NextExercisePicker(
                exercises = state.upcomingExercises,
                selectedExerciseId = state.selectedNextExerciseId,
                onSelect = onSelectNextExercise,
            )
        }
        Button(onClick = onSkip, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(
                when {
                    state.requiresConfirmation -> "Продолжить"
                    else -> "Пропустить отдых"
                },
            )
        }
    }
}
