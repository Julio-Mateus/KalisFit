package com.jcmateus.kalisfit.model

data class UserExerciseResponse(
    val exerciseId: String = "", // ID simple del StoicExerciseDefinition
    val moduleId: String = "",   // ID del StoicModule al que pertenece
    var responseText: String? = null,
    var selectedChoiceIndex: Int? = null,
    var completed: Boolean? = null, // Usado para ACTION_PROMPT y como un flag general
    var checklistResponses: List<Boolean>? = null, // Lista de booleanos para CHECKLIST
    var timestamp: Long = System.currentTimeMillis() // Momento de la última actualización
) {
    // Constructor sin argumentos para Firestore
    constructor() : this(
        exerciseId = "",
        moduleId = "",
        responseText = null,
        selectedChoiceIndex = null,
        completed = null,
        checklistResponses = null,
        timestamp = System.currentTimeMillis()
    )

    // Función para determinar si la respuesta se considera "completa" para la lógica de habilitar "completar módulo"
    fun isConsideredRespondedOrCompleted(exerciseDefinition: StoicExerciseDefinition): Boolean {
        if (!exerciseDefinition.isRequired) return true // Los no obligatorios no bloquean

        return when (exerciseDefinition.type) {
            StoicExerciseType.REFLECTION_TEXT -> !responseText.isNullOrBlank()
            StoicExerciseType.MULTIPLE_CHOICE -> selectedChoiceIndex != null
            StoicExerciseType.ACTION_PROMPT -> completed == true
            StoicExerciseType.CHECKLIST -> {
                // El flag 'completed' para CHECKLIST se actualiza en el ViewModel.
                // Podrías tener una lógica más compleja aquí si el flag 'completed'
                // no fuera suficiente, por ejemplo, requerir que todos los items
                // de un checklist obligatorio estén marcados.
                // Pero como el ViewModel ya setea 'completed' basado en (ej.) .all { it },
                // simplemente chequear 'completed' es suficiente.
                this.completed == true
            }
        }
    }
}

data class UserStoicProgress(
    val userId: String = "",
    var currentModuleId: String? = null,
    val completedModuleIds: List<String> = emptyList(),
    // La clave es el ID compuesto "moduleId_exerciseId"
    val exerciseResponses: Map<String, UserExerciseResponse> = emptyMap(),
    // Si usas Timestamp de Firebase:
    // val lastUpdated: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
    // Si usas Long para el timestamp:
    val lastUpdated: Long = System.currentTimeMillis()
) {
    // Constructor sin argumentos para Firestore
    constructor() : this(
        userId = "",
        currentModuleId = null,
        completedModuleIds = emptyList(),
        exerciseResponses = emptyMap(),
        // Si usas Timestamp de Firebase:
        // lastUpdated = com.google.firebase.Timestamp.now()
        // Si usas Long para el timestamp:
        lastUpdated = System.currentTimeMillis()
    )
}