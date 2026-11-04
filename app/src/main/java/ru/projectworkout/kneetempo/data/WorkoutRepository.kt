package ru.projectworkout.kneetempo.data

import ru.projectworkout.kneetempo.domain.model.Exercise
import ru.projectworkout.kneetempo.domain.model.MovementPhase
import ru.projectworkout.kneetempo.domain.model.WorkoutBlock

object WorkoutRepository {
    private val gymExercises =
        listOf(
            Exercise(
                id = "gym-adduction",
                name = "Сведение ног в тренажере сидя",
                technique = listOf("Упор на бедра.", "Сводите ноги плавно.", "Не бросайте вес при возврате."),
                sets = 3,
                repetitions = 15,
                phases =
                    listOf(
                        MovementPhase("Сведение — медленно", 3),
                        MovementPhase("Удержание", 2),
                        MovementPhase("Разведение — медленно", 2),
                    ),
            ),
            Exercise(
                id = "gym-abduction",
                name = "Отведение ног в тренажере",
                technique = listOf("Держите корпус неподвижно.", "Отводите ноги без рывка.", "Возвращайте вес под контролем."),
                sets = 3,
                repetitions = 15,
                phases =
                    listOf(
                        MovementPhase("Отведение — медленно", 2),
                        MovementPhase("Удержание", 2),
                        MovementPhase("Возврат — медленно", 2),
                    ),
            ),
            Exercise(
                id = "gym-leg-press",
                name = "Жим ногами в платформе",
                technique = listOf("Стопы выше центра платформы.", "Не сгибайте колени глубже 90°.", "Не блокируйте колени при подъеме."),
                sets = 3,
                repetitions = 15,
                phases =
                    listOf(
                        MovementPhase("Опускание — медленно", 4),
                        MovementPhase("Пауза", 1),
                        MovementPhase("Подъем — медленно", 2),
                    ),
            ),
            Exercise(
                id = "gym-leg-curl",
                name = "Сгибание ног в тренажере лежа",
                technique = listOf("Таз остается на скамье.", "Сгибайте ноги плавно.", "Полностью контролируйте разгибание."),
                sets = 3,
                repetitions = 15,
                phases =
                    listOf(
                        MovementPhase("Сгибание — медленно", 2),
                        MovementPhase("Удержание", 2),
                        MovementPhase("Разгибание — медленно", 3),
                    ),
            ),
        )

    private val homeExercises =
        listOf(
            Exercise(
                id = "home-quad-set",
                name = "Напряжение квадрицепса с валиком",
                technique = listOf("Валик под коленом.", "Прижмите колено вниз и напрягите бедро.", "Затем полностью расслабьтесь."),
                sets = 3,
                repetitions = 12,
                phases =
                    listOf(
                        MovementPhase("Давите коленом вниз", 5),
                        MovementPhase("Расслабление", 3),
                    ),
            ),
            Exercise(
                id = "home-straight-leg-raise",
                name = "Подъем прямой ноги лежа",
                technique = listOf("Лежа на спине, здоровая нога согнута.", "Поднимите прямую ногу без рывка.", "Опускайте медленно."),
                sets = 3,
                repetitions = 15,
                phases =
                    listOf(
                        MovementPhase("Подъем — медленно", 2),
                        MovementPhase("Фиксация вверху", 2),
                        MovementPhase("Опускание — медленно", 2),
                    ),
            ),
            Exercise(
                id = "home-side-leg-raise",
                name = "Отведение ноги лежа на боку",
                technique = listOf("Лежите устойчиво на боку.", "Поднимайте ногу без разворота таза.", "Плавно верните ногу."),
                sets = 3,
                repetitions = 15,
                phases =
                    listOf(
                        MovementPhase("Подъем — медленно", 2),
                        MovementPhase("Фиксация", 1),
                        MovementPhase("Опускание — медленно", 2),
                    ),
            ),
            Exercise(
                id = "home-wall-sit",
                name = "Стульчик у стены",
                technique = listOf("Спина прижата к стене.", "Колени не глубже комфортного угла.", "Удерживайте положение без боли."),
                sets = 3,
                repetitions = 1,
                phases = listOf(MovementPhase("Статическое удержание", 30)),
            ),
            Exercise(
                id = "home-calf-stretch",
                name = "Растяжка икроножной мышцы у стены",
                technique = listOf("Упритесь руками в стену.", "Пятка задней ноги на полу.", "Тянитесь мягко, без пружинящих движений."),
                sets = 3,
                repetitions = 1,
                phases = listOf(MovementPhase("Статическое удержание", 45)),
            ),
        )

    val allExercises: List<Exercise>
        get() = gymExercises + homeExercises

    val builtInBlocks =
        listOf(
            WorkoutBlock(
                id = "gym",
                name = "Тренировка в зале",
                exerciseIds = gymExercises.map(Exercise::id),
                isBuiltIn = true,
            ),
            WorkoutBlock(
                id = "home",
                name = "Домашняя тренировка",
                exerciseIds = homeExercises.map(Exercise::id),
                isBuiltIn = true,
            ),
        )
}
