package com.jcmateus.kalisfit.viewmodel

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.getRutinaByIdFromFirestore
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoutineViewModel : ViewModel() {

    private val _rutina = MutableStateFlow<Rutina?>(null)
    val rutina: StateFlow<Rutina?> = _rutina

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val TAG = "RoutineViewModel"

    fun loadRutina(rutinaId: String) {
        _isLoading.value = true
        _errorMessage.value = null // Limpiar errores anteriores

        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando rutina completa con ID: $rutinaId")
                // LLAMA A LA FUNCIÓN QUE CARGA LA RUTINA Y SUS EJERCICIOS
                val loadedRutina = getRutinaByIdFromFirestore(rutinaId)

                _rutina.value = loadedRutina
                if (loadedRutina == null) {
                    _errorMessage.value = "Rutina no encontrada."
                    Log.w(TAG, "Rutina con ID $rutinaId no encontrada.")
                } else {
                    // Log adicional para confirmar que los ejercicios se cargaron
                    Log.d(TAG, "Rutina cargada: ${loadedRutina.nombre}, Número de ejercicios: ${loadedRutina.ejercicios.size}")
                }

            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error desconocido al cargar la rutina."
                Log.e(TAG, "Error al cargar rutina con ID $rutinaId", e)
                _rutina.value = null // Asegúrate de limpiar la rutina en caso de error
            } finally {
                _isLoading.value = false
            }
        }
    }
}