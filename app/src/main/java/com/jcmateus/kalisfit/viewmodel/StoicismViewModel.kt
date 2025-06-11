package com.jcmateus.kalisfit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.model.StoicExerciseType
import com.jcmateus.kalisfit.model.StoicModule
import com.jcmateus.kalisfit.model.UserExerciseResponse
import com.jcmateus.kalisfit.model.UserStoicProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// CAMBIO 1: Modificar StoicismScreenState para usar UserExerciseResponse
data class StoicismScreenState(
    val isLoadingModules: Boolean = true,
    val isLoadingProgress: Boolean = true,
    val modules: List<StoicModule> = emptyList(),
    val userProgress: UserStoicProgress? = null,
    val activeModule: StoicModule? = null,
    val errorMessage: String? = null,
    val currentExerciseResponses: Map<String, UserExerciseResponse> = emptyMap()
)

class StoicismViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _screenState = MutableStateFlow(StoicismScreenState())
    val screenState: StateFlow<StoicismScreenState> = _screenState.asStateFlow()

    init {
        loadStoicContent()
    }

    private fun generateModuleExerciseId(moduleId: String, exerciseId: String): String {
        return "${moduleId}_${exerciseId}"
    }

    private fun loadStoicContent() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _screenState.update {
                it.copy(
                    errorMessage = "Usuario no autenticado.",
                    isLoadingModules = false,
                    isLoadingProgress = false
                )
            }
            return
        }

        viewModelScope.launch {
            _screenState.update { it.copy(isLoadingModules = true, isLoadingProgress = true) }
            try {
                val modulesSnapshot = db.collection("stoic_modules").orderBy("order").get().await()
                // Asegúrate que StoicModule y sus sub-clases (TheoryParagraph, StoicQuote, StoicExerciseDefinition)
                // tengan constructores sin argumentos para la deserialización de Firestore.
                val modules = modulesSnapshot.toObjects(StoicModule::class.java)

                val progressDoc = db.collection("user_stoic_progress").document(userId).get().await()
                // Asegúrate que UserStoicProgress y UserExerciseResponse tengan constructores sin argumentos.
                var userProgress = progressDoc.toObject(UserStoicProgress::class.java)

                if (userProgress == null) {
                    // Si UserStoicProgress.lastUpdated es Timestamp:
                    // userProgress = UserStoicProgress(userId = userId, currentModuleId = modules.firstOrNull()?.id, lastUpdated = Timestamp.now())
                    // Si UserStoicProgress.lastUpdated es Long:
                    userProgress = UserStoicProgress(userId = userId, currentModuleId = modules.firstOrNull()?.id, lastUpdated = System.currentTimeMillis())
                }

                val activeModuleId = userProgress.currentModuleId
                    ?: modules.firstOrNull { !userProgress.completedModuleIds.contains(it.id) }?.id
                val activeModule = modules.find { it.id == activeModuleId }

                val responsesForActiveModule = mutableMapOf<String, UserExerciseResponse>()
                if (activeModule != null) {
                    activeModule.exercises.forEach { exercise ->
                        val moduleExerciseId = generateModuleExerciseId(activeModule.id, exercise.id)
                        userProgress.exerciseResponses[moduleExerciseId]?.let { savedResponse ->
                            responsesForActiveModule[exercise.id] = savedResponse
                        }
                    }
                }

                _screenState.update {
                    it.copy(
                        isLoadingModules = false,
                        isLoadingProgress = false,
                        modules = modules,
                        userProgress = userProgress,
                        activeModule = activeModule,
                        currentExerciseResponses = responsesForActiveModule,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                _screenState.update {
                    it.copy(
                        isLoadingModules = false,
                        isLoadingProgress = false,
                        errorMessage = "Error al cargar contenido de estoicismo: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateExerciseResponse(
        exerciseId: String,
        responseValue: String,
        exerciseType: StoicExerciseType,
        moduleId: String
    ) {
        _screenState.update { currentState ->
            val updatedLocalResponses = currentState.currentExerciseResponses.toMutableMap()
            val activeModule = currentState.activeModule
            // StoicExerciseDefinition es necesario para CHECKLIST para saber el número de items
            val exerciseDefinition = activeModule?.exercises?.find { it.id == exerciseId }

            val existingResponse = updatedLocalResponses[exerciseId] ?: UserExerciseResponse(
                exerciseId = exerciseId,
                moduleId = moduleId,
                timestamp = System.currentTimeMillis() // Asegurar que el timestamp se inicializa
            )

            var newResponseObject = existingResponse.copy(timestamp = System.currentTimeMillis())

            when (exerciseType) {
                StoicExerciseType.REFLECTION_TEXT -> {
                    newResponseObject = newResponseObject.copy(
                        responseText = responseValue,
                        completed = responseValue.isNotBlank()
                    )
                }
                StoicExerciseType.MULTIPLE_CHOICE -> {
                    val choiceIndex = responseValue.toIntOrNull()
                    newResponseObject = newResponseObject.copy(
                        selectedChoiceIndex = choiceIndex,
                        completed = choiceIndex != null
                    )
                }
                StoicExerciseType.ACTION_PROMPT -> {
                    val isCompleted = responseValue.toBooleanStrictOrNull() ?: existingResponse.completed ?: false
                    newResponseObject = newResponseObject.copy(
                        completed = isCompleted
                    )
                }
                StoicExerciseType.CHECKLIST -> {
                    // Asegurarse de que exerciseDefinition y exerciseDefinition.items no sean null
                    if (exerciseDefinition?.items != null) {
                        val parts = responseValue.split(":")
                        if (parts.size == 2) {
                            val itemIndex = parts[0].toIntOrNull()
                            val isChecked = parts[1].toBooleanStrictOrNull()

                            if (itemIndex != null && isChecked != null && itemIndex >= 0 && itemIndex < exerciseDefinition.items.size) {
                                // Inicializar checklistResponses si es null o con el tamaño correcto
                                val currentChecklist = newResponseObject.checklistResponses?.toMutableList()
                                    ?: MutableList(exerciseDefinition.items.size) { false }

                                // Asegurarse de que la lista sea lo suficientemente grande (aunque debería serlo si se inicializó arriba)
                                if (itemIndex < currentChecklist.size) {
                                    currentChecklist[itemIndex] = isChecked
                                }

                                newResponseObject = newResponseObject.copy(
                                    checklistResponses = currentChecklist.toList(),
                                    completed = currentChecklist.all { it } // Completado si todos los ítems están chequeados
                                )
                            }
                        }
                    }
                }
            }
            updatedLocalResponses[exerciseId] = newResponseObject
            currentState.copy(currentExerciseResponses = updatedLocalResponses)
        }
    }

    fun completeModule(moduleId: String) {
        val userId = auth.currentUser?.uid ?: return
        val currentState = _screenState.value
        val currentProgress = currentState.userProgress ?: return
        val modules = currentState.modules
        val completedModule = modules.find { it.id == moduleId } ?: return

        val allRequiredResponded = completedModule.exercises.all { exDef ->
            if (!exDef.isRequired) {
                true
            } else {
                val response = currentState.currentExerciseResponses[exDef.id]
                // Se usa la función del modelo UserExerciseResponse
                response?.isConsideredRespondedOrCompleted(exDef) == true
            }
        }

        if (!allRequiredResponded) {
            _screenState.update {
                it.copy(errorMessage = "Por favor, completa todos los ejercicios obligatorios antes de continuar.")
            }
            viewModelScope.launch {
                delay(3000) // Usar kotlinx.coroutines.delay
                _screenState.update {
                    if (it.errorMessage == "Por favor, completa todos los ejercicios obligatorios antes de continuar.") {
                        it.copy(errorMessage = null)
                    } else {
                        it // No cambiar si el mensaje ya es otro o null
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            try {
                val updatedGlobalExerciseResponses = currentProgress.exerciseResponses.toMutableMap()
                currentState.currentExerciseResponses.forEach { (exId, userExResponse) ->
                    val moduleExerciseId = generateModuleExerciseId(moduleId, exId)
                    // Asegurarse que el moduleId dentro de UserExerciseResponse está correcto al guardar globalmente
                    updatedGlobalExerciseResponses[moduleExerciseId] = userExResponse.copy(moduleId = moduleId)
                }

                val updatedCompletedModuleIds = currentProgress.completedModuleIds.toMutableList().apply { add(moduleId) }.distinct()

                // Encontrar el siguiente módulo basado en 'order' y que no esté ya completado
                val nextModule = modules
                    .filter { !updatedCompletedModuleIds.contains(it.id) } // Solo módulos no completados
                    .minByOrNull { it.order } // El de menor 'order' entre los no completados
                // (Opcional) Si quieres que sea estrictamente el siguiente después del actual:
                // .filter { it.order > completedModule.order }
                // .minByOrNull { it.order }

                val updatedUserProgress = currentProgress.copy(
                    completedModuleIds = updatedCompletedModuleIds,
                    currentModuleId = nextModule?.id,
                    exerciseResponses = updatedGlobalExerciseResponses,
                    // Si UserStoicProgress.lastUpdated es Timestamp:
                    // lastUpdated = Timestamp.now()
                    // Si UserStoicProgress.lastUpdated es Long:
                    lastUpdated = System.currentTimeMillis()
                )

                db.collection("user_stoic_progress").document(userId).set(updatedUserProgress).await()

                val responsesForNextModule = mutableMapOf<String, UserExerciseResponse>()
                if (nextModule != null) {
                    nextModule.exercises.forEach { exercise ->
                        val moduleExerciseId = generateModuleExerciseId(nextModule.id, exercise.id)
                        updatedUserProgress.exerciseResponses[moduleExerciseId]?.let { savedResponse ->
                            responsesForNextModule[exercise.id] = savedResponse
                        }
                    }
                }

                _screenState.update {
                    it.copy(
                        userProgress = updatedUserProgress,
                        activeModule = nextModule,
                        currentExerciseResponses = responsesForNextModule,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                _screenState.update { it.copy(errorMessage = "Error al guardar progreso: ${e.message}") }
            }
        }
    }

    fun reviewModule(moduleId: String) {
        val currentState = _screenState.value
        val moduleToReview = currentState.modules.find { it.id == moduleId }
        val userProgress = currentState.userProgress

        if (moduleToReview != null && userProgress != null) {
            val responsesForReviewModule = mutableMapOf<String, UserExerciseResponse>()
            moduleToReview.exercises.forEach { exercise ->
                val moduleExerciseId = generateModuleExerciseId(moduleToReview.id, exercise.id)
                userProgress.exerciseResponses[moduleExerciseId]?.let { savedResponse ->
                    responsesForReviewModule[exercise.id] = savedResponse
                }
            }
            _screenState.update {
                it.copy(
                    activeModule = moduleToReview,
                    currentExerciseResponses = responsesForReviewModule, // Cargar respuestas para el módulo en revisión
                    errorMessage = null
                )
            }
        } else {
            _screenState.update { it.copy(errorMessage = "No se pudo iniciar la revisión del módulo.") }
        }
    }
}