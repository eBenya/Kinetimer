package ru.projectworkout.kinetimer.ui.screens

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import ru.projectworkout.kinetimer.R
import ru.projectworkout.kinetimer.domain.model.Exercise
import ru.projectworkout.kinetimer.domain.model.WorkoutBlock
import java.util.Locale

@Composable
fun localizedBlockName(block: WorkoutBlock): String {
    val resourceId =
        when (block.id) {
            "gym" -> R.string.builtin_block_gym
            "home" -> R.string.builtin_block_home
            else -> return block.name
        }
    return localizedBuiltInValue(block.name, resourceId, block.isBuiltIn)
}

@Composable
fun localizedExerciseName(exercise: Exercise): String {
    val resourceId =
        when (exercise.id) {
            "gym-adduction" -> R.string.builtin_gym_adduction_name
            "gym-abduction" -> R.string.builtin_gym_abduction_name
            "gym-leg-press" -> R.string.builtin_gym_leg_press_name
            "gym-leg-curl" -> R.string.builtin_gym_leg_curl_name
            "home-quad-set" -> R.string.builtin_home_quad_name
            "home-straight-leg-raise" -> R.string.builtin_home_leg_raise_name
            "home-side-leg-raise" -> R.string.builtin_home_side_raise_name
            "home-wall-sit" -> R.string.builtin_home_wall_sit_name
            "home-calf-stretch" -> R.string.builtin_home_calf_stretch_name
            else -> return exercise.name
        }
    return localizedBuiltInValue(exercise.name, resourceId, !exercise.id.startsWith("custom-"))
}

@Composable
fun localizedExerciseTip(
    exercise: Exercise,
    index: Int,
): String {
    val resourceId =
        when (exercise.id) {
            "gym-adduction" -> listOf(R.string.builtin_gym_adduction_tip_1, R.string.builtin_gym_adduction_tip_2, R.string.builtin_gym_adduction_tip_3)
            "gym-abduction" -> listOf(R.string.builtin_gym_abduction_tip_1, R.string.builtin_gym_abduction_tip_2, R.string.builtin_gym_abduction_tip_3)
            "gym-leg-press" -> listOf(R.string.builtin_gym_leg_press_tip_1, R.string.builtin_gym_leg_press_tip_2, R.string.builtin_gym_leg_press_tip_3)
            "gym-leg-curl" -> listOf(R.string.builtin_gym_leg_curl_tip_1, R.string.builtin_gym_leg_curl_tip_2, R.string.builtin_gym_leg_curl_tip_3)
            "home-quad-set" -> listOf(R.string.builtin_home_quad_tip_1, R.string.builtin_home_quad_tip_2, R.string.builtin_home_quad_tip_3)
            "home-straight-leg-raise" -> listOf(R.string.builtin_home_leg_raise_tip_1, R.string.builtin_home_leg_raise_tip_2, R.string.builtin_home_leg_raise_tip_3)
            "home-side-leg-raise" -> listOf(R.string.builtin_home_side_raise_tip_1, R.string.builtin_home_side_raise_tip_2, R.string.builtin_home_side_raise_tip_3)
            "home-wall-sit" -> listOf(R.string.builtin_home_wall_sit_tip_1, R.string.builtin_home_wall_sit_tip_2, R.string.builtin_home_wall_sit_tip_3)
            "home-calf-stretch" -> listOf(R.string.builtin_home_calf_stretch_tip_1, R.string.builtin_home_calf_stretch_tip_2, R.string.builtin_home_calf_stretch_tip_3)
            else -> return exercise.technique.getOrElse(index) { "" }
        }
    val value = exercise.technique.getOrElse(index) { return "" }
    return localizedBuiltInValue(value, resourceId.getOrNull(index) ?: return value, true)
}

@Composable
fun localizedExercisePhase(
    exercise: Exercise,
    index: Int,
): String {
    val resourceIds =
        when (exercise.id) {
            "gym-adduction" -> listOf(R.string.builtin_gym_adduction_phase_1, R.string.builtin_gym_adduction_phase_2, R.string.builtin_gym_adduction_phase_3)
            "gym-abduction" -> listOf(R.string.builtin_gym_abduction_phase_1, R.string.builtin_gym_abduction_phase_2, R.string.builtin_gym_abduction_phase_3)
            "gym-leg-press" -> listOf(R.string.builtin_gym_leg_press_phase_1, R.string.builtin_gym_leg_press_phase_2, R.string.builtin_gym_leg_press_phase_3)
            "gym-leg-curl" -> listOf(R.string.builtin_gym_leg_curl_phase_1, R.string.builtin_gym_leg_curl_phase_2, R.string.builtin_gym_leg_curl_phase_3)
            "home-quad-set" -> listOf(R.string.builtin_home_quad_phase_1, R.string.builtin_home_quad_phase_2)
            "home-straight-leg-raise" -> listOf(R.string.builtin_home_leg_raise_phase_1, R.string.builtin_home_leg_raise_phase_2, R.string.builtin_home_leg_raise_phase_3)
            "home-side-leg-raise" -> listOf(R.string.builtin_home_side_raise_phase_1, R.string.builtin_home_side_raise_phase_2, R.string.builtin_home_side_raise_phase_3)
            "home-wall-sit", "home-calf-stretch" -> listOf(R.string.builtin_home_static_hold)
            else -> return exercise.phases.getOrNull(index)?.title.orEmpty()
        }
    val value = exercise.phases.getOrNull(index)?.title ?: return ""
    return localizedBuiltInValue(value, resourceIds.getOrNull(index) ?: return value, true)
}

@Composable
private fun localizedBuiltInValue(
    value: String,
    resourceId: Int,
    builtIn: Boolean,
): String {
    if (!builtIn) return value
    val context = LocalContext.current
    return if (value == context.russianString(resourceId)) {
        stringResource(resourceId)
    } else {
        value
    }
}

private fun Context.russianString(resourceId: Int): String {
    val configuration = Configuration(resources.configuration).apply {
        setLocale(Locale("ru"))
    }
    return createConfigurationContext(configuration).getString(resourceId)
}
