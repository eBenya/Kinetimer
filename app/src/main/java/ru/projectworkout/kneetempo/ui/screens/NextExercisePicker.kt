package ru.projectworkout.kneetempo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.projectworkout.kneetempo.domain.model.Exercise

@Composable
fun NextExercisePicker(
    exercises: List<Exercise>,
    selectedExerciseId: String?,
    onSelect: (String) -> Unit,
) {
    Column {
        Text(
            text = "Следующее упражнение",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        exercises.forEachIndexed { index, exercise ->
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable(
                            role = Role.RadioButton,
                            onClick = { onSelect(exercise.id) },
                        ).padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = exercise.id == selectedExerciseId,
                    onClick = { onSelect(exercise.id) },
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
                    if (index == 0) {
                        Text(
                            "По порядку блока",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
