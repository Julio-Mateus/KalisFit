package com.jcmateus.kalisfit.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.jcmateus.kalisfit.data.ResumenSemanal
import com.jcmateus.kalisfit.data.calcularResumenSemanal
import com.jcmateus.kalisfit.data.obtenerHistorialProgreso
import com.jcmateus.kalisfit.model.ProgresoRutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Define un data class para representar el estado de la UI de HistorialScreen
data class HistoryUiState(
    val historial: List<ProgresoRutina> = emptyList(),
    val resumen: ResumenSemanal? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class HistoryViewModel : ViewModel() {

    // Expone el estado de la UI como un StateFlow inmutable
    private val _historyState = MutableStateFlow(HistoryUiState())
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    // Inicia la carga del historial al crear el ViewModel
    init {
        loadHistory()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadHistory() {
        // Empezar la carga, establecer isLoading a true y resetear el error
        _historyState.value = _historyState.value.copy(isLoading = true, errorMessage = null)

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            _historyState.value = _historyState.value.copy(
                isLoading = false,
                errorMessage = "Usuario no autenticado."
            )
            return
        }

        // Usar viewModelScope para lanzar una coroutine
        viewModelScope.launch {
            obtenerHistorialProgreso(
                userId = userId,
                onResult = { historialProgreso ->
                    val resumenSemanal = calcularResumenSemanal(historialProgreso)
                    _historyState.value = _historyState.value.copy(
                        historial = historialProgreso,
                        resumen = resumenSemanal,
                        isLoading = false,
                        errorMessage = null // Limpiar cualquier error previo
                    )
                },
                onError = { errorMsg ->
                    _historyState.value = _historyState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            )
        }
    }
}