package com.jcmateus.kalisfit.viewmodel

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

class RoutineExplorerViewModel(
    private val userProfileViewModel: UserProfileViewModel // Recibimos el ViewModel del perfil
) : ViewModel() {

    private val _rutinas = MutableStateFlow<List<Rutina>>(emptyList())
    val rutinas: StateFlow<List<Rutina>> = _rutinas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Observamos el StateFlow 'user' del UserProfileViewModel
    val userProfile: StateFlow<UserProfile?> = userProfileViewModel.user

    init {
        // No necesitamos cargar el perfil aquí, ya lo hace UserProfileViewModel
        // Nos suscribiremos a sus cambios para cargar las rutinas
        viewModelScope.launch {
            userProfileViewModel.user
                .filterNotNull() // Solo reaccionar cuando el perfil no es nulo
                .collect { profile ->
                    // Cuando el perfil cambie (es decir, cuando se cargue), cargar las rutinas
                    // Asegúrate de que profile.lugarEntrenamiento sea compatible con LugarEntrenamiento enum
                    // Puede que necesites mapear String a LugarEntrenamiento si es necesario
                    val primerLugar = profile.lugarEntrenamiento.firstOrNull()
                    val lugarEnum = if (primerLugar != null) {
                        try {
                            LugarEntrenamiento.valueOf(primerLugar.uppercase()) // Convierte a enum
                        } catch (e: IllegalArgumentException) {
                            null // Manejar caso si el String no coincide con ningún enum
                        }
                    } else null

                    loadRutinas(
                        nivel = profile.nivel,
                        objetivos = profile.objetivos,
                        lugarEntrenamiento = lugarEnum
                    )
                }
        }
    }

    fun loadRutinas(nivel: String?, objetivos: List<String>?, lugarEntrenamiento: LugarEntrenamiento?) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            obtenerRutinas(
                nivel = nivel,
                objetivos = objetivos,
                lugarEntrenamiento = lugarEntrenamiento,
                onResult = { rutinasObtenidas ->
                    _rutinas.value = rutinasObtenidas
                    _isLoading.value = false
                },
                onError = { msg ->
                    _errorMessage.value = msg
                    _isLoading.value = false
                }
            )
        }
    }

    // No necesitamos loadUserProfile() en este ViewModel
}