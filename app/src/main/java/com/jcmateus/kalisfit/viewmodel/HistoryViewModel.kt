package com.jcmateus.kalisfit.viewmodel

import java.util.Calendar
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import java.util.Locale
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jcmateus.kalisfit.data.ProgresoRutinaFirestore
import com.jcmateus.kalisfit.data.ResumenSemanal
import com.jcmateus.kalisfit.data.calcularResumenParaSemanaEspecifica
import com.jcmateus.kalisfit.data.calcularResumenSemanal
import com.jcmateus.kalisfit.data.obtenerHistorialProgreso
import com.jcmateus.kalisfit.model.ProgresoRutina
import com.jcmateus.kalisfit.model.UserActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Nuevo Data Class para el resumen semanal con información de fechas
data class ResumenSemanalConFechas(
    val resumen: ResumenSemanal,
    val fechaInicioSemana: Timestamp,
    val fechaFinSemana: Timestamp,
    val semanaDelAnio: Int, // Ej: 1 para la primera semana, 52 para la última
    val anio: Int,
    // Opcional pero recomendado: la lista de progresos que componen este resumen
    // para no tener que filtrar de nuevo en la UI para los gráficos.
    val progresosDeLaSemana: List<ProgresoRutina>
)
// Define un data class para representar el estado de la UI de HistorialScreen
data class HistoryUiState(
    // Historial de Rutinas
    val historialRutinas: List<ProgresoRutina> = emptyList(),
    // val resumenRutinas: ResumenSemanal? = null, // <- Eliminamos este
    val listaResumenesSemanales: List<ResumenSemanalConFechas> = emptyList(), // <- Nuevo
    val isLoadingRutinas: Boolean = true,
    val isLoadingResumenesSemanales: Boolean = true, // <- Nuevo estado de carga para resúmenes
    // Historial de Actividades Libres (carreras/caminatas)
    val historialActividadesLibres: List<UserActivity> = emptyList(),
    val isLoadingActividadesLibres: Boolean = true,
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
            // loadRoutineHistoryInternal ahora también se encargará de los resúmenes semanales
            loadRoutineHistoryInternal(userId)
            listenForFreeActivitiesHistoryInternal(userId)
        } else {
            Log.w("HistoryViewModel", "Usuario no autenticado en init. No se cargarán historiales.")
            _historyState.value = HistoryUiState(
                isLoadingRutinas = false,
                isLoadingActividadesLibres = false,
                isLoadingResumenesSemanales = false, // <- Añadir
                errorMessage = "Usuario no autenticado."
            )
        }
    }
    // --- Funciones para el Historial de Rutinas ---
    // Función INTERNA para cargar el historial de rutinas
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadRoutineHistoryInternal(userId: String) {
        // Actualizamos el estado para indicar que tanto rutinas como resúmenes están cargando
        _historyState.update {
            it.copy(
                isLoadingRutinas = true,
                isLoadingResumenesSemanales = true, // <- Añadir
                errorMessage = null
            )
        }
        viewModelScope.launch {
            // Llama a tu función existente para obtener el historial
            obtenerHistorialProgreso(
                userId = userId,
                onResult = { historialProgresoModel -> // Esto es List<com.jcmateus.kalisfit.model.ProgresoRutina>
                    // Ordenar el historial por fecha descendente, es útil para la agrupación
                    val historialOrdenado =
                        historialProgresoModel.sortedByDescending { it.fecha.seconds }
                    // Generar la lista de resúmenes semanales
                    val resumenesSemanales =
                        generarListaResumenesSemanales(historialOrdenado, userId)
                    // Mapear el historial del modelo a ProgresoRutinaFirestore si todavía
                    // lo necesitas para alguna otra función (como `calcularResumenSemanal` original).
                    // Si `generarListaResumenesSemanales` ya hace el mapeo internamente,
                    // este bloque podría no ser necesario o ser parte de esa función.
                    // Por ahora, asumiré que tu `calcularResumenSemanal` (singular) toma Firestore.
                    val historialProgresoFirestore = historialOrdenado.map { progresoModel ->
                        val ejerciciosFirestore =
                            progresoModel.ejerciciosCompletados.map { ejercicioModel ->
                                com.jcmateus.kalisfit.data.EjercicioProgresoFirestore(
                                    ejercicioIdOriginal = ejercicioModel.ejercicioIdOriginal,
                                    nombre = ejercicioModel.nombre,
                                    duracionPorSerieSegundos = ejercicioModel.duracionPorSerieSegundos,
                                    repeticionesPorSerie = ejercicioModel.repeticionesPorSerie,
                                    seriesRealizadas = ejercicioModel.seriesRealizadas
                                )
                            }
                        ProgresoRutinaFirestore(
                            userId = userId,
                            rutinaIdOriginal = progresoModel.rutinaIdOriginal,
                            nombreRutina = progresoModel.nombreRutina,
                            fecha = progresoModel.fecha,
                            nivelUsuarioAlCompletar = progresoModel.nivelUsuarioAlCompletar,
                            objetivosUsuarioAlCompletar = progresoModel.objetivosUsuarioAlCompletar,
                            ejerciciosCompletados = ejerciciosFirestore,
                            rondasRealizadas = progresoModel.rondasRealizadas,
                            tiempoTotalSesionSegundos = progresoModel.tiempoTotalSesionSegundos
                        )
                    }
                    // Si todavía necesitas un resumen general de "la última actividad" o algo así.
                    // Si no, este `resumenGeneral` podría eliminarse.
                    val resumenGeneral = if (historialProgresoFirestore.isNotEmpty()) {
                        com.jcmateus.kalisfit.data.calcularResumenSemanal(historialProgresoFirestore)
                    } else {
                        null
                    }
                    _historyState.update { currentState ->
                        currentState.copy(
                            historialRutinas = historialOrdenado, // Guardamos el historial del modelo original ordenado
                            listaResumenesSemanales = resumenesSemanales, // Guardamos la nueva lista
                            // resumenRutinas = resumenGeneral, // Si todavía necesitas el resumen global
                            isLoadingRutinas = false,
                            isLoadingResumenesSemanales = false, // <- Marcamos como cargado
                            errorMessage = if (currentState.isLoadingActividadesLibres) currentState.errorMessage else null
                        )
                    }
                    Log.d(
                        "HistoryViewModel",
                        "Historial de rutinas y resúmenes semanales cargados. ${historialOrdenado.size} rutinas, ${resumenesSemanales.size} resúmenes."
                    )
                },
                onError = { errorMsg ->
                    _historyState.update {
                        it.copy(
                            isLoadingRutinas = false,
                            isLoadingResumenesSemanales = false, // <- Marcar como no cargando en error
                            errorMessage = "Error rutinas: $errorMsg"
                        )
                    }
                    Log.e("HistoryViewModel", "Error cargando historial de rutinas: $errorMsg")
                }
            )
        }
    }
    // --- NUEVA FUNCIÓN PRIVADA PARA GENERAR LOS RESÚMENES SEMANALES ---
    @RequiresApi(Build.VERSION_CODES.O)
    private fun generarListaResumenesSemanales(
        historialCompleto: List<ProgresoRutina>, // Lista de model.ProgresoRutina, ya ordenada desc.
        userId: String // Necesario para crear ProgresoRutinaFirestore si `calcularResumenSemanal` lo requiere
    ): List<ResumenSemanalConFechas> {
        if (historialCompleto.isEmpty()) return emptyList()
        val calendar = Calendar.getInstance(Locale.getDefault()) // Usar Locale para consistencia
        // Ajustar el primer día de la semana si es necesario (ej. Lunes)
        // calendar.firstDayOfWeek = Calendar.MONDAY // Descomentar si tu semana empieza en Lunes
        // Agrupar progresos por clave "año-semanaDelAñoISO"
        val progresosAgrupadosPorSemana = historialCompleto.groupBy { progreso ->
            calendar.time = progreso.fecha.toDate()
            // Para obtener una semana ISO consistente (Lunes-Domingo)
            // Si puedes usar java.time (API 26+), es preferible:
            // val fechaLocalDateTime = progreso.fecha.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
            // val semanaIso = fechaLocalDateTime.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
            // val anioIso = fechaLocalDateTime.get(java.time.temporal.WeekFields.ISO.weekBasedYear())
            // "${anioIso}-${semanaIso}"
            //
            // Usando Calendar, requiere más cuidado:
            calendar.minimalDaysInFirstWeek = 4 // Estándar ISO 8601
            calendar.firstDayOfWeek = Calendar.MONDAY // Estándar ISO 8601
            val anio = calendar.get(Calendar.YEAR)
            // WORKAROUND para el año de la semana: si es la semana 52 o 53 y el mes es enero, es del año anterior.
            // Si es la semana 1 y el mes es diciembre, es del año siguiente.
            val semanaDelAnio = calendar.get(Calendar.WEEK_OF_YEAR)
            val mes = calendar.get(Calendar.MONTH)
            val anioRealDeLaSemana = when {
                semanaDelAnio >= 52 && mes == Calendar.JANUARY -> anio - 1
                semanaDelAnio == 1 && mes == Calendar.DECEMBER -> anio + 1
                else -> anio
            }
            "${anioRealDeLaSemana}-${semanaDelAnio}"
        }
        val listaResumenes = mutableListOf<ResumenSemanalConFechas>()
        for ((_, progresosDeLaSemanaModel) in progresosAgrupadosPorSemana) { // progresosDeLaSemanaModel es List<model.ProgresoRutina>
            if (progresosDeLaSemanaModel.isNotEmpty()) {
                // Mapear los progresos de esta semana a ProgresoRutinaFirestore
                // para usar con tu función calcularResumenSemanal existente.
                val progresosDeLaSemanaFirestore = progresosDeLaSemanaModel.map { progresoModel ->
                    val ejerciciosFirestore =
                        progresoModel.ejerciciosCompletados.map { ejercicioModel ->
                            // Asumo que EjercicioProgresoFirestore está disponible o importado correctamente
                            com.jcmateus.kalisfit.data.EjercicioProgresoFirestore(
                                ejercicioIdOriginal = ejercicioModel.ejercicioIdOriginal,
                                nombre = ejercicioModel.nombre,
                                duracionPorSerieSegundos = ejercicioModel.duracionPorSerieSegundos,
                                repeticionesPorSerie = ejercicioModel.repeticionesPorSerie,
                                seriesRealizadas = ejercicioModel.seriesRealizadas
                            )
                        }
                    // Asumo que ProgresoRutinaFirestore está disponible o importado correctamente
                    ProgresoRutinaFirestore(
                        userId = userId,
                        rutinaIdOriginal = progresoModel.rutinaIdOriginal,
                        nombreRutina = progresoModel.nombreRutina,
                        fecha = progresoModel.fecha,
                        nivelUsuarioAlCompletar = progresoModel.nivelUsuarioAlCompletar,
                        objetivosUsuarioAlCompletar = progresoModel.objetivosUsuarioAlCompletar,
                        ejerciciosCompletados = ejerciciosFirestore,
                        rondasRealizadas = progresoModel.rondasRealizadas,
                        tiempoTotalSesionSegundos = progresoModel.tiempoTotalSesionSegundos
                    )
                }
                // Calcular el resumen para esta semana específica
                // Asegúrate de que `calcularResumenSemanal` esté disponible en este contexto
                // y que devuelva un objeto `ResumenSemanal` apropiado para el historial.
                Log.d("HistoryVM_Debug", "Procesando semana: ${progresosDeLaSemanaModel.firstOrNull()?.fecha?.toDate()}")
                Log.d("HistoryVM_Debug", "Número de progresos para esta semana: ${progresosDeLaSemanaModel.size}")
                progresosDeLaSemanaModel.forEachIndexed { index, progreso ->
                    Log.d("HistoryVM_Debug", "  Progreso $index: ${progreso.nombreRutina}, Ejercicios: ${progreso.ejerciciosCompletados.size}, Series totales en este progreso: ${progreso.ejerciciosCompletados.sumOf { it.seriesRealizadas }}")
                }
                val resumenDeLaSemana =
                    calcularResumenParaSemanaEspecifica(progresosDeLaSemanaFirestore)
                Log.d("HistoryVM_Debug", "Resumen calculado para la semana - TotalEjercicios: ${resumenDeLaSemana.totalEjercicios}, Rutinas: ${resumenDeLaSemana.rutinas}")
                // --- INICIO: CÁLCULO PRECISO DE INICIO Y FIN DE SEMANA ISO ---
                // Tomar una fecha de referencia de esta semana (la primera es suficiente)
                val fechaReferenciaEnSemana = progresosDeLaSemanaModel.first().fecha.toDate()
                // Calcular Lunes de esa semana a las 00:00:00
                val calInicioSemana = Calendar.getInstance(Locale.getDefault())
                calInicioSemana.time = fechaReferenciaEnSemana
                calInicioSemana.firstDayOfWeek = Calendar.MONDAY // Importante para la norma ISO
                calInicioSemana.minimalDaysInFirstWeek = 4     // Importante para la norma ISO
                // Retroceder al Lunes de la semana ISO
                calInicioSemana.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                // Normalizar la hora
                calInicioSemana.set(Calendar.HOUR_OF_DAY, 0)
                calInicioSemana.set(Calendar.MINUTE, 0)
                calInicioSemana.set(Calendar.SECOND, 0)
                calInicioSemana.set(Calendar.MILLISECOND, 0)
                val inicioSemanaRealTimestamp = Timestamp(calInicioSemana.time)
                // Calcular Domingo de esa semana a las 23:59:59
                val calFinSemana = Calendar.getInstance(Locale.getDefault())
                calFinSemana.time = inicioSemanaRealTimestamp.toDate() // Empezar desde el Lunes calculado
                calFinSemana.add(Calendar.DAY_OF_YEAR, 6) // Lunes + 6 días = Domingo
                // Normalizar la hora al final del día
                calFinSemana.set(Calendar.HOUR_OF_DAY, 23)
                calFinSemana.set(Calendar.MINUTE, 59)
                calFinSemana.set(Calendar.SECOND, 59)
                calFinSemana.set(Calendar.MILLISECOND, 999)
                val finSemanaRealTimestamp = Timestamp(calFinSemana.time)
                // --- FIN: CÁLCULO PRECISO DE INICIO Y FIN DE SEMANA ISO ---
                // Obtener año y semana para el ResumenSemanalConFechas (usando el Lunes calculado para consistencia)
                // El Calendar 'calendar' global ya está configurado con Locale, firstDayOfWeek y minimalDaysInFirstWeek
                // pero lo reconfiguramos aquí para asegurar que usamos el inicioSemanaRealTimestamp como base.
                val calendarParaInfoSemana = Calendar.getInstance(Locale.getDefault())
                calendarParaInfoSemana.time = inicioSemanaRealTimestamp.toDate()
                calendarParaInfoSemana.firstDayOfWeek = Calendar.MONDAY // ISO 8601
                calendarParaInfoSemana.minimalDaysInFirstWeek = 4     // ISO 8601
                val anioSemana = calendarParaInfoSemana.get(Calendar.YEAR)
                val semanaDelAnio = calendarParaInfoSemana.get(Calendar.WEEK_OF_YEAR)
                val mesSemana = calendarParaInfoSemana.get(Calendar.MONTH) // Para el workaround del año
                // WORKAROUND para el año de la semana: si es la semana 52 o 53 y el mes es enero, es del año anterior.
                // Si es la semana 1 y el mes es diciembre, es del año siguiente.
                val anioRealSemana = when {
                    semanaDelAnio >= 52 && mesSemana == Calendar.JANUARY -> anioSemana - 1
                    semanaDelAnio == 1 && mesSemana == Calendar.DECEMBER -> anioSemana + 1
                    else -> anioSemana
                }
                listaResumenes.add(
                    ResumenSemanalConFechas(
                        resumen = resumenDeLaSemana,
                        fechaInicioSemana = inicioSemanaRealTimestamp, // Lunes real de la semana ISO
                        fechaFinSemana = finSemanaRealTimestamp,   // Domingo real de la semana ISO
                        semanaDelAnio = semanaDelAnio,
                        anio = anioRealSemana,
                        progresosDeLaSemana = progresosDeLaSemanaModel // Guardamos los progresos originales del modelo
                    )
                )
            }
        }
        // Ordenar los resúmenes por año y luego por semana, descendente (más reciente primero)
        return listaResumenes.sortedWith(compareByDescending<ResumenSemanalConFechas> { it.anio }.thenByDescending { it.semanaDelAnio })
    }
    // Función PÚBLICA para que la UI reintente cargar el historial de rutinas
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadRoutineHistory() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d(
                "HistoryViewModel",
                "Reintentando cargar historial de rutinas y resúmenes para usuario: $userId"
            )
            loadRoutineHistoryInternal(userId)
        } else {
            handleUnauthenticatedUser("reintentar cargar historial de rutinas")
        }
    }
    // --- Funciones para el Historial de Actividades Libres ---
    // Función INTERNA para escuchar cambios en actividades libres
    private fun listenForFreeActivitiesHistoryInternal(userId: String) {
        _historyState.update {
            it.copy(
                isLoadingActividadesLibres = true,
                errorMessage = if (it.isLoadingRutinas || it.isLoadingResumenesSemanales) it.errorMessage else null
            )
        }
        val activitiesRef = db.collection("users").document(userId)
            .collection("activities")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        activitiesRef.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("HistoryViewModel", "Error al escuchar actividades libres.", e)
                _historyState.update {
                    it.copy(
                        isLoadingActividadesLibres = false,
                        errorMessage = "Error actividades: ${e.localizedMessage}"
                    )
                }
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val userActivities = snapshots.documents.mapNotNull { document ->
                    try {
                        document.toObject(UserActivity::class.java)?.apply { id = document.id }
                    } catch (ex: Exception) {
                        Log.e(
                            "HistoryViewModel",
                            "Error al convertir documento a UserActivity: ${document.id}",
                            ex
                        )
                        null
                    }
                }
                _historyState.update { currentState ->
                    currentState.copy(
                        historialActividadesLibres = userActivities,
                        isLoadingActividadesLibres = false,
                        errorMessage = if (currentState.isLoadingRutinas || currentState.isLoadingResumenesSemanales) currentState.errorMessage else null
                    )
                }
                Log.d(
                    "HistoryViewModel",
                    "Historial de actividades libres actualizado. ${userActivities.size} elementos."
                )
            } else {
                Log.d(
                    "HistoryViewModel",
                    "Snapshot de actividades libres es null o no contiene documentos, pero sin error explícito."
                )
                _historyState.update {
                    it.copy(
                        historialActividadesLibres = emptyList(),
                        isLoadingActividadesLibres = false
                    )
                }
            }
        }
    }
    fun loadFreeActivityHistory() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            Log.d(
                "HistoryViewModel",
                "Reintentando cargar/escuchar historial de actividades libres para usuario: $userId"
            )
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
            _historyState.update { it.copy(errorMessage = "ID de actividad inválido para eliminar.") }
            return
        }
        val userId = auth.currentUser?.uid
        if (userId == null) {
            handleUnauthenticatedUser("eliminar actividad libre")
            return
        }

        Log.d(
            "HistoryViewModel",
            "Intentando eliminar actividad libre: $activityId para usuario: $userId"
        )
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("activities").document(activityId)
                    .delete()
                    .await()
                Log.d("HistoryViewModel", "Actividad libre eliminada de Firestore: $activityId")
                _historyState.update { it.copy(errorMessage = null) }
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error al eliminar actividad libre '$activityId'", e)
                _historyState.update { it.copy(errorMessage = "Error al eliminar actividad: ${e.message}") }
            }
        }
    }
    fun clearErrorMessage() {
        _historyState.update { it.copy(errorMessage = null) }
    }
    private fun handleUnauthenticatedUser(actionAttempted: String) {
        Log.w("HistoryViewModel", "Usuario no autenticado al intentar $actionAttempted.")
        _historyState.update {
            it.copy(
                isLoadingRutinas = false,
                isLoadingActividadesLibres = false,
                isLoadingResumenesSemanales = false, // <- Añadir
                errorMessage = "Usuario no autenticado para $actionAttempted."
            )
        }
    }
}
