package com.jcmateus.kalisfit.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.getRutinaByIdFromFirestore
import com.jcmateus.kalisfit.data.guardarProgresoRutina
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoutineViewModel : ViewModel() {

    private val _rutina = MutableStateFlow<Rutina?>(null)
    val rutina: StateFlow<Rutina?> = _rutina

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Podrías tener un estado específico para el guardado si necesitas feedback más granular en la UI
    private val _isSavingProgress = MutableStateFlow(false)
    val isSavingProgress: StateFlow<Boolean> = _isSavingProgress

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Para mensajes de éxito que no sean errores, pero que quieras mostrar (ej. en un Snackbar desde el ViewModel)
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage


    private val TAG = "RoutineViewModel"

    fun loadRutina(rutinaId: String) {
        _isLoading.value = true
        _errorMessage.value = null // Limpiar errores anteriores
        _successMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando rutina completa con ID: $rutinaId")
                // Asumiendo que getRutinaByIdFromFirestore es la función correcta para cargar una sola rutina
                // Si esta función también está en FirestoreUtils, podrías llamarla como FirestoreUtils.getRutinaById(rutinaId)
                val loadedRutina = getRutinaByIdFromFirestore(rutinaId) // O FirestoreUtils.getRutinaById(rutinaId)

                _rutina.value = loadedRutina
                if (loadedRutina == null) {
                    _errorMessage.value = "Rutina con ID $rutinaId no encontrada."
                    Log.w(TAG, "Rutina con ID $rutinaId no encontrada.")
                } else {
                    Log.d(TAG, "Rutina cargada: ${loadedRutina.nombre}, Número de ejercicios: ${loadedRutina.ejercicios.size}")
                }

            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error desconocido al cargar la rutina."
                Log.e(TAG, "Error al cargar rutina con ID $rutinaId", e)
                _rutina.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O) // Coincide con la anotación en FirestoreUtils
    fun saveRoutineProgress(
        userId: String,
        userProfile: UserProfile,
        completedRoutine: Rutina,
        // Nuevos parámetros que necesita guardarProgresoRutina
        rondasCompletadas: Int,
        tiempoTotalSegundos: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isSavingProgress.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "Intentando guardar progreso para userID: $userId, rutina: ${completedRoutine.nombre}")

                // Llamada directa a la función importada
                guardarProgresoRutina(
                    userIdAuth = userId, // El parámetro en FirestoreUtils se llama userIdAuth
                    rutinaRealizada = completedRoutine, // El parámetro se llama rutinaRealizada
                    perfilUsuarioActual = userProfile, // El parámetro se llama perfilUsuarioActual
                    rondasCompletadasEnSesion = rondasCompletadas, // Nuevo
                    tiempoTotalDeLaSesionSegundos = tiempoTotalSegundos, // Nuevo
                    onSuccess = {
                        Log.i(TAG, "Progreso de rutina guardado exitosamente para userID: $userId")
                        _successMessage.value = "¡Progreso guardado!"
                        _isSavingProgress.value = false
                        onSuccess()
                    },
                    onError = { errorMsg ->
                        Log.e(TAG, "Error al guardar progreso de rutina para userID: $userId. Error: $errorMsg")
                        _errorMessage.value = errorMsg
                        _isSavingProgress.value = false
                        onError(errorMsg)
                    }
                )
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Error desconocido al intentar guardar el progreso."
                Log.e(TAG, "Excepción al guardar progreso de rutina para userID: $userId", e)
                _errorMessage.value = errorMsg
                _isSavingProgress.value = false
                onError(errorMsg)
            }
        }
    }

    /**
     *  Limpia un mensaje de éxito después de que ha sido mostrado/consumido por la UI.
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Limpia un mensaje de error después de que ha sido mostrado/consumido por la UI.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}