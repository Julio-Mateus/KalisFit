package com.jcmateus.kalisfit.viewmodel

import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.model.StoicExerciseType
import com.jcmateus.kalisfit.model.StoicModule
import com.jcmateus.kalisfit.model.UserExerciseResponse
import com.jcmateus.kalisfit.model.UserStoicProgress
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
    // La clave es exercise.id (simple), el valor es el objeto UserExerciseResponse completo
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

    // --- Funciones Helper ---
    private fun generateModuleExerciseId(moduleId: String, exerciseId: String): String {
        return "${moduleId}_${exerciseId}"
    }

    // --- Lógica Principal ---
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
                val modules = modulesSnapshot.toObjects(StoicModule::class.java)

                val progressDoc = db.collection("user_stoic_progress").document(userId).get().await()
                var userProgress = progressDoc.toObject(UserStoicProgress::class.java)

                if (userProgress == null) {
                    userProgress = UserStoicProgress(
                        userId = userId,
                        currentModuleId = modules.firstOrNull()?.id
                        // exerciseResponses se inicializará vacío por defecto desde UserStoicProgress
                    )
                    // Opcional: Guardar este progreso inicial si no existe
                    // db.collection("user_stoic_progress").document(userId).set(userProgress).await()
                }

                val activeModuleId = userProgress.currentModuleId
                    ?: modules.firstOrNull { !userProgress.completedModuleIds.contains(it.id) }?.id
                val activeModule = modules.find { it.id == activeModuleId }

                // CAMBIO 2: Cargar UserExerciseResponse para el módulo activo
                val responsesForActiveModule = mutableMapOf<String, UserExerciseResponse>()
                if (activeModule != null) {
                    activeModule.exercises.forEach { exercise -> // Asume que StoicModule tiene List<StoicExerciseDefinition> exercises
                        val moduleExerciseId = generateModuleExerciseId(activeModule.id, exercise.id)
                        userProgress.exerciseResponses[moduleExerciseId]?.let { savedResponse ->
                            responsesForActiveModule[exercise.id] = savedResponse // Usar exercise.id como clave local
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
                        currentExerciseResponses = responsesForActiveModule, // Establecer respuestas cargadas
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

    // CAMBIO 3: Modificar updateExerciseResponse para manejar UserExerciseResponse y tipos
    fun updateExerciseResponse(
        exerciseId: String,          // ID simple del ejercicio (StoicExerciseDefinition.id)
        responseValue: String,       // El valor crudo de la UI (texto, índice como string, "true"/"false")
        exerciseType: StoicExerciseType, // El tipo de ejercicio desde StoicExerciseDefinition
        moduleId: String             // ID del módulo activo actual
    ) {
        _screenState.update { currentState ->
            val updatedLocalResponses = currentState.currentExerciseResponses.toMutableMap()

            val existingResponse = updatedLocalResponses[exerciseId] ?: UserExerciseResponse(
                exerciseId = exerciseId,
                moduleId = moduleId // Importante: Asegurar que el moduleId se establece aquí
            )

            val newResponseObject = when (exerciseType) {
                StoicExerciseType.REFLECTION_TEXT -> existingResponse.copy(
                    responseText = responseValue,
                    // Considerar si está completo basado en si hay texto
                    completed = responseValue.isNotBlank(),
                    timestamp = System.currentTimeMillis()
                )
                StoicExerciseType.MULTIPLE_CHOICE -> {
                    val choiceIndex = responseValue.toIntOrNull()
                    existingResponse.copy(
                        selectedChoiceIndex = choiceIndex,
                        // Considerar si está completo basado en si se seleccionó algo
                        completed = choiceIndex != null,
                        timestamp = System.currentTimeMillis()
                    )
                }
                StoicExerciseType.ACTION_PROMPT -> {
                    // Para ACTION_PROMPT, la "respuesta" podría ser simplemente marcarlo como completado.
                    // Si 'responseValue' es un booleano como string (ej. desde un Checkbox).
                    val isCompleted = responseValue.toBooleanStrictOrNull() ?: existingResponse.completed
                    existingResponse.copy(
                        completed = isCompleted, // O simplemente 'true' si la interacción significa completado
                        timestamp = System.currentTimeMillis()
                    )
                }
                // Añade otros casos si tienes más StoicExerciseType
            }
            updatedLocalResponses[exerciseId] = newResponseObject
            currentState.copy(currentExerciseResponses = updatedLocalResponses)
        }
    }

    fun completeModule(moduleId: String) {
        val userId = auth.currentUser?.uid ?: return
        val currentState = _screenState.value
        val currentProgress = currentState.userProgress ?: return // Es UserStoicProgress
        val modules = currentState.modules
        val completedModule = modules.find { it.id == moduleId } ?: return

        viewModelScope.launch {
            try {
                // CAMBIO 4: Fusionar currentExerciseResponses (UserExerciseResponse objects)
                // en UserStoicProgress.exerciseResponses antes de guardar
                val updatedGlobalExerciseResponses = currentProgress.exerciseResponses.toMutableMap()

                currentState.currentExerciseResponses.forEach { (exId, userExResponse) ->
                    // La clave para el mapa global es "moduleId_exerciseId"
                    val moduleExerciseId = generateModuleExerciseId(moduleId, exId) // moduleId es el módulo que se está completando

                    // Asegurarse de que el moduleId dentro de UserExerciseResponse es correcto
                    updatedGlobalExerciseResponses[moduleExerciseId] = userExResponse.copy(moduleId = moduleId)
                }

                val updatedCompletedModuleIds = currentProgress.completedModuleIds.toMutableList().apply { add(moduleId) }.distinct()
                val nextModule = modules.firstOrNull {
                    it.order > (completedModule.order) && !updatedCompletedModuleIds.contains(it.id)
                }

                val updatedUserProgress = currentProgress.copy(
                    completedModuleIds = updatedCompletedModuleIds,
                    currentModuleId = nextModule?.id,
                    exerciseResponses = updatedGlobalExerciseResponses // Guardar el mapa actualizado de UserExerciseResponse
                )

                db.collection("user_stoic_progress").document(userId).set(updatedUserProgress).await()

                // CAMBIO 5: Cargar respuestas para el siguiente módulo (si existe)
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
                        currentExerciseResponses = responsesForNextModule, // Respuestas para el nuevo módulo
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                _screenState.update { it.copy(errorMessage = "Error al guardar progreso: ${e.message}") }
            }
        }
    }

    // CAMBIO 6: Nueva función para repasar un módulo
    fun reviewModule(moduleId: String) {
        val currentState = _screenState.value
        val moduleToReview = currentState.modules.find { it.id == moduleId }
        val userProgress = currentState.userProgress // Es UserStoicProgress

        if (moduleToReview != null && userProgress != null) {
            val responsesForReviewModule = mutableMapOf<String, UserExerciseResponse>()
            moduleToReview.exercises.forEach { exercise ->
                val moduleExerciseId = generateModuleExerciseId(moduleToReview.id, exercise.id)
                userProgress.exerciseResponses[moduleExerciseId]?.let { savedResponse ->
                    // La clave para currentExerciseResponses es el exercise.id simple
                    responsesForReviewModule[exercise.id] = savedResponse
                }
            }

            _screenState.update {
                it.copy(
                    activeModule = moduleToReview,
                    currentExerciseResponses = responsesForReviewModule, // Establecer respuestas guardadas para el repaso
                    errorMessage = null
                )
            }
        } else {
            _screenState.update { it.copy(errorMessage = "No se pudo iniciar la revisión del módulo.") }
        }
    }
}