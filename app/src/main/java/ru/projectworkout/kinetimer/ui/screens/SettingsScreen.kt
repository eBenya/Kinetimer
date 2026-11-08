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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.projectworkout.kinetimer.R
import ru.projectworkout.kinetimer.domain.model.AppLanguage
import ru.projectworkout.kinetimer.domain.model.AppSettings
import ru.projectworkout.kinetimer.domain.model.UpdateState

@Composable
fun SettingsScreen(
    appVersion: String,
    settings: AppSettings,
    updateState: UpdateState,
    onSettingsChange: (AppSettings) -> Unit,
    onCheckForUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onOpenRelease: (String) -> Unit,
    onBack: () -> Unit,
) {
    var languageMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.common_back_home)) }
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
                Column {
                    TextButton(onClick = { languageMenuExpanded = true }) {
                        Text(
                            stringResource(
                                if (settings.language == AppLanguage.Russian) {
                                    R.string.settings_language_russian
                                } else {
                                    R.string.settings_language_english
                                },
                            ),
                        )
                    }
                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false },
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (language == AppLanguage.Russian) {
                                                R.string.settings_language_russian
                                            } else {
                                                R.string.settings_language_english
                                            },
                                        ),
                                    )
                                },
                                onClick = {
                                    onSettingsChange(settings.copy(language = language))
                                    languageMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_workout), style = MaterialTheme.typography.titleLarge)
                SwitchSetting(
                    title = stringResource(R.string.settings_sound),
                    description = stringResource(R.string.settings_sound_description),
                    checked = settings.soundEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(soundEnabled = it)) },
                )
                SwitchSetting(
                    title = stringResource(R.string.settings_keep_screen_on),
                    description = stringResource(R.string.settings_keep_screen_on_description),
                    checked = settings.keepScreenOnDuringWorkout,
                    onCheckedChange = { onSettingsChange(settings.copy(keepScreenOnDuringWorkout = it)) },
                )
                SecondsSetting(
                    title = stringResource(R.string.settings_preparation),
                    seconds = settings.preparationSeconds,
                    minimum = MIN_PREPARATION_SECONDS,
                    maximum = MAX_PREPARATION_SECONDS,
                    step = PREPARATION_STEP_SECONDS,
                    onChange = { onSettingsChange(settings.copy(preparationSeconds = it)) },
                )
                SecondsSetting(
                    title = stringResource(R.string.settings_set_rest),
                    seconds = settings.setRestSeconds,
                    minimum = MIN_REST_SECONDS,
                    maximum = MAX_REST_SECONDS,
                    step = REST_STEP_SECONDS,
                    onChange = { onSettingsChange(settings.copy(setRestSeconds = it)) },
                )
                SecondsSetting(
                    title = stringResource(R.string.settings_exercise_rest),
                    seconds = settings.exerciseRestSeconds,
                    minimum = MIN_REST_SECONDS,
                    maximum = MAX_REST_SECONDS,
                    step = REST_STEP_SECONDS,
                    onChange = { onSettingsChange(settings.copy(exerciseRestSeconds = it)) },
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_updates), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.settings_installed_version, appVersion))
                when (updateState) {
                    UpdateState.Idle -> Text(stringResource(R.string.settings_update_idle))
                    UpdateState.Checking -> Text(stringResource(R.string.settings_update_checking))
                    UpdateState.Downloading -> Text(stringResource(R.string.settings_update_downloading))
                    is UpdateState.Error ->
                        Text(stringResource(updateState.messageResId), color = MaterialTheme.colorScheme.error)
                    is UpdateState.InstallPermissionRequired -> {
                        Text(stringResource(updateState.messageResId), color = MaterialTheme.colorScheme.error)
                        Button(onClick = onInstallUpdate, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_update_install))
                        }
                        OutlinedButton(
                            onClick = { onOpenRelease(updateState.result.release.pageUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_update_open_github))
                        }
                    }
                    is UpdateState.Result -> {
                        val release = updateState.release
                        Text(
                            when {
                                updateState.updateAvailable && updateState.versionComparable ->
                                    stringResource(R.string.settings_update_available, release.name, release.tag)
                                updateState.updateAvailable ->
                                    stringResource(R.string.settings_update_found_uncomparable, release.name, release.tag)
                                updateState.versionComparable ->
                                    stringResource(R.string.settings_update_up_to_date, release.name, release.tag)
                                else ->
                                    stringResource(R.string.settings_update_uncomparable, release.name, release.tag)
                            },
                        )
                        if (updateState.updateAvailable) {
                            Button(onClick = onInstallUpdate, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.settings_update_download_install))
                            }
                            Text(
                                stringResource(R.string.settings_update_confirmation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { onOpenRelease(release.pageUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.settings_update_open_github))
                        }
                    }
                }
                OutlinedButton(
                    onClick = onCheckForUpdates,
                    enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_update_check))
                }
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SecondsSetting(
    title: String,
    seconds: Int,
    minimum: Int,
    maximum: Int,
    step: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { onChange((seconds - step).coerceAtLeast(minimum)) }, enabled = seconds > minimum) {
            Text("−")
        }
        Text(
            stringResource(R.string.duration_format, seconds, stringResource(R.string.duration_seconds_short)),
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = { onChange((seconds + step).coerceAtMost(maximum)) }, enabled = seconds < maximum) {
            Text("+")
        }
    }
}

private const val MIN_PREPARATION_SECONDS = 0
private const val MAX_PREPARATION_SECONDS = 30
private const val PREPARATION_STEP_SECONDS = 5
private const val MIN_REST_SECONDS = 0
private const val MAX_REST_SECONDS = 300
private const val REST_STEP_SECONDS = 15
