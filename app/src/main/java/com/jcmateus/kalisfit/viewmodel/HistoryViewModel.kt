package com.jcmateus.kalisfit.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jcmateus.kalisfit.data.ProgresoRutinaFirestore
import com.jcmateus.kalisfit.data.ResumenSemanal
import com.jcmateus.kalisfit.model.ProgresoRutina
import com.jcmateus.kalisfit.model.UserActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Define un data class para representar el estado de la UI de HistorialScreen
data class HistoryUiState(
    // Historial de Rutinas
    val historialRutinas: List<ProgresoRutina> = emptyList(),
    val resumenRutinas: ResumenSemanal? = null,
    val isLoadingRutinas: Boolean = true, // Estado de carga específico para rutinas

    // Historial de Actividades Libres (carreras/caminatas)
    val historialActividadesLibres: List<UserActivity> = emptyList(),
    val isLoadingActividadesLibres: Boolean = true, // Estado de carga específico para actividades libres

    // Mensaje de error general
    val errorMessage: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
class HistoryViewModel : ViewModel() {

    private val _historyState = MutableStateFlow(HistoryUiState())
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d("HistoryViewModel", "Usuario autenticado: $userId. Cargando historiales.")
            loadRoutineHistoryInternal(userId) // Carga historial de rutinas
            listenForFreeActivitiesHistoryInternal(userId) // Escucha historial de actividades libres
        } else {
            Log.w("HistoryViewModel", "Usuario no autenticado en init. No se cargarán historiales.")
            _historyState.value = HistoryUiState(
                isLoadingRutinas = false,
                isLoadingActividadesLibres = false,
                errorMessage = "Usuario no autenticado."
            )
        }
    }

    // --- Funciones para el Historial de Rutinas ---

    // Función INTERNA para cargar el historial de rutinas
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadRoutineHistoryInternal(userId: String) {
        _historyState.value = _historyState.value.copy(isLoadingRutinas = true, errorMessage = null)
        viewModelScope.launch {
            com.jcmateus.kalisfit.data.obtenerHistorialProgreso( // Esta devuelve List<model.ProgresoRutina>
                userId = userId,
                onResult = { historialProgresoModel -> // Esto es List<com.jcmateus.kalisfit.model.ProgresoRutina>
                    // Mapear de List<model.ProgresoRutina> a List<data.ProgresoRutinaFirestore>
                    val historialProgresoFirestore = historialProgresoModel.map { progresoModel ->
                        // Convertir model.EjercicioProgreso a data.EjercicioProgresoFirestore
                        val ejerciciosFirestore = progresoModel.ejerciciosCompletados.map { ejercicioModel ->
                            com.jcmateus.kalisfit.data.EjercicioProgresoFirestore(
                                ejercicioIdOriginal = ejercicioModel.ejercicioIdOriginal,
                                nombre = ejercicioModel.nombre,
                                duracionPorSerieSegundos = ejercicioModel.duracionPorSerieSegundos,
                                repeticionesPorSerie = ejercicioModel.repeticionesPorSerie,
                                seriesRealizadas = ejercicioModel.seriesRealizadas
                                // Asegúrate de que todos los campos de EjercicioProgresoFirestore se llenen aquí
                            )
                        }

                        // Convertir la fecha String del modelo a Timestamp para Firestore
                        // ESTA ES LA PARTE COMPLICADA Y REQUIERE QUE EL STRING DE FECHA SEA PARSEABLE
                        // Si tu `progresoModel.fecha` es un String como "yyyy-MM-dd HH:mm:ss" o similar,
                        // necesitarás parsearlo a un Date y luego a Timestamp.
                        // Si `obtenerHistorialProgreso` ya te diera un Timestamp o un Long (milis), sería más fácil.
                        // Por ahora, asumiré que tienes una forma de convertir ese String a Timestamp.
                        // SI NO TIENES UNA FORMA ESTANDARIZADA, ESTO PUEDE FALLAR O SER IMPRECISO.
                        // Ejemplo muy básico (y potencialmente propenso a errores si el formato no coincide):
                        val fechaTimestamp: Timestamp = try {
                            // Si progresoModel.fecha es milisegundos desde epoch como String:
                            // Timestamp(Date(progresoModel.fecha.toLong()))
                            // Si progresoModel.fecha es un formato de fecha específico, necesitarás SimpleDateFormat:
                            // val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Ajusta el formato
                            // Timestamp(sdf.parse(progresoModel.fecha))
                            // COMO ÚLTIMO RECURSO y si los datos en `progresoModel.fecha` son basura para convertir,
                            // podrías usar Timestamp.now(), pero perderías la fecha original.
                            // ESTO ES UN PLACEHOLDER - NECESITAS UNA CONVERSIÓN ROBUSTA AQUÍ
                            Timestamp.now() // ¡¡¡REEMPLAZA ESTO CON UNA CONVERSIÓN REAL!!!
                            // Si no puedes convertirlo, tendrás que reconsiderar cómo manejas las fechas.
                        } catch (e: Exception) {
                            Log.e("HistoryViewModel", "Error convirtiendo fecha String a Timestamp: ${progresoModel.fecha}", e)
                            Timestamp.now() // Fallback, no ideal
                        }


                        ProgresoRutinaFirestore(
                            userId = userId, // El userId lo tienes de la función del ViewModel
                            rutinaIdOriginal = progresoModel.rutinaIdOriginal,
                            nombreRutina = progresoModel.nombreRutina,
                            fecha = fechaTimestamp, // Usar el Timestamp convertido
                            nivelUsuarioAlCompletar = progresoModel.nivelUsuarioAlCompletar,
                            objetivosUsuarioAlCompletar = progresoModel.objetivosUsuarioAlCompletar,
                            ejerciciosCompletados = ejerciciosFirestore,
                            rondasRealizadas = progresoModel.rondasRealizadas,
                            tiempoTotalSesionSegundos = progresoModel.tiempoTotalSesionSegundos
                        )
                    }

                    val resumenSemanal = com.jcmateus.kalisfit.data.calcularResumenSemanal(historialProgresoFirestore)

                    _historyState.value = _historyState.value.copy(
                        historialRutinas = historialProgresoModel, // Para el UI, seguimos usando el modelo original
                        resumenRutinas = resumenSemanal,
                        isLoadingRutinas = false,
                        errorMessage = if (_historyState.value.isLoadingActividadesLibres) _historyState.value.errorMessage else null
                    )
                    Log.d("HistoryViewModel", "Historial de rutinas cargado y procesado para resumen. ${historialProgresoModel.size} elementos.")
                },
                onError = { errorMsg ->
                    _historyState.value = _historyState.value.copy(
                        isLoadingRutinas = false,
                        errorMessage = "Error rutinas: $errorMsg"
                    )
                    Log.e("HistoryViewModel", "Error cargando historial de rutinas: $errorMsg")
                }
            )
        }
    }

    // Función PÚBLICA para que la UI reintente cargar el historial de rutinas
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadRoutineHistory() { // Llamada desde la UI
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d("HistoryViewModel", "Reintentando cargar historial de rutinas para usuario: $userId")
            loadRoutineHistoryInternal(userId) // Llama a la función interna
        } else {
            handleUnauthenticatedUser("reintentar cargar historial de rutinas")
        }
    }

    // --- Funciones para el Historial de Actividades Libres ---

    // Función INTERNA para escuchar cambios en actividades libres
    private fun listenForFreeActivitiesHistoryInternal(userId: String) {
        _historyState.value = _historyState.value.copy(isLoadingActividadesLibres = true, errorMessage = null)
        val activitiesRef = db.collection("users").document(userId)
            .collection("activities")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        activitiesRef.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("HistoryViewModel", "Error al escuchar actividades libres.", e)
                _historyState.value = _historyState.value.copy(
                    isLoadingActividadesLibres = false,
                    errorMessage = "Error actividades: ${e.localizedMessage}"
                )
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val userActivities = snapshots.documents.mapNotNull { document ->
                    try {
                        document.toObject(UserActivity::class.java)?.apply { id = document.id }
                    } catch (ex: Exception) {
                        Log.e("HistoryViewModel", "Error al convertir documento a UserActivity: ${document.id}", ex)
                        null
                    }
                }
                _historyState.value = _historyState.value.copy(
                    historialActividadesLibres = userActivities,
                    isLoadingActividadesLibres = false,
                    // No borrar error si el otro sigue cargando/falló
                    errorMessage = if (_historyState.value.isLoadingRutinas) _historyState.value.errorMessage else null
                )
                Log.d("HistoryViewModel", "Historial de actividades libres actualizado. ${userActivities.size} elementos.")
            } else {
                // Esto puede ocurrir si la colección está vacía o no existe inicialmente, pero el listener sigue activo.
                Log.d("HistoryViewModel", "Snapshot de actividades libres es null o no contiene documentos, pero sin error explícito.")
                _historyState.value = _historyState.value.copy(
                    historialActividadesLibres = emptyList(), // Establece una lista vacía
                    isLoadingActividadesLibres = false
                )
            }
        }
    }

    // Función PÚBLICA para que la UI reintente cargar/refrescar el historial de actividades libres
    fun loadFreeActivityHistory() { // Llamada desde la UI
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d("HistoryViewModel", "Reintentando cargar/escuchar historial de actividades libres para usuario: $userId")
            listenForFreeActivitiesHistoryInternal(userId)
        } else {
            handleUnauthenticatedUser("reintentar cargar historial de actividades libres")
        }
    }

    // --- Funciones de Gestión de Datos y UI ---

    // Función para eliminar una actividad libre (carrera/caminata)
    fun deleteFreeActivity(activityId: String?) {
        if (activityId == null) {
            Log.w("HistoryViewModel", "Intento de eliminar actividad con ID nulo.")
            _historyState.value = _historyState.value.copy(errorMessage = "ID de actividad inválido para eliminar.")
            return
        }
        val userId = auth.currentUser?.uid
        if (userId == null) {
            handleUnauthenticatedUser("eliminar actividad libre")
            return
        }

        Log.d("HistoryViewModel", "Intentando eliminar actividad libre: $activityId para usuario: $userId")
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("activities").document(activityId)
                    .delete()
                    .await()
                Log.d("HistoryViewModel", "Actividad libre eliminada de Firestore: $activityId")
                // La lista se actualizará automáticamente gracias a addSnapshotListener.
                // Podrías limpiar un mensaje de error si fuera específico de esta operación y solo si tuvo éxito.
                _historyState.value = _historyState.value.copy(errorMessage = null)
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error al eliminar actividad libre '$activityId'", e)
                _historyState.value = _historyState.value.copy(
                    errorMessage = "Error al eliminar actividad: ${e.message}"
                )
            }
        }
    }

    fun clearErrorMessage() {
        _historyState.value = _historyState.value.copy(errorMessage = null)
    }

    private fun handleUnauthenticatedUser(actionAttempted: String) {
        Log.w("HistoryViewModel", "Usuario no autenticado al intentar $actionAttempted.")
        _historyState.value = _historyState.value.copy(
            isLoadingRutinas = false, // Asume que la carga se detiene si no hay usuario
            isLoadingActividadesLibres = false,
            errorMessage = "Usuario no autenticado para $actionAttempted."
        )
    }
}