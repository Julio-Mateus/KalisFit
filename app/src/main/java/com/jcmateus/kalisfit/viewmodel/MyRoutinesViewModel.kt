package com.jcmateus.kalisfit.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.data.getUserCustomRoutines
import com.jcmateus.kalisfit.model.UserCustomRoutine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Estado de la UI para la pantalla de Mis Rutinas
data class MyRoutinesUiState(
    val isLoading: Boolean = true,
    val routines: List<UserCustomRoutine> = emptyList(),
    val errorMessage: String? = null,
    val currentUserId: String? = null
)

class MyRoutinesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyRoutinesUiState())
    val uiState: StateFlow<MyRoutinesUiState> = _uiState.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        loadCurrentUserAndRoutines()
    }

    private fun loadCurrentUserAndRoutines() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            _uiState.value = _uiState.value.copy(currentUserId = firebaseUser.uid)
            fetchUserCustomRoutines(firebaseUser.uid)
        } else {
            _uiState.value = MyRoutinesUiState(isLoading = false, errorMessage = "Usuario no autenticado.")
            Log.w("MyRoutinesViewModel", "No hay usuario autenticado.")
        }
    }

    fun fetchUserCustomRoutines(userId: String? = _uiState.value.currentUserId) {
        if (userId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "ID de usuario no disponible.")
            Log.w("MyRoutinesViewModel", "Se intentó cargar rutinas sin ID de usuario.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                // Aquí llamas a tu función de FirestoreUtils
                val routinesList = getUserCustomRoutines(userId) // Asumiendo que getUserCustomRoutines está disponible globalmente o importada
                _uiState.value = _uiState.value.copy(isLoading = false, routines = routinesList)
            } catch (e: Exception) {
                Log.e("MyRoutinesViewModel", "Error al cargar rutinas personalizadas", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al cargar tus rutinas: ${e.localizedMessage}"
                )
            }
        }
    }

    // Opcional: Función para refrescar
    fun refreshRoutines() {
        _uiState.value.currentUserId?.let {
            fetchUserCustomRoutines(it)
        } ?: run {
            _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "No se puede refrescar: Usuario no disponible.")
            loadCurrentUserAndRoutines() // Intenta recargar el usuario también
        }
    }
}