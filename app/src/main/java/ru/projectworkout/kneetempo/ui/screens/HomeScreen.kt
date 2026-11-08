package ru.projectworkout.kneetempo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.projectworkout.kneetempo.R
import ru.projectworkout.kneetempo.domain.model.WorkoutBlock

@Composable
fun HomeScreen(
    blocks: List<WorkoutBlock>,
    message: Int?,
    onStartBlock: (String) -> Unit,
    onCreateBlock: () -> Unit,
    onEditBlock: (String) -> Unit,
    onDeleteBlock: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearMessage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Knee Tempo", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyLarge)
        if (message != null) {
            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onClearMessage) { Text(stringResource(R.string.common_ok)) }
        }

        Text(stringResource(R.string.home_built_in_workouts), style = MaterialTheme.typography.titleLarge)
        blocks.filter(WorkoutBlock::isBuiltIn).forEach { block ->
            BlockCard(
                block = block,
                onStart = { onStartBlock(block.id) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.home_my_blocks), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onCreateBlock) { Text(stringResource(R.string.home_create_block)) }
        }
        val customBlocks = blocks.filterNot(WorkoutBlock::isBuiltIn)
        if (customBlocks.isEmpty()) {
            Text(stringResource(R.string.home_build_your_sequence))
        } else {
            customBlocks.forEach { block ->
                BlockCard(
                    block = block,
                    onStart = { onStartBlock(block.id) },
                    onEdit = { onEditBlock(block.id) },
                    onDelete = { onDeleteBlock(block.id) },
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_my_exercises))
        }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_settings))
        }
        Text(
            stringResource(R.string.home_medical_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BlockCard(
    block: WorkoutBlock,
    onStart: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                localizedBlockName(block),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                pluralStringResource(R.plurals.exercise_count, block.exerciseIds.size, block.exerciseIds.size),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_start))
                }
                if (onEdit != null) {
                    OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.common_edit)) }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete)) }
                }
            }
        }
    }
}
