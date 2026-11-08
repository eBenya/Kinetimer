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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.projectworkout.kneetempo.R
import ru.projectworkout.kneetempo.domain.model.Exercise
import ru.projectworkout.kneetempo.domain.model.MovementPhase

private data class PhaseDraft(
    val title: String,
    val seconds: String,
)

@Composable
fun ExerciseEditorScreen(
    exercise: Exercise?,
    message: Int?,
    onSave: (Exercise) -> Unit,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var name by remember(exercise?.id) { mutableStateOf(exercise?.name.orEmpty()) }
    var technique by remember(exercise?.id) { mutableStateOf(exercise?.technique?.joinToString("\n").orEmpty()) }
    var sets by remember(exercise?.id) { mutableStateOf(exercise?.sets?.toString() ?: "3") }
    var repetitions by remember(exercise?.id) { mutableStateOf(exercise?.repetitions?.toString() ?: "12") }
    var validationMessage by remember(exercise?.id) { mutableStateOf<Int?>(null) }
    val defaultPhaseTitle = stringResource(R.string.exercise_default_phase)
    val phases =
        remember(exercise?.id) {
            mutableStateListOf<PhaseDraft>().apply {
                exercise?.phases?.forEach { add(PhaseDraft(it.title, it.durationSeconds.toString())) }
                if (isEmpty()) add(PhaseDraft(defaultPhaseTitle, "3"))
            }
        }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.common_back_exercises)) }
        Text(
            stringResource(if (exercise == null) R.string.exercise_create_title else R.string.exercise_edit_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; onClearMessage() },
            label = { Text(stringResource(R.string.common_name)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = technique,
            onValueChange = { technique = it; onClearMessage() },
            label = { Text(stringResource(R.string.exercise_technique_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = sets,
                onValueChange = { sets = it.filter(Char::isDigit); onClearMessage() },
                label = { Text(stringResource(R.string.exercise_sets_label)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(
                value = repetitions,
                onValueChange = { repetitions = it.filter(Char::isDigit); onClearMessage() },
                label = { Text(stringResource(R.string.exercise_repetitions_label)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        }
        Text(stringResource(R.string.exercise_phases_title), style = MaterialTheme.typography.titleLarge)
        phases.forEachIndexed { index, phase ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = phase.title,
                    onValueChange = { phases[index] = phase.copy(title = it); onClearMessage() },
                    label = { Text(stringResource(R.string.exercise_phase_label, index + 1)) },
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
                        label = { Text(stringResource(R.string.exercise_phase_duration)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    if (phases.size > 1) {
                        TextButton(onClick = { phases.removeAt(index) }) {
                            Text(stringResource(R.string.exercise_phase_delete))
                        }
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
            Text(stringResource(R.string.exercise_phase_add))
        }

        validationMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
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
                        name.isBlank() -> R.string.exercise_validation_name
                        setCount == null || setCount <= 0 -> R.string.exercise_validation_sets
                        repetitionCount == null || repetitionCount <= 0 -> R.string.exercise_validation_repetitions
                        parsedPhases.size != phases.size -> R.string.exercise_validation_phases
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
            Text(stringResource(R.string.exercise_save))
        }
    }
}
