package com.jcmateus.kalisfit.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jcmateus.kalisfit.data.ProgresoRutinaFirestore
import com.jcmateus.kalisfit.model.Rutina
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Asume que tienes estos modelos de datos (pueden ser los mismos que usas en otros sitios)
// data class Rutina(val id: String = "", val nombre: String = "", /* ...otros campos... */)
// data class ProgresoRutinaFirestore(/* ...tus campos... */) // La que guardas en Firestore

data class UserProgressUiState(
    val isLoadingRutinas: Boolean = false,
    val rutinasDisponibles: List<Rutina> = emptyList(),
    val errorCargaRutinas: String? = null,

    val isLoadingProgreso: Boolean = false,
    val historialProgreso: List<ProgresoRutinaFirestore> = emptyList(),
    val errorCargaProgreso: String? = null
    // Añade más campos según necesites (ej. estadísticas, rutina seleccionada para detalle, etc.)
)

class UserProgressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UserProgressUiState())
    val uiState: StateFlow<UserProgressUiState> = _uiState.asStateFlow()

    private val db = FirebaseFirestore.getInstance()

    // --- Gestión de Lista de Rutinas ---
    fun loadRutinasDisponibles(userId: String) { // O quizás las rutinas no dependen del usuario y son globales
        if (_uiState.value.isLoadingRutinas) return
        _uiState.update { it.copy(isLoadingRutinas = true, errorCargaRutinas = null) }

        viewModelScope.launch {
            try {
                // Ejemplo: Cargar todas las rutinas de una colección "rutinas"
                // Ajusta la query según cómo tengas estructuradas tus rutinas en Firestore
                val rutinasSnapshot = db.collection("rutinas")
                    // .whereEqualTo("creadorId", userId) // Si las rutinas son por usuario
                    .orderBy("nombre", Query.Direction.ASCENDING) // Ejemplo de ordenación
                    .get()
                    .await()

                val rutinas = rutinasSnapshot.documents.mapNotNull { document ->
                    // Aquí necesitas convertir el DocumentSnapshot a tu objeto Rutina
                    // Asumiendo que Rutina tiene un constructor vacío para la deserialización de Firestore
                    // y que los campos coinciden.
                    document.toObject(Rutina::class.java)?.copy(id = document.id)
                }
                _uiState.update { it.copy(isLoadingRutinas = false, rutinasDisponibles = rutinas) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingRutinas = false, errorCargaRutinas = e.message ?: "Error al cargar rutinas") }
            }
        }
    }

    // --- Gestión de Progreso del Usuario ---
    fun loadHistorialProgreso(userId: String) {
        if (userId.isBlank() || _uiState.value.isLoadingProgreso) return
        _uiState.update { it.copy(isLoadingProgreso = true, errorCargaProgreso = null) }

        viewModelScope.launch {
            try {
                val progresoSnapshot = db.collection("users")
                    .document(userId)
                    .collection("progresoRutinas")
                    .orderBy("fecha", Query.Direction.DESCENDING) // Mostrar el más reciente primero
                    .get()
                    .await()

                val historial = progresoSnapshot.documents.mapNotNull { document ->
                    document.toObject(ProgresoRutinaFirestore::class.java)
                }
                _uiState.update { it.copy(isLoadingProgreso = false, historialProgreso = historial) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingProgreso = false, errorCargaProgreso = e.message ?: "Error al cargar progreso") }
            }
        }
    }

    // --- Funciones para limpiar errores (similar a RoutineViewModel) ---
    fun clearRutinasError() {
        _uiState.update { it.copy(errorCargaRutinas = null) }
    }

    fun clearProgresoError() {
        _uiState.update { it.copy(errorCargaProgreso = null) }
    }

    // Aquí podrías añadir más funciones:
    // - Seleccionar una rutina para ver sus detalles (actualizaría el UiState con la rutina seleccionada)
    // - Calcular estadísticas a partir de historialProgreso
    // - Funciones para iniciar el proceso de creación/edición de rutinas
}