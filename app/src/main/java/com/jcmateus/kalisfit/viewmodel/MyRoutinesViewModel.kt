package com.jcmateus.kalisfit.viewmodel

import android.util.Log
import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jcmateus.kalisfit.data.getUserCustomRoutines
import com.jcmateus.kalisfit.model.UserCustomRoutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Estado de la UI para la pantalla de Mis Rutinas
data class MyRoutinesUiState(
    val isLoading: Boolean = false, // Buen momento para revisar el valor inicial si la carga empieza en init
    val routines: List<UserCustomRoutine> = emptyList(),
    val currentUserId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null // <--- AÑADIR ESTO
)

class MyRoutinesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyRoutinesUiState())
    val uiState: StateFlow<MyRoutinesUiState> = _uiState.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        // Si quieres que isLoading sea true desde el inicio mientras se carga el usuario:
        // _uiState.update { it.copy(isLoading = true) } // <--- Actualización
        loadCurrentUserAndRoutines()
    }

    private fun loadCurrentUserAndRoutines() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            _uiState.update { // Usar update para ser más conciso
                it.copy(
                    currentUserId = firebaseUser.uid,
                    isLoading = true // Mantener isLoading o ponerlo true aquí si no lo hiciste en init
                )
            }
            fetchUserCustomRoutines(firebaseUser.uid)
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Usuario no autenticado."
                )
            }
            Log.w("MyRoutinesViewModel", "No hay usuario autenticado.")
        }
    }

    fun fetchUserCustomRoutines(userId: String? = _uiState.value.currentUserId) {
        if (userId == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "ID de usuario no disponible."
                )
            }
            Log.w("MyRoutinesViewModel", "Se intentó cargar rutinas sin ID de usuario.")
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) } // Limpiar mensajes
        viewModelScope.launch {
            try {
                val routinesList = getUserCustomRoutines(userId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        routines = routinesList
                    )
                }
            } catch (e: Exception) {
                Log.e("MyRoutinesViewModel", "Error al cargar rutinas personalizadas", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar tus rutinas: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun refreshRoutines() {
        _uiState.value.currentUserId?.let { userId ->
            // _uiState.update { it.copy(isLoading = true) } // Opcional: indicar carga
            fetchUserCustomRoutines(userId)
        } ?: run {
            _uiState.update {
                it.copy(
                    isLoading = false, // Asegurarse de que no quede en true
                    errorMessage = "No se puede refrescar: Usuario no disponible."
                )
            }
            // loadCurrentUserAndRoutines() // Considera si realmente quieres recargar el usuario aquí
            // o solo mostrar el error. Si el usuario se deslogueó,
            // loadCurrentUserAndRoutines ya manejará ese estado.
        }
    }

    fun deleteRoutine(routineId: String) {
        val currentUserId = _uiState.value.currentUserId
        if (currentUserId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Error: Usuario no identificado para eliminar la rutina.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .collection("customRoutines")
                    .document(routineId)
                    .delete()
                    .await()

                val updatedRoutines = _uiState.value.routines.filterNot { it.id == routineId }
                _uiState.update {
                    it.copy(
                        routines = updatedRoutines,
                        isLoading = false,
                        successMessage = "Rutina eliminada correctamente" // <--- USAR successMessage
                    )
                }

            } catch (e: Exception) {
                Log.e("MyRoutinesViewModel", "Error al eliminar rutina ID: $routineId", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al eliminar la rutina: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() { // <--- AÑADIR ESTA FUNCIÓN
        _uiState.update { it.copy(successMessage = null) }
    }
}