package com.jcmateus.kalisfit.model


data class StoicModule(
    val id: String = "",
    val order: Int = 0,
    val title: String = "",
    val introduction: String = "",
    val theoryContent: List<TheoryParagraph> = emptyList(),
    val quotes: List<StoicQuote> = emptyList(),
    val exercises: List<StoicExerciseDefinition> = emptyList(),
    // Para keyTakeaway: Si este campo NO está en tu JSON de Firestore
    // y no planeas añadirlo pronto, es MEJOR hacerlo nullable o quitarlo
    // para evitar problemas de deserialización si un documento no lo tiene.
    // Si SÍ está o lo vas a añadir, String = "" está bien.
    val keyTakeaway: String? = null // <- RECOMENDACIÓN: Hacerlo nullable si no siempre está presente
) {
    // Constructor sin argumentos para Firestore
    constructor() : this("", 0, "", "", emptyList(), emptyList(), emptyList(), null)
}

data class TheoryParagraph(
    val id: String = "",
    val subtitle: String? = null,
    val text: String = ""
) {
    // Constructor sin argumentos para Firestore
    constructor() : this("", null, "")
}

data class StoicQuote(
    val id: String = "",
    val text: String = "",
    val author: String = ""
) {
    // Constructor sin argumentos para Firestore
    constructor() : this("", "", "")
}

enum class StoicExerciseType {
    REFLECTION_TEXT,
    MULTIPLE_CHOICE,
    ACTION_PROMPT,
    CHECKLIST
}

data class StoicExerciseDefinition(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: StoicExerciseType = StoicExerciseType.REFLECTION_TEXT,
    val questionPrompt: String? = null,
    val choices: List<String>? = null,      // Para MULTIPLE_CHOICE
    val correctAnswerIndex: Int? = null,   // Para MULTIPLE_CHOICE (opcional, si quieres validar)
    val items: List<String>? = null,        // Para CHECKLIST (lista de textos de los ítems)
    val isRequired: Boolean = false         // Indica si el ejercicio es obligatorio para completar el módulo
) {
    // Constructor sin argumentos para Firestore
    constructor() : this(
        id = "",
        title = "",
        description = "",
        type = StoicExerciseType.REFLECTION_TEXT,
        questionPrompt = null,
        choices = null,
        correctAnswerIndex = null,
        items = null,
        isRequired = false
    )
}
