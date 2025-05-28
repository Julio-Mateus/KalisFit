package com.jcmateus.kalisfit.model

data class UserStoicProgress(
    val userId: String = "",
    val completedModuleIds: List<String> = emptyList(), // IDs de StoicModule completados
    val currentModuleId: String? = null, // El módulo en el que está actualmente
    val exerciseResponses: Map<String, UserExerciseResponse> = emptyMap() // Key: moduleExerciseId (ej. "module1_exerciseA")
)

data class UserExerciseResponse(
    val exerciseId: String = "", // ID del StoicExerciseDefinition
    val moduleId: String = "",
    val responseText: String? = null, // Para REFLECTION_TEXT
    val selectedChoiceIndex: Int? = null, // Para MULTIPLE_CHOICE
    val completed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)