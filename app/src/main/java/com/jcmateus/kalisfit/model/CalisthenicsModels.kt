package com.jcmateus.kalisfit.model

import com.google.firebase.firestore.PropertyName


data class ExerciseLevel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val targetReps: String? = null,
    val targetSets: String? = null,
    val targetHoldTime: String? = null,
    val videoUrl: String? = null,
    val notes: String? = null,
    val imageUrl: String? = null,
    @get:PropertyName("orden") // Le dice a Firestore que para obtener este valor, busque "orden"
    val order: Int = 0 // Para asegurar el orden de los niveles
)

data class Progression( // Esta es la plantilla de la progresión
    val id: String = "",
    val name: String = "",
    val iconUrl: String? = null,
    val description: String? = null,
    val levels: List<ExerciseLevel> = emptyList() // Asegúrate de que los niveles aquí estén ordenados por 'order' al crearlos/cargarlos
)

// NUEVO MODELO: Para el estado del progreso de un usuario en una progresión específica
data class UserProgressionState(
    // Podrías usar un ID compuesto en Firestore (userId_progressionId) o campos separados.
    // Para el modelo en Kotlin, campos separados son más claros.
    val userId: String = "",
    val progressionId: String = "", // ID de la Progression a la que se refiere este estado
    var lastCompletedLevelId: String? = null,
    var currentAttemptLevelId: String? = null, // El nivel que se está intentando actualmente
    val lastUpdated: Long = System.currentTimeMillis(), // Opcional: para saber cuándo se actualizó
    // Podrías añadir también:
     val completedLevelIds: List<String> = emptyList(), // Si quieres una lista de todos los completados
     val attempts: Map<String, Int> = emptyMap() // Map de levelId a número de intentos
)

// --- Funciones de Extensión para UserProgressionState ---

/**
 * Verifica si un nivel específico está marcado como completado.
 * Puedes basarte en `lastCompletedLevelId` o en la lista `completedLevelIds`.
 * Usar `completedLevelIds` es más robusto si permites completar niveles fuera de orden
 * o si quieres marcar niveles como "re-completados".
 */
fun UserProgressionState.isLevelCompleted(levelId: String): Boolean {
    // Opción 1: Basado en la lista de IDs completados (más flexible)
    return this.completedLevelIds.contains(levelId)

    // Opción 2: Basado solo en el último completado (si el progreso es estrictamente lineal
    // y no te importa si un nivel anterior fue completado si ya se avanzó más allá)
    // val lastCompletedOrder = allLevelsInProgression.find { it.id == this.lastCompletedLevelId }?.order ?: -1
    // val currentLevelOrder = allLevelsInProgression.find { it.id == levelId }?.order ?: Int.MAX_VALUE
    // return currentLevelOrder <= lastCompletedOrder && lastCompletedOrder != -1
}

/**
 * Determina si el nivel actual es el siguiente nivel lógico para el usuario
 * o si ya ha sido superado (completado o se completó uno posterior).
 *
 * @param levelId El ID del nivel que se está visualizando.
 * @param currentLevelDetails Los detalles del nivel que se está visualizando.
 * @param allLevelsInProgression Lista de todos los niveles en la progresión actual, ordenados.
 */
fun UserProgressionState.isLevelNextOrPast(
    levelId: String,
    currentLevelDetails: ExerciseLevel, // Necesitamos su 'order'
    allLevelsInProgression: List<ExerciseLevel> // Asume que está ordenada por 'order'
): Boolean {
    if (allLevelsInProgression.isEmpty()) return false // No hay niveles para comparar

    // Si el nivel actual ya está completado, entonces es "pasado".
    if (isLevelCompleted(levelId)) {
        return true
    }

    // Encuentra el último nivel completado real (si existe)
    val lastActuallyCompletedLevel = allLevelsInProgression
        .filter { completedLevelIds.contains(it.id) }
        .maxByOrNull { it.order }

    val currentLevelOrder = currentLevelDetails.order

    if (lastActuallyCompletedLevel == null) {
        // No se ha completado ningún nivel.
        // El nivel actual es el "siguiente" si es el primero en la lista (order más bajo).
        val firstLevelInProgression = allLevelsInProgression.minByOrNull { it.order }
        return firstLevelInProgression?.id == levelId
    } else {
        // Se ha completado al menos un nivel.
        val lastCompletedOrder = lastActuallyCompletedLevel.order

        // Si el nivel actual es posterior al último completado, no es el siguiente ni pasado (a menos que lo permitas)
        if (currentLevelOrder > lastCompletedOrder) {
            // Es el "siguiente" si no hay niveles intermedios sin completar.
            // Esto significa que todos los niveles con 'order' entre lastCompletedOrder y currentLevelOrder
            // deberían estar ya en completedLevelIds (lo cual no debería pasar si este es el siguiente).
            // Más simple: es el siguiente si es el primer nivel con 'order' > lastCompletedOrder.
            val nextLogicalLevel = allLevelsInProgression
                .filter { it.order > lastCompletedOrder }
                .minByOrNull { it.order }
            return nextLogicalLevel?.id == levelId
        } else {
            // El nivel actual tiene un 'order' <= al último completado.
            // Como ya comprobamos que NO está en `completedLevelIds` al inicio de la función,
            // esto significaría que es un nivel anterior que no se completó (o se "desmarcó").
            // En este contexto, no sería el "siguiente" ni "pasado" en el sentido de estar habilitado para completar.
            // Pero si la pregunta es si "está en un punto que podría ser relevante o ya fue superado",
            // y no está completado, entonces podría ser falso.
            // Sin embargo, si la lógica es "mostrar botón 'completar' para el primer nivel no completado
            // en o después del último completado", entonces esta lógica necesitaría ajuste.

            // Para la lógica de "mostrar botón completar":
            // Queremos habilitar el botón si este `levelId` es el primer nivel NO COMPLETADO
            // que viene *después* del `lastActuallyCompletedLevel.order` o si es el primer nivel
            // de todos si `lastActuallyCompletedLevel` es null.

            // Reformulando `isLevelNextOrPast` para el botón "Marcar como Completado":
            // El botón debe aparecer si:
            // 1. El nivel NO está completado AÚN. (Esto ya lo manejas fuera: `!isLevelCompleted`)
            // 2. Es el primer nivel de la progresión y ningún otro está completado.
            // 3. O es el primer nivel *después* del último nivel completado (`lastActuallyCompletedLevel.order`).

            // Esta función tal como está definida arriba, es más bien: "¿Está este nivel en un punto donde el usuario ya llegó o superó?"
            // Si la necesitas estrictamente para "es el próximo para marcar como completo":

            // Si el nivel actual ya está completado, no es el "siguiente a completar".
            // Esta condición ya está fuera (`!isLevelCompleted`), así que podemos omitirla aquí si
            // el propósito de `isLevelNextOrPast` es *SOLO* para habilitar el botón.

            // Caso: Ningún nivel completado.
            if (lastActuallyCompletedLevel == null) {
                val firstLevelInProgression = allLevelsInProgression.minByOrNull { it.order }
                return firstLevelInProgression?.id == levelId
            }

            // Caso: Hay niveles completados.
            // Es el "siguiente" si es el primer nivel con 'order' > lastCompletedOrder que *no* está en completedLevelIds.
            // (La parte de "no está en completedLevelIds" ya está implícita si llegamos aquí y !isLevelCompleted(levelId))
            val nextLogicalLevelOrder = allLevelsInProgression
                .filter { it.order > lastActuallyCompletedLevel.order && !completedLevelIds.contains(it.id) }
                .minByOrNull { it.order }

            if (nextLogicalLevelOrder != null) {
                return nextLogicalLevelOrder.id == levelId
            }

            // Si el nivel actual es anterior o igual al último completado, pero no está completo él mismo,
            // y no hay un "siguiente lógico" después del último completado,
            // entonces no es el "siguiente" ni "pasado" en el sentido de progreso lineal.
            // Por ejemplo, si completó nivel 1 y 3, pero está viendo nivel 2 (no completo).
            // Nivel 2 no es "pasado" (porque no está completo) y no es el "siguiente" después del 3.
            // El botón de completar para el nivel 2 SÍ debería aparecer en este caso.

            // Simplificación para el botón "Marcar como completado":
            // El botón debe aparecer si el nivel NO está completado, Y
            // ( (no hay niveles completados Y este es el primer nivel de la progresión) OR
            //   (hay niveles completados Y este nivel es el primero con order > order del último completado Y no está completo) OR
            //   (este nivel tiene un order <= al último completado pero no está completo él mismo) ) <= Esto último permite completar niveles "saltados".

            // La lógica del botón en tu Composable era: `if (isNextLevelOrPast && !isLevelCompleted)`
            // Si `isLevelCompleted(levelId)` es true, entonces `isLevelNextOrPast` debería ser true.

            // Vamos a redefinir `isLevelInteractable` para mayor claridad para el botón:
            // Un nivel es "interactuabLe" (se puede marcar como completado) si NO está completado AÚN, Y
            // 1. No hay ningún nivel completado todavía Y este es el primer nivel de la progresión (el de menor 'order').
            // 2. O bien, el nivel anterior (en orden) a este `currentLevelDetails` SÍ está completado.
            // 3. O bien, no hay un "nivel anterior" (es el primero) y ninguno está completo.

            // Esto se vuelve complejo y depende de si permites completar en cualquier orden o estrictamente lineal.
            // Asumiendo un progreso mayormente lineal donde quieres habilitar el *siguiente* nivel no completado:

            if (lastActuallyCompletedLevel == null) { // Ninguno completado
                return allLevelsInProgression.minByOrNull { it.order }?.id == levelId
            } else { // Al menos uno completado
                // Si el nivel actual es posterior al último completado
                if (currentLevelOrder > lastActuallyCompletedLevel.order) {
                    // Es el "siguiente" si es el primer nivel no completado después del último completado
                    val firstNonCompletedAfterLast = allLevelsInProgression
                        .filter { it.order > lastActuallyCompletedLevel.order && !completedLevelIds.contains(it.id) }
                        .minByOrNull { it.order }
                    return firstNonCompletedAfterLast?.id == levelId
                } else {
                    // El nivel actual es anterior o igual al último completado.
                    // Si no está completo, sí se podría considerar "interactuable" para completar (si permites rellenar huecos).
                    // Si la regla es estrictamente "solo el siguiente después del último completado", entonces esto sería 'false'.
                    // Para tu botón `if (isNextLevelOrPast && !isLevelCompleted)`:
                    // Si `isLevelCompleted` es falso, y es un nivel anterior al `lastActuallyCompletedLevel`,
                    // `isNextLevelOrPast` debería ser `true` para que se pueda completar.
                    return true // Permite completar niveles anteriores no completados
                }
            }
        }
    }
}