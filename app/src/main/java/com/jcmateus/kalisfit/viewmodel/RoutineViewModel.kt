package com.jcmateus.kalisfit.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.getRutinaById
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
                Log.d(TAG, "Cargando rutina con ID: $rutinaId")
                // Llama a la función que obtiene la rutina desde tu fuente de datos
                val loadedRutina = getRutinaById(rutinaId) // Asumiendo que getRutinaById está disponible

                _rutina.value = loadedRutina
                if (loadedRutina == null) {
                    _errorMessage.value = "Rutina no encontrada."
                    Log.w(TAG, "Rutina con ID $rutinaId no encontrada.")
                }

            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Error desconocido al cargar la rutina."
                Log.e(TAG, "Error al cargar rutina con ID $rutinaId", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}