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
import com.jcmateus.kalisfit.model.isLevelCompleted
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
    const val USER_PROGRESSION_STATES = "userProgressStates"
}

class CalisthenicsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var authListener: FirebaseAuth.AuthStateListener? = null


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

    private val _nextLevelToNavigate = MutableStateFlow<Pair<String, String>?>(null)
    val nextLevelToNavigate: StateFlow<Pair<String, String>?> = _nextLevelToNavigate.asStateFlow()

    private val _userProgressionStates = MutableStateFlow<Map<String, UserProgressionState>>(emptyMap())
    val userProgressionStates: StateFlow<Map<String, UserProgressionState>> = _userProgressionStates.asStateFlow()

    init {
        fetchCalisthenicsProgressions()
        observeAuthChangesAndLoadProgress()
    }

    private fun observeAuthChangesAndLoadProgress() {
        // Remover listener anterior si existe para evitar duplicados al recrear ViewModel (ej. cambio de config)
        authListener?.let { auth.removeAuthStateListener(it) }

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("CalisthenicsVM", "Auth state changed: User logged in (${user.uid}). Loading progression states.")
                loadCurrentUserProgressionStates()
            } else {
                Log.d("CalisthenicsVM", "Auth state changed: User logged out. Clearing progression states.")
                _userProgressionStates.value = emptyMap()
                _expandedProgressionId.value = null // También limpiar la expansión
            }
        }
        auth.addAuthStateListener(authListener!!)

        // Carga inicial si el usuario ya está logueado al iniciar el ViewModel
        if (auth.currentUser != null) {
            loadCurrentUserProgressionStates()
        }
    }

    fun fetchCalisthenicsProgressions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Asegúrate que esta función obtiene las progresiones y sus niveles
                // Idealmente, los niveles dentro de cada progresión ya vienen ordenados por 'order'
                val progressionList = getAllCalisthenicsProgressions()
                _progressions.value = progressionList

                if (progressionList.isEmpty()) {
                    Log.d("CalisthenicsVM", "No calisthenics progressions found.")
                } else {
                    Log.d("CalisthenicsVM", "Progressions loaded: ${progressionList.size}")
                }
            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error loading progressions: ${e.message}", e)
                _error.value = "Error loading progressions: ${e.localizedMessage}"
                _progressions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadExerciseLevelDetails(progressionId: String, levelId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _exerciseLevelDetails.value = null // Limpiar detalles anteriores
            try {
                val fetchedDetails = getCalisthenicsExerciseLevel(progressionId, levelId)
                if (fetchedDetails != null) {
                    _exerciseLevelDetails.value = fetchedDetails
                } else {
                    Log.w("CalisthenicsVM", "No details found for level $progressionId - $levelId.")
                    _error.value = "Details not found for this level."
                }
            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error loading level details: ${e.message}", e)
                _error.value = "Error loading level details: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCurrentUserProgressionStates() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("CalisthenicsVM", "Cannot load user progression states: User not logged in.")
            _userProgressionStates.value = emptyMap()
            return
        }
        Log.d("CalisthenicsVM", "Loading user progression states for user $userId")
        viewModelScope.launch {
            // No establezcas isLoading aquí si quieres que la carga de progreso sea más en segundo plano
            // _isLoading.value = true
            try {
                val snapshot = db.collection(FirestoreCollections.USER_PROGRESSION_STATES)
                    .whereEqualTo("userId", userId) // Campo dentro del documento UserProgressionState
                    .get()
                    .await()

                val statesMap = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(UserProgressionState::class.java)?.apply {
                        // Si UserProgressionState no tiene un campo 'id' que coincida con el ID del documento,
                        // y necesitas el ID del documento, tendrías que asignarlo aquí.
                        // Pero para `associateBy { it.progressionId }`, no es estrictamente necesario
                        // si `progressionId` es la clave que quieres usar en el mapa local.
                    }
                }.associateBy { it.progressionId } // Clave del mapa local es progressionId

                _userProgressionStates.value = statesMap
                Log.d("CalisthenicsVM", "User progression states loaded: ${statesMap.size} for user $userId.")
            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error loading user progression states for $userId", e)
                _error.value = "Error loading your progress: ${e.localizedMessage}"
                _userProgressionStates.value = emptyMap()
            } finally {
                // _isLoading.value = false
            }
        }
    }

    private fun getInMemoryOrDefaultUserProgressionState(progressionId: String): UserProgressionState {
        val userId = auth.currentUser?.uid // Asegurarse de tener el userId actual
        val existingState = _userProgressionStates.value[progressionId]

        if (existingState != null) {
            // Si el estado existe pero es de un usuario anterior (poco probable con la lógica de auth, pero por seguridad)
            // o si el userId del estado en memoria es vacío (estado por defecto no guardado), actualízalo.
            if (userId != null && existingState.userId != userId && existingState.userId.isBlank()) {
                Log.d("CalisthenicsVM_State", "Found existing in-memory state for P:$progressionId but with blank userId. Updating with current userId: $userId")
                return existingState.copy(userId = userId)
            }
            Log.d("CalisthenicsVM_State", "Found existing in-memory state for P:$progressionId: $existingState")
            return existingState
        }

        Log.d("CalisthenicsVM_State", "No in-memory state for P:$progressionId. Creating default.")
        if (userId != null) {
            val targetProgression = _progressions.value.firstOrNull { it.id == progressionId }
            // Asumimos que los niveles ya están ordenados por 'order' desde la carga inicial
            val firstLevelId = targetProgression?.levels?.firstOrNull()?.id

            return UserProgressionState(
                userId = userId,
                progressionId = progressionId,
                currentAttemptLevelId = firstLevelId,
                lastCompletedLevelId = null,
                completedLevelIds = emptyList(),
                lastUpdated = System.currentTimeMillis()
                // Asegúrate que UserProgressionState NO tiene un campo 'id' propio,
                // o si lo tiene, que no interfiera con el ID del documento de Firestore.
            )
        }
        Log.w("CalisthenicsVM_State", "Cannot create valid default state for P:$progressionId - user not logged in or progression template missing.")
        return UserProgressionState( // Estado inválido/placeholder
            progressionId = progressionId,
            userId = "", // userId vacío para indicar que es inválido/no utilizable
            currentAttemptLevelId = null,
            lastCompletedLevelId = null,
            completedLevelIds = emptyList(),
            lastUpdated = 0L
        )
    }

    fun markLevelAsCompleted(progressionId: String, completedLevelId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _error.value = "User not authenticated to save progress."
            Log.w("CalisthenicsVM", "Cannot mark level completed: User not logged in")
            return
        }

        val currentProgression = _progressions.value.firstOrNull { it.id == progressionId }
        if (currentProgression == null || currentProgression.levels.isEmpty()) {
            _error.value = "Progression or its levels not found."
            Log.e("CalisthenicsVM", "MarkComplete: P-$progressionId not found or has no levels.")
            return
        }
        // Asume que los niveles en currentProgression.levels ya están ordenados por 'order'
        // Si no, usa: val sortedLevels = currentProgression.levels.sortedBy { it.order }
        val sortedLevels = currentProgression.levels // Asumiendo que ya están ordenados

        val completedLevelInTemplate = sortedLevels.find { it.id == completedLevelId }
        if (completedLevelInTemplate == null) {
            _error.value = "Completed level not found in progression template."
            Log.e("CalisthenicsVM", "MarkComplete: L-$completedLevelId not in P-${currentProgression.name}")
            return
        }

        Log.d("CalisthenicsVM", "Marking level P:$progressionId, L:$completedLevelId as completed for user $userId.")

        var userStateToUpdate = getInMemoryOrDefaultUserProgressionState(progressionId)

        // Asegurar que el userId es el correcto, especialmente si era un estado por defecto.
        if (userStateToUpdate.userId.isBlank() || userStateToUpdate.userId != userId) {
            userStateToUpdate = userStateToUpdate.copy(userId = userId)
        }

        val updatedCompletedIds = (userStateToUpdate.completedLevelIds + completedLevelId).distinct()

        val completedLevelIndex = sortedLevels.indexOfFirst { it.id == completedLevelId }
        val nextLevelId: String? = if (completedLevelIndex != -1 && completedLevelIndex < sortedLevels.size - 1) {
            sortedLevels[completedLevelIndex + 1].id
        } else {
            null // Último nivel completado o nivel no encontrado (aunque ya se verificó)
        }

        userStateToUpdate = userStateToUpdate.copy(
            lastCompletedLevelId = completedLevelId,
            currentAttemptLevelId = nextLevelId, // Puede ser null si es el último nivel
            completedLevelIds = updatedCompletedIds,
            lastUpdated = System.currentTimeMillis()
        )

        Log.d("CalisthenicsVM", "MarkComplete: UserState to save: $userStateToUpdate")

        viewModelScope.launch {
            _isLoading.value = true // Indicar carga para la operación de guardado
            _error.value = null
            try {
                // *** ID del Documento PREDECIBLE para Firestore ***
                val firestoreDocId = "${userId}_${progressionId}"

                db.collection(FirestoreCollections.USER_PROGRESSION_STATES)
                    .document(firestoreDocId)
                    .set(userStateToUpdate) // Crea si no existe, sobrescribe si existe.
                    // Usa .set(userStateToUpdate, SetOptions.merge()) si solo quieres actualizar campos específicos
                    // y no sobrescribir todo el documento si otros campos pudieran existir y no están en userStateToUpdate.
                    // Para este caso, `set()` es usualmente lo que quieres.
                    .await()

                Log.d("CalisthenicsVM", "UserProgressionState for P:$progressionId (Doc: $firestoreDocId) SAVED/UPDATED. State: $userStateToUpdate")

                // Actualizar el StateFlow local
                _userProgressionStates.value = _userProgressionStates.value + (progressionId to userStateToUpdate)
                Log.d("CalisthenicsVM", "Local _userProgressionStates updated for P:$progressionId. New map size: ${_userProgressionStates.value.size}")

                // Si esta acción también desbloquea el siguiente nivel, podemos prepararlo para la navegación
                if (nextLevelId != null && isLevelUnlocked(progressionId, nextLevelId)) {
                    // No es necesario llamar a _nextLevelToNavigate aquí directamente
                    // La UI puede reaccionar a los cambios en userProgressionStates y determinar si se navega
                }


            } catch (e: Exception) {
                Log.e("CalisthenicsVM", "Error saving/updating UserProgressionState for P:$progressionId (Doc: ${userId}_${progressionId})", e)
                _error.value = "Error saving progress: ${e.localizedMessage}"
                // No reviertas el estado local aquí, la UI ya lo tiene.
                // Si la escritura falla, el estado local y el backend estarán desincronizados temporalmente.
                // El usuario podría reintentar o una futura carga de datos lo corregirá.
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun markLevelAsCompletedAndPrepareNext(progressionId: String, levelId: String) {
        // Primero, marca el nivel como completado (esto actualiza el backend y el StateFlow _userProgressionStates)
        markLevelAsCompleted(progressionId, levelId)

        // El resto de la lógica de esta función se basa en el estado actualizado por markLevelAsCompleted.
        // viewModelScope.launch no es estrictamente necesario aquí si markLevelAsCompleted ya usa su propio scope
        // y _nextLevelToNavigate es solo para la UI.
        // Pero si quieres que el cálculo de nextLevelToNavigate también sea asíncrono y no bloquee el hilo llamador:
        viewModelScope.launch {
            // Esperar un breve momento para asegurar que _userProgressionStates se actualizó por markLevelAsCompleted
            // Esto es un pequeño "hack". Una mejor forma sería que markLevelAsCompleted devuelva el estado actualizado
            // o una señal de que terminó, pero para simplificar por ahora:
            // kotlinx.coroutines.delay(50) // Considera alternativas más robustas a delay

            val currentProgression = _progressions.value.find { it.id == progressionId }
            currentProgression?.let { prog ->
                // Asume que prog.levels ya está ordenado
                val completedLevelIndex = prog.levels.indexOfFirst { it.id == levelId }

                if (completedLevelIndex != -1 && completedLevelIndex < prog.levels.size - 1) {
                    val nextLevel = prog.levels[completedLevelIndex + 1]
                    // Comprueba si el siguiente nivel ESTÁ AHORA DESBLOQUEADO (después de que userProgressionStates se actualizó)
                    if (isLevelUnlocked(progressionId, nextLevel.id)) {
                        _nextLevelToNavigate.value = Pair(progressionId, nextLevel.id)
                        Log.d("CalisthenicsVM", "Next level to navigate set: P:${progressionId}, L:${nextLevel.id}")
                    } else {
                        _nextLevelToNavigate.value = null
                        Log.d("CalisthenicsVM", "Next level P:${progressionId}, L:${nextLevel.id} is not unlocked. No navigation.")
                    }
                } else {
                    _nextLevelToNavigate.value = null // Es el último nivel o algo salió mal
                    Log.d("CalisthenicsVM", "No next level to navigate (last level or index issue) for P:${progressionId}, L:${levelId}")
                }
            }
        }
    }


    fun consumedNextLevelNavigation() {
        _nextLevelToNavigate.value = null
    }

    fun isLevelCompleted(progressionId: String, levelId: String): Boolean {
        val userState = _userProgressionStates.value[progressionId]
        val isCompleted = userState?.completedLevelIds?.contains(levelId) ?: false
        // Log.d("CalisthenicsVM_StateCheck", "isLevelCompleted P:$progressionId, L:$levelId? -> $isCompleted") // Log menos verboso
        return isCompleted
    }

    fun isLevelUnlocked(progressionId: String, levelId: String): Boolean {
        val targetProgression = _progressions.value.firstOrNull { it.id == progressionId }
        if (targetProgression == null || targetProgression.levels.isEmpty()) {
            Log.w("CalisthenicsVM_Unlock", "P-$progressionId not found or no levels. L-$levelId unlock check failed.")
            return false
        }

        // Asume que targetProgression.levels ya está ordenado por 'order'
        val sortedLevels = targetProgression.levels
        val levelToCheck = sortedLevels.find { it.id == levelId }

        if (levelToCheck == null) {
            Log.w("CalisthenicsVM_Unlock", "L-$levelId not in P-$progressionId template. Unlock check failed.")
            return false
        }

        // 1. Primer nivel siempre está desbloqueado
        if (levelToCheck.id == sortedLevels.firstOrNull()?.id) {
            // Log.d("CalisthenicsVM_Unlock", "L:$levelId is first level -> UNLOCKED")
            return true
        }

        // 2. Nivel ya completado está desbloqueado
        if (isLevelCompleted(progressionId, levelId)) {
            // Log.d("CalisthenicsVM_Unlock", "L:$levelId is completed -> UNLOCKED")
            return true
        }

        // 3. Nivel anterior (en orden) completado desbloquea el actual
        val currentLevelIndex = sortedLevels.indexOfFirst { it.id == levelId }
        if (currentLevelIndex > 0) {
            val previousLevel = sortedLevels[currentLevelIndex - 1]
            if (isLevelCompleted(progressionId, previousLevel.id)) {
                // Log.d("CalisthenicsVM_Unlock", "Previous L:${previousLevel.id} completed -> L:$levelId UNLOCKED")
                return true
            }
        }
        // Log.d("CalisthenicsVM_Unlock", "L:$levelId -> LOCKED (conditions not met)")
        return false
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

    override fun onCleared() {
        super.onCleared()
        authListener?.let { auth.removeAuthStateListener(it) } // Muy importante para evitar memory leaks
        Log.d("CalisthenicsVM", "ViewModel cleared, AuthStateListener removed.")
    }
}