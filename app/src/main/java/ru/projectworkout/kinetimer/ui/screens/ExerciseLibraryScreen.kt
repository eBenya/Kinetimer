package ru.projectworkout.kinetimer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.projectworkout.kinetimer.R
import ru.projectworkout.kinetimer.domain.model.Exercise

@Composable
fun ExerciseLibraryScreen(
    exercises: List<Exercise>,
    message: Int?,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.common_back_home))
        }
        Text(
            stringResource(R.string.exercise_library_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (message != null) {
            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onClearMessage) { Text(stringResource(R.string.common_ok)) }
        }
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exercise_library_add))
        }
        exercises.forEach { exercise ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        localizedExerciseName(exercise),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(R.string.exercise_sets_reps, exercise.sets, exercise.repetitions))
                    val secondsUnit = stringResource(R.string.duration_seconds_short)
                    val durations = exercise.phases.joinToString { "${it.durationSeconds} $secondsUnit" }
                    Text(
                        stringResource(
                            R.string.duration_phases,
                            pluralStringResource(R.plurals.exercise_phase_count, exercise.phases.size, exercise.phases.size),
                            durations,
                        ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEdit(exercise.id) }) {
                            Text(stringResource(R.string.common_edit))
                        }
                        if (exercise.id.startsWith("custom-")) {
                            TextButton(onClick = { onDelete(exercise.id) }) {
                                Text(stringResource(R.string.common_delete))
                            }
                        }
                    }
                }
            }
        }
    }
}
