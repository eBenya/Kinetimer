package ru.projectworkout.kneetempo.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.projectworkout.kneetempo.domain.model.Exercise

@Composable
fun ExerciseLibraryScreen(
    exercises: List<Exercise>,
    message: String?,
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
        TextButton(onClick = onBack) { Text("← На главную") }
        Text("Библиотека упражнений", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (message != null) {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onClearMessage) { Text("Понятно") }
        }
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("Добавить упражнение") }
        exercises.forEach { exercise ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${exercise.sets} подхода × ${exercise.repetitions} повторений")
                    Text("${exercise.phases.size} фаз · ${exercise.phases.joinToString { "${it.durationSeconds} с" }}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEdit(exercise.id) }) { Text("Изменить") }
                        if (exercise.id.startsWith("custom-")) {
                            TextButton(onClick = { onDelete(exercise.id) }) { Text("Удалить") }
                        }
                    }
                }
            }
        }
    }
}
