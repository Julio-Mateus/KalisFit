package com.jcmateus.kalisfit.viewmodel

import androidx.compose.foundation.layout.size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.obtenerRutinas
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import android.util.Log

class RoutineExplorerViewModel(private val userProfileViewModel: UserProfileViewModel) : ViewModel() {

    private val TAG = "RoutineExplorerViewModel" // TAG para logging


    // Estados que la UI observará
    private val _rutinas = MutableStateFlow<List<Rutina>>(emptyList())
    val rutinas: StateFlow<List<Rutina>> = _rutinas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Iniciar la carga de rutinas cuando el ViewModel se crea
        loadRutinas()
    }

    private fun loadRutinas() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null // Limpiar cualquier error previo

            val userProfile = userProfileViewModel.user.value
            val userLevel = userProfile?.nivel
            val userGoals = userProfile?.objetivos

            // --- COMIENZA LA CONVERSIÓN ---
            // Obtiene el primer lugar de entrenamiento del usuario como String, si existe
            val firstUserLocationString = userProfile?.lugarEntrenamiento?.firstOrNull()

            // Intenta convertir ese String a un valor del enum LugarEntrenamiento
            val userLocationEnum: LugarEntrenamiento? = try {
                // Usa uppercase() porque tu enum probablemente usa mayúsculas (CASA, GIMNASIO, etc.)
                firstUserLocationString?.let { LugarEntrenamiento.valueOf(it.uppercase()) }
            } catch (e: IllegalArgumentException) {
                // Si el String no coincide con ningún valor del enum, el resultado es null
                Log.w(TAG, "Lugar de entrenamiento del usuario '$firstUserLocationString' no coincide con enum LugarEntrenamiento.")
                null
            }
            // --- TERMINA LA CONVERSIÓN ---


            // Usamos la función obtenerRutinas que ya tienes
            obtenerRutinas(
                nivel = userLevel,
                objetivos = userGoals,
                lugarEntrenamiento = userLocationEnum, // ¡Pasa el valor convertido del tipo correcto!
                onResult = { rutinasList: List<Rutina> ->
                    // Esto se ejecuta en el hilo principal gracias a addOnSuccessListener/addOnFailureListener
                    _rutinas.value = rutinasList
                    _isLoading.value = false
                    Log.d(TAG, "Rutinas cargadas exitosamente. Cantidad: ${rutinasList.size}")
                },
                onError = { errorMsg: String ->
                    // Esto también se ejecuta en el hilo principal
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                    Log.e(TAG, "Error al cargar rutinas: $errorMsg")
                }
            )
        }
    }

    // Opcional: Una función para recargar las rutinas si es necesario (ej. pull-to-refresh)
    fun refreshRutinas() {
        loadRutinas()
    }
}