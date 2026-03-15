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
    val formCues: List<String> = emptyList(),
    val commonMistakes: List<String> = emptyList(),
    @get:PropertyName("orden")
    val order: Int = 0
)

data class Progression(
    val id: String = "",
    val name: String = "",
    val iconUrl: String? = null,
    val description: String? = null,
    val category: String = "General",
    val difficulty: String = "Principiante",
    val prerequisites: String? = null,
    val levels: List<ExerciseLevel> = emptyList()
)

data class UserProgressionState(
    val userId: String = "",
    val progressionId: String = "",
    var lastCompletedLevelId: String? = null,
    var currentAttemptLevelId: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val completedLevelIds: List<String> = emptyList(),
    val attempts: Map<String, Int> = emptyMap()
)

fun UserProgressionState.isLevelCompleted(levelId: String): Boolean {
    return this.completedLevelIds.contains(levelId)
}

fun UserProgressionState.isLevelNextOrPast(
    levelId: String,
    currentLevelDetails: ExerciseLevel,
    allLevelsInProgression: List<ExerciseLevel>
): Boolean {
    if (allLevelsInProgression.isEmpty()) return false

    if (isLevelCompleted(levelId)) {
        return true
    }

    val lastActuallyCompletedLevel = allLevelsInProgression
        .filter { completedLevelIds.contains(it.id) }
        .maxByOrNull { it.order }

    val currentLevelOrder = currentLevelDetails.order

    if (lastActuallyCompletedLevel == null) {
        val firstLevelInProgression = allLevelsInProgression.minByOrNull { it.order }
        return firstLevelInProgression?.id == levelId
    } else {
        val lastCompletedOrder = lastActuallyCompletedLevel.order

        if (currentLevelOrder > lastCompletedOrder) {
            val nextLogicalLevel = allLevelsInProgression
                .filter { it.order > lastCompletedOrder }
                .minByOrNull { it.order }
            return nextLogicalLevel?.id == levelId
        } else {
            return true
        }
    }
}