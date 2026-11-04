package ru.projectworkout.kneetempo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.projectworkout.kneetempo.domain.model.Exercise
import ru.projectworkout.kneetempo.domain.model.MovementPhase

private data class PhaseDraft(
    val title: String,
    val seconds: String,
)

@Composable
fun ExerciseEditorScreen(
    exercise: Exercise?,
    message: String?,
    onSave: (Exercise) -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var name by remember(exercise?.id) { mutableStateOf(exercise?.name.orEmpty()) }
    var technique by remember(exercise?.id) { mutableStateOf(exercise?.technique?.joinToString("\n").orEmpty()) }
    var sets by remember(exercise?.id) { mutableStateOf(exercise?.sets?.toString() ?: "3") }
    var repetitions by remember(exercise?.id) { mutableStateOf(exercise?.repetitions?.toString() ?: "12") }
    var validationMessage by remember(exercise?.id) { mutableStateOf<String?>(null) }
    val phases =
        remember(exercise?.id) {
            mutableStateListOf<PhaseDraft>().apply {
                exercise?.phases?.forEach { add(PhaseDraft(it.title, it.durationSeconds.toString())) }
                if (isEmpty()) add(PhaseDraft("Медленное движение", "3"))
            }
        }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Назад к упражнениям") }
        Text(
            if (exercise == null) "Новое упражнение" else "Изменить упражнение",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; onClearMessage() },
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = technique,
            onValueChange = { technique = it; onClearMessage() },
            label = { Text("Подсказки техники — по одной на строку") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it.filter(Char::isDigit); onClearMessage() },
                label = { Text("Подходы") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = repetitions,
                onValueChange = { repetitions = it.filter(Char::isDigit); onClearMessage() },
                label = { Text("Повторения") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }
        Text("Фазы движения", style = MaterialTheme.typography.titleLarge)
        phases.forEachIndexed { index, phase ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = phase.title,
                    onValueChange = { phases[index] = phase.copy(title = it); onClearMessage() },
                    label = { Text("Фаза ${index + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = phase.seconds,
                        onValueChange = {
                            phases[index] = phase.copy(seconds = it.filter(Char::isDigit))
                            onClearMessage()
                        },
                        label = { Text("Длительность, секунд") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    if (phases.size > 1) {
                        TextButton(onClick = { phases.removeAt(index) }) { Text("Удалить фазу") }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                phases.add(PhaseDraft("", "2"))
                onClearMessage()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Добавить фазу")
        }

        validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                val setCount = sets.toIntOrNull()
                val repetitionCount = repetitions.toIntOrNull()
                val parsedPhases =
                    phases.mapNotNull { draft ->
                        val duration = draft.seconds.toIntOrNull()
                        if (draft.title.isBlank() || duration == null || duration <= 0) {
                            null
                        } else {
                            MovementPhase(draft.title.trim(), duration)
                        }
                    }
                validationMessage =
                    when {
                        name.isBlank() -> "Введите название упражнения."
                        setCount == null || setCount <= 0 -> "Количество подходов должно быть больше нуля."
                        repetitionCount == null || repetitionCount <= 0 -> "Количество повторений должно быть больше нуля."
                        parsedPhases.size != phases.size -> "У каждой фазы должно быть название и положительная длительность."
                        else -> null
                    }
                if (validationMessage == null) {
                    onSave(
                        Exercise(
                            id = exercise?.id.orEmpty(),
                            name = name.trim(),
                            technique = technique.lines().map(String::trim).filter(String::isNotEmpty),
                            sets = requireNotNull(setCount),
                            repetitions = requireNotNull(repetitionCount),
                            phases = parsedPhases,
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить упражнение")
        }
    }
}
