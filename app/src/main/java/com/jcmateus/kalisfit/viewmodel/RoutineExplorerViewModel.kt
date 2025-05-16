package com.jcmateus.kalisfit.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.obtenerRutinas
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.text.any
import kotlin.text.equals
import kotlin.text.lowercase


class RoutineExplorerViewModel : ViewModel() {

    private val TAG = "RoutineExplorerViewModel"

    private val _rutinasCompletas = MutableStateFlow<List<Rutina>>(emptyList())

    private val _selectedNivel = MutableStateFlow<String?>(null)
    val selectedNivel: StateFlow<String?> = _selectedNivel.asStateFlow()

    private val _selectedLugar = MutableStateFlow<LugarEntrenamiento?>(null)
    val selectedLugar: StateFlow<LugarEntrenamiento?> = _selectedLugar.asStateFlow()

    private val _selectedGrupoMuscular = MutableStateFlow<String?>(null)
    val selectedGrupoMuscular: StateFlow<String?> = _selectedGrupoMuscular.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val rutinasFiltradas: StateFlow<List<Rutina>> = combine(
        _rutinasCompletas,
        _selectedNivel,
        _selectedLugar,
        _selectedGrupoMuscular
    ) { rutinas, nivelFiltro, lugarEnumFiltro, grupoMuscularFiltroString ->
        if (rutinas.isEmpty()) {
            emptyList()
        } else {
            rutinas.filter { rutina ->
                // --- Para nivelMatch (asumiendo que rutina.nivelRecomendado es List<String>) ---
                val nivelMatch = if (nivelFiltro == null) {
                    true
                } else {
                    val filtroNivelMinusculas = nivelFiltro.lowercase()
                    rutina.nivelRecomendado.any { nivelRutina -> // nivelRutina es String
                        nivelRutina.lowercase().equals(filtroNivelMinusculas)
                    }
                }

                // --- Para lugarMatch (asumiendo que rutina.lugarEntrenamiento es List<String>) ---
                val lugarMatch = if (lugarEnumFiltro == null) {
                    true
                } else {
                    val nombreLugarFiltroMinusculas = lugarEnumFiltro.name.lowercase()
                    rutina.lugarEntrenamiento.any { lugarRutina -> // lugarRutina es String
                        lugarRutina.lowercase().equals(nombreLugarFiltroMinusculas)
                    }
                }

                // --- Para grupoMuscularMatch ---
                val grupoMuscularMatch = if (grupoMuscularFiltroString == null) {
                    true
                } else {
                    // rutina.ejercicios.flatMap { it.grupoMuscular } producirá List<GrupoMuscular>
                    val gruposDeLaRutinaEnEnum: List<GrupoMuscular> = rutina.ejercicios
                        .flatMap { it.grupoMuscular }
                        .distinct()

                    val filtroGrupoMinusculas = grupoMuscularFiltroString.lowercase()

                    // grupoDeRutinaEnum es de tipo GrupoMuscular
                    gruposDeLaRutinaEnEnum.any { grupoDeRutinaEnum ->
                        grupoDeRutinaEnum.name.lowercase().equals(filtroGrupoMinusculas) // <--- CORRECCIÓN AQUÍ
                    }
                }
                // ------------------------------------

                nivelMatch && lugarMatch && grupoMuscularMatch
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ... (resto del ViewModel igual que antes) ...

    init {
        loadAllRutinas()
    }

    private fun loadAllRutinas() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            Log.d(TAG, "Cargando TODAS las rutinas desde la fuente de datos.")

            obtenerRutinas(
                nivel = null,
                objetivos = null,
                lugaresEntrenamiento = null,
                onResult = { rutinasList ->
                    _rutinasCompletas.value = rutinasList
                    _isLoading.value = false
                    Log.d(TAG, "Todas las rutinas cargadas en _rutinasCompletas. Cantidad: ${rutinasList.size}")
                    if (rutinasList.isEmpty()) {
                        _errorMessage.value = "No se encontraron rutinas en la base de datos."
                    }
                },
                onError = { errorMsg ->
                    _errorMessage.value = errorMsg
                    _isLoading.value = false
                    Log.e(TAG, "Error al cargar todas las rutinas: $errorMsg")
                }
            )
        }
    }

    fun setNivelFilter(nivel: String?) {
        _selectedNivel.value = nivel
    }

    fun setLugarFilter(lugar: LugarEntrenamiento?) {
        _selectedLugar.value = lugar
    }

    fun setGrupoMuscularFilter(grupo: String?) {
        _selectedGrupoMuscular.value = grupo
    }

    fun clearFilters() {
        _selectedNivel.value = null
        _selectedLugar.value = null
        _selectedGrupoMuscular.value = null
    }

    fun refreshRutinas() {
        loadAllRutinas()
    }
}