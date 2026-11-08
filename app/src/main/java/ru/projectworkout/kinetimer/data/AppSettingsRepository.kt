package ru.projectworkout.kinetimer.data

import android.content.Context
import ru.projectworkout.kinetimer.domain.model.AppLanguage
import ru.projectworkout.kinetimer.domain.model.AppSettings

class AppSettingsRepository(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings =
        AppSettings(
            soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true),
            preparationSeconds =
                preferences.getInt(KEY_PREPARATION_SECONDS, DEFAULT_PREPARATION_SECONDS).coerceIn(0, 30),
            setRestSeconds = preferences.getInt(KEY_SET_REST_SECONDS, DEFAULT_SET_REST_SECONDS).coerceIn(0, 300),
            exerciseRestSeconds =
                preferences.getInt(KEY_EXERCISE_REST_SECONDS, DEFAULT_EXERCISE_REST_SECONDS).coerceIn(0, 300),
            keepScreenOnDuringWorkout = preferences.getBoolean(KEY_KEEP_SCREEN_ON, true),
            language = AppLanguage.fromPreferenceValue(preferences.getString(KEY_LANGUAGE, AppLanguage.Russian.preferenceValue)),
        )

    fun save(settings: AppSettings) {
        preferences.edit()
            .putBoolean(KEY_SOUND_ENABLED, settings.soundEnabled)
            .putInt(KEY_PREPARATION_SECONDS, settings.preparationSeconds)
            .putInt(KEY_SET_REST_SECONDS, settings.setRestSeconds)
            .putInt(KEY_EXERCISE_REST_SECONDS, settings.exerciseRestSeconds)
            .putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOnDuringWorkout)
            .putString(KEY_LANGUAGE, settings.language.preferenceValue)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_PREPARATION_SECONDS = "preparation_seconds"
        const val KEY_SET_REST_SECONDS = "set_rest_seconds"
        const val KEY_EXERCISE_REST_SECONDS = "exercise_rest_seconds"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_LANGUAGE = "language"
        const val DEFAULT_PREPARATION_SECONDS = 5
        const val DEFAULT_SET_REST_SECONDS = 60
        const val DEFAULT_EXERCISE_REST_SECONDS = 90
    }
}
