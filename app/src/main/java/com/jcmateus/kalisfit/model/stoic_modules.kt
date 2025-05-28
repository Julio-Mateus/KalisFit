package com.jcmateus.kalisfit.model

data class StoicModule(
    val id: String = "", // ID del documento de Firestore
    val order: Int = 0, // Para secuenciar los módulos
    val title: String = "",
    val introduction: String = "", // Texto introductorio del módulo
    val theoryContent: List<TheoryParagraph> = emptyList(), // Párrafos de teoría
    val quotes: List<StoicQuote> = emptyList(),
    val exercises: List<StoicExerciseDefinition> = emptyList(), // Definiciones de ejercicios
    val keyTakeaway: String = "" // Mensaje clave para llevar
)

data class TheoryParagraph(
    val subtitle: String? = null, // Subtítulo opcional dentro de la teoría
    val text: String = ""
)

data class StoicQuote(
    val text: String = "",
    val author: String = ""
)

// Tipo de ejercicio (podría expandirse)
enum class StoicExerciseType {
    REFLECTION_TEXT, // Usuario escribe una reflexión
    MULTIPLE_CHOICE, // Pregunta de opción múltiple (simple, para reforzar)
    ACTION_PROMPT    // Un recordatorio de acción
}

data class StoicExerciseDefinition(
    val id: String = "", // ID único para el ejercicio dentro del módulo
    val title: String = "",
    val description: String = "",
    val type: StoicExerciseType = StoicExerciseType.REFLECTION_TEXT,
    val questionPrompt: String? = null, // Para REFLECTION_TEXT o MULTIPLE_CHOICE
    val choices: List<String>? = null, // Para MULTIPLE_CHOICE
    val correctAnswerIndex: Int? = null // Para MULTIPLE_CHOICE (opcional, si quieres validar)
)
