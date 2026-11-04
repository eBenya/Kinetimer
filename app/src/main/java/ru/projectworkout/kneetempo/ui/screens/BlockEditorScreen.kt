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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.projectworkout.kneetempo.domain.model.Exercise
import ru.projectworkout.kneetempo.domain.model.WorkoutBlock

@Composable
fun BlockEditorScreen(
    block: WorkoutBlock?,
    exercises: List<Exercise>,
    message: String?,
    onSave: (name: String, exerciseIds: List<String>) -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var name by remember(block?.id) { mutableStateOf(block?.name.orEmpty()) }
    var selectedIds by remember(block?.id) { mutableStateOf(block?.exerciseIds.orEmpty()) }
    var validationMessage by remember(block?.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("← На главную") }
        Text(
            if (block == null) "Новый блок" else "Изменить блок",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; onClearMessage() },
            label = { Text("Название блока") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text("Выберите упражнения. Их порядок будет соответствовать порядку выбора.")
        if (exercises.isEmpty()) {
            Text("Сначала добавьте упражнение в библиотеку.")
        } else {
            exercises.forEach { exercise ->
                val selected = exercise.id in selectedIds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { checked ->
                            selectedIds =
                                if (checked) {
                                    selectedIds + exercise.id
                                } else {
                                    selectedIds.filterNot { it == exercise.id }
                                }
                            onClearMessage()
                        },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, fontWeight = FontWeight.Medium)
                        Text("${exercise.sets} × ${exercise.repetitions}")
                    }
                    if (selected) {
                        Text(
                            "${selectedIds.indexOf(exercise.id) + 1}",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                validationMessage =
                    when {
                        name.isBlank() -> "Введите название блока."
                        selectedIds.isEmpty() -> "Выберите хотя бы одно упражнение."
                        else -> null
                    }
                if (validationMessage == null) onSave(name.trim(), selectedIds)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить блок")
        }
    }
}
