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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.projectworkout.kneetempo.domain.model.AppSettings
import ru.projectworkout.kneetempo.domain.model.UpdateState

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
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("← На главную") }
        Text("Настройки", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Тренировка", style = MaterialTheme.typography.titleLarge)
                SwitchSetting(
                    title = "Звуковые сигналы",
                    description = "Сигнал смены фазы и отсчет последних секунд",
                    checked = settings.soundEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(soundEnabled = it)) },
                )
                SwitchSetting(
                    title = "Не выключать экран",
                    description = "Оставлять экран включенным во время тренировки",
                    checked = settings.keepScreenOnDuringWorkout,
                    onCheckedChange = { onSettingsChange(settings.copy(keepScreenOnDuringWorkout = it)) },
                )
                SecondsSetting(
                    title = "Подготовка к упражнению",
                    seconds = settings.preparationSeconds,
                    minimum = MIN_PREPARATION_SECONDS,
                    maximum = MAX_PREPARATION_SECONDS,
                    step = PREPARATION_STEP_SECONDS,
                    onChange = { onSettingsChange(settings.copy(preparationSeconds = it)) },
                )
                SecondsSetting(
                    title = "Отдых между подходами",
                    seconds = settings.setRestSeconds,
                    minimum = MIN_REST_SECONDS,
                    maximum = MAX_REST_SECONDS,
                    step = REST_STEP_SECONDS,
                    onChange = { onSettingsChange(settings.copy(setRestSeconds = it)) },
                )
                SecondsSetting(
                    title = "Отдых между упражнениями",
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
                Text("Обновление приложения", style = MaterialTheme.typography.titleLarge)
                Text("Установлена версия $appVersion")
                when (updateState) {
                    UpdateState.Idle -> Text("Проверьте, доступен ли новый выпуск.")
                    UpdateState.Checking -> Text("Проверяем GitHub Releases…")
                    UpdateState.Downloading -> Text("Скачиваем обновление…")
                    is UpdateState.Error -> Text(updateState.message, color = MaterialTheme.colorScheme.error)
                    is UpdateState.InstallPermissionRequired -> {
                        Text(updateState.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onInstallUpdate, modifier = Modifier.fillMaxWidth()) {
                            Text("Установить обновление")
                        }
                        OutlinedButton(
                            onClick = { onOpenRelease(updateState.result.release.pageUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Открыть выпуск на GitHub")
                        }
                    }
                    is UpdateState.Result -> {
                        val release = updateState.release
                        Text(
                            when {
                                updateState.updateAvailable && updateState.versionComparable ->
                                    "Доступно обновление ${release.name} (${release.tag})."
                                updateState.updateAvailable ->
                                    "Найден выпуск ${release.name} (${release.tag}); его версию нельзя автоматически сравнить с установленной."
                                updateState.versionComparable ->
                                    "Новых версий нет. Последний выпуск: ${release.name} (${release.tag})."
                                else ->
                                    "Последний выпуск: ${release.name} (${release.tag}). Версию нельзя автоматически сравнить с установленной."
                            },
                        )
                        if (updateState.updateAvailable) {
                            Button(onClick = onInstallUpdate, modifier = Modifier.fillMaxWidth()) {
                                Text("Скачать и установить")
                            }
                            Text(
                                "Установку потребуется подтвердить в системном окне Android.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { onOpenRelease(release.pageUrl) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Открыть выпуск на GitHub")
                        }
                    }
                }
                OutlinedButton(
                    onClick = onCheckForUpdates,
                    enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Проверить обновления")
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
        Text("$seconds с", style = MaterialTheme.typography.titleMedium)
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
