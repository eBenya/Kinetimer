package ru.projectworkout.kneetempo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.os.LocaleListCompat
import ru.projectworkout.kneetempo.data.AppSettingsRepository
import ru.projectworkout.kneetempo.domain.model.AppLanguage
import ru.projectworkout.kneetempo.ui.WorkoutApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsRepository = AppSettingsRepository(this)
        val settings = settingsRepository.load()
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val systemSelectedLanguage =
            appLocales.get(0)?.language?.let { languageTag ->
                AppLanguage.entries.firstOrNull { it.preferenceValue == languageTag }
            }
        val language = systemSelectedLanguage ?: settings.language
        if (language != settings.language) {
            settingsRepository.save(settings.copy(language = language))
        }
        val selectedLocales = LocaleListCompat.forLanguageTags(language.preferenceValue)
        if (appLocales != selectedLocales) {
            AppCompatDelegate.setApplicationLocales(selectedLocales)
        }
        setContent {
            MaterialTheme(
                colorScheme =
                    lightColorScheme(
                        primary = Color(0xFF176B58),
                        onPrimary = Color.White,
                        secondary = Color(0xFFE9A23B),
                        background = Color(0xFFF7F8F5),
                        surface = Color.White,
                    ),
            ) {
                WorkoutApp()
            }
        }
    }
}
