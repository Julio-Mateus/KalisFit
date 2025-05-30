package com.jcmateus.kalisfit.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.animation.core.copy
import androidx.compose.foundation.layout.size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.data.getAllCalisthenicsProgressions
import com.jcmateus.kalisfit.data.getCalisthenicsExerciseLevel
import com.jcmateus.kalisfit.model.ExerciseLevel
import com.jcmateus.kalisfit.model.Progression
import com.jcmateus.kalisfit.model.UserProgressionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.text.associateBy
import kotlin.text.isNotBlank
import kotlin.text.mapNotNull

// Constantes para nombres de colecciones (solo para UserProgressionState, ya que las otras están en FirestoreUtils)
object FirestoreCollections {
    // const val PROGRESSIONS = "calisthenicsProgressions" // No es necesario si getAllCalisthenicsProgressions la usa internamente
    const val USER_PROGRESSION_STATES = "userProgressStates"
}

class CalisthenicsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance() // Para UserProgressionState
    private val auth = FirebaseAuth.getInstance()

    private val _progressions = MutableStateFlow<List<Progression>>(emptyList())
    val progressions: StateFlow<List<Progression>> = _progressions.asStateFlow()

    private val _expandedProgressionId = MutableStateFlow<String?>(null)
    val expandedProgressionId: StateFlow<String?> = _expandedProgressionId.asStateFlow()

    private val _exerciseLevelDetails = MutableStateFlow<ExerciseLevel?>(null)
    val exerciseLevelDetails: StateFlow<ExerciseLevel?> = _exerciseLevelDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _userProgressionStates = MutableStateFlow<Map<String, UserProgressionState>>(emptyMap())
    val userProgressionStates: StateFlow<Map<String, UserProgressionState>> = _userProgressionStates.asStateFlow()

    init {
        fetchCalisthenicsProgressions() // Carga las plantillas usando tu función
        loadCurrentUserProgressionStates() // Carga el progreso del usuario
    }

    // --- Modificado para usar tu función getAllCalisthenicsProgressions ---
    fun fetchCalisthenicsProgressions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Llama directamente a tu suspend fun de FirestoreUtils
                // Esta función ya debería devolver los niveles ordenados.
                val progressionList = getAllCalisthenicsProgressions()
                _progressions.value = progressionList

                if (progressionList.isEmpty()) {
                    Log.d("CalisthenicsVM", "No calisthenics progressions found (via FirestoreUtils).")
                } else {
                    Log.d("CalisthenicsVM", "Progressions loaded (via FirestoreUtils): ${progressionList.size}")
                    // Opcional: verifica el orden si quieres estar extra seguro
                    // progressionList.firstOrNull()?.levels?.forEach { Log.d("CalisthenicsVM", "Level: ${it.name} (Order implied by list)")}
                }
            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error loading progressions (via FirestoreUtils): ${e.message}", e)
                _error.value = "Error loading progressions: ${e.localizedMessage}"
                _progressions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Modificado para usar tu función getCalisthenicsExerciseLevel ---
    fun loadExerciseLevelDetails(progressionId: String, levelId: String) {
        viewModelScope.launch {
            _isLoading.value = true // O un isLoadingDetails
            _error.value = null
            _exerciseLevelDetails.value = null // Limpiar detalles anteriores
            try {
                // Llama directamente a tu suspend fun de FirestoreUtils
                val fetchedDetails = getCalisthenicsExerciseLevel(progressionId, levelId)

                if (fetchedDetails != null) {
                    _exerciseLevelDetails.value = fetchedDetails
                } else {
                    Log.w("CalisthenicsVM", "No details found (via FirestoreUtils) for level $progressionId - $levelId.")
                    _error.value = "Details not found for this level."
                }
            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error loading level details (via FirestoreUtils): ${e.message}", e)
                _error.value = "Error loading level details: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- NUEVO: Cargar todos los estados de progreso para el usuario actual ---
    fun loadCurrentUserProgressionStates() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("CalisthenicsVM", "Cannot load user progression states: User not logged in")
            _userProgressionStates.value = emptyMap()
            return
        }

        viewModelScope.launch {
            // _isLoadingUserProgress.value = true // Si tienes un loader específico
            try {
                val snapshot = db.collection(FirestoreCollections.USER_PROGRESSION_STATES)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val statesMap = snapshot.documents.mapNotNull { doc ->
                    // Importante: Asegúrate de que UserProgressionState tenga un constructor sin argumentos
                    // o los campos tengan valores por defecto para que toObject funcione bien.
                    // Si usas un ID compuesto para el documento, el ID del documento NO se mapea
                    // automáticamente a un campo 'id' en el objeto a menos que lo hagas manualmente.
                    // Aquí, el ID de la progresión está DENTRO del objeto UserProgressionState.
                    doc.toObject(UserProgressionState::class.java)
                }.associateBy { it.progressionId } // El progressionId debe estar en el objeto

                _userProgressionStates.value = statesMap
                Log.d("CalisthenicsVM", "User progression states loaded: ${statesMap.size} for user $userId")

            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error loading user progression states for user $userId", e)
                // _error.value = "Error al cargar el progreso del usuario: ${e.message}"
            } finally {
                // _isLoadingUserProgress.value = false
            }
        }
    }


    // --- NUEVO: Obtener o crear UserProgressionState en memoria para una progresión ---
    // (Helper interno, o podría ser público si la UI lo necesita directamente)
    // Asegúrate que tu modelo Progression y ExerciseLevel tengan el campo 'id'
    private fun getInMemoryOrDefaultUserProgressionState(
        progressionId: String
    ): UserProgressionState {
        val userId = auth.currentUser?.uid
        val existingState = _userProgressionStates.value[progressionId]

        if (existingState != null) {
            return existingState
        }

        if (userId != null) {
            // Obtener la progresión de la lista ya cargada para encontrar el primer nivel
            val targetProgression = _progressions.value.firstOrNull { it.id == progressionId }
            // Los niveles ya deberían estar ordenados por getAllCalisthenicsProgressions
            val firstLevelId = targetProgression?.levels?.firstOrNull()?.id

            return UserProgressionState(
                // id = "${userId}_${progressionId}", // El ID del documento se genera al guardar
                userId = userId,
                progressionId = progressionId,
                currentAttemptLevelId = firstLevelId, // Puede ser null si la progresión no tiene niveles
                lastCompletedLevelId = null,
                lastUpdated = 0L // O System.currentTimeMillis() si lo prefieres para la creación
            )
        }
        // Si no hay usuario o no se encontró la progresión, devuelve uno "vacío" o "inválido".
        // La UI debería manejar esto (ej. no mostrando opciones de progreso).
        return UserProgressionState(progressionId = progressionId) // userId será blank
    }


    // --- NUEVO: Marcar un nivel como completado ---
    fun markLevelAsCompleted(progressionId: String, completedLevelId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _error.value = "Usuario no autenticado para guardar progreso."
            Log.w("CalisthenicsVM", "Cannot mark level completed: User not logged in")
            return
        }

        val currentProgression = _progressions.value.firstOrNull { it.id == progressionId }
        if (currentProgression == null || currentProgression.levels.isEmpty()) {
            _error.value = "Progresión o sus niveles no encontrados."
            Log.e("CalisthenicsVM", "Progression $progressionId not found or has no levels.")
            return
        }
        // Los niveles ya están ordenados por getAllCalisthenicsProgressions

        var userStateToUpdate = getInMemoryOrDefaultUserProgressionState(progressionId)

        // Si el estado es el por defecto (sin userId), es porque el usuario no estaba logueado
        // o es la primera interacción. Forzamos el userId ahora.
        if (userStateToUpdate.userId.isBlank() && userId.isNotBlank()) {
            userStateToUpdate = userStateToUpdate.copy(userId = userId)
            // Si currentAttemptLevelId es null, intentar poner el primer nivel
            if (userStateToUpdate.currentAttemptLevelId == null && currentProgression.levels.isNotEmpty()){
                userStateToUpdate = userStateToUpdate.copy(currentAttemptLevelId = currentProgression.levels.first().id)
            }
        }


        val completedLevelIndex = currentProgression.levels.indexOfFirst { it.id == completedLevelId }
        if (completedLevelIndex == -1) {
            _error.value = "Nivel completado no encontrado en la progresión."
            Log.e("CalisthenicsVM", "Level $completedLevelId not found in progression ${currentProgression.name}")
            return
        }

        val nextLevelId = if (completedLevelIndex < currentProgression.levels.size - 1) {
            currentProgression.levels[completedLevelIndex + 1].id
        } else {
            null // Último nivel completado, no hay siguiente intento
        }

        userStateToUpdate = userStateToUpdate.copy(
            lastCompletedLevelId = completedLevelId,
            currentAttemptLevelId = nextLevelId, // Será null si se completó el último nivel
            lastUpdated = System.currentTimeMillis()
        )

        // Guardar el estado actualizado en Firestore
        viewModelScope.launch {
            _isLoading.value = true // O un isLoadingSavingProgress
            _error.value = null
            try {
                // El ID del documento es crucial para poder sobrescribir/actualizar
                val docId = "${userId}_${progressionId}"

                db.collection(FirestoreCollections.USER_PROGRESSION_STATES)
                    .document(docId) // Usar el ID compuesto para asegurar que se actualice el mismo doc
                    .set(userStateToUpdate) // .set() crea o sobrescribe
                    .await()

                // Actualizar el StateFlow local para reflejar el cambio inmediatamente
                _userProgressionStates.value = _userProgressionStates.value + (progressionId to userStateToUpdate)
                Log.d("CalisthenicsVM", "UserProgressionState for $progressionId, user $userId updated. Next level: $nextLevelId")

            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error updating UserProgressionState for $progressionId", e)
                _error.value = "Error al guardar el progreso: ${e.message}"
                // Considera revertir el cambio en _userProgressionStates.value si Firestore falla
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun onProgressionHeaderClick(progressionId: String) {
        _expandedProgressionId.value = if (_expandedProgressionId.value == progressionId) null else progressionId
    }

    fun clearError() {
        _error.value = null
    }

    fun clearExerciseLevelDetails() {
        _exerciseLevelDetails.value = null
    }
}