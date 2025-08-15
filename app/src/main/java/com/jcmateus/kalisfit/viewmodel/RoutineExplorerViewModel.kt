package com.jcmateus.kalisfit.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.type.Date
import com.jcmateus.kalisfit.KalisFitApplication
import com.jcmateus.kalisfit.R
import com.jcmateus.kalisfit.data.AlarmRepository
import com.jcmateus.kalisfit.data.SharedPreferencesAlarmRepository
import com.jcmateus.kalisfit.data.obtenerRutinas
import com.jcmateus.kalisfit.model.AlarmItem
import com.jcmateus.kalisfit.model.GrupoMuscular
import com.jcmateus.kalisfit.model.LugarEntrenamiento
import com.jcmateus.kalisfit.model.Rutina
import com.jcmateus.kalisfit.notifications.scheduler.AlarmScheduler
import com.jcmateus.kalisfit.notifications.scheduler.AndroidAlarmScheduler
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



class RoutineExplorerViewModel(application: Application) : AndroidViewModel(application) {
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
    private val alarmRepository: AlarmRepository =
        SharedPreferencesAlarmRepository(getApplication<Application>().applicationContext)
    private val alarmScheduler: AlarmScheduler =
        AndroidAlarmScheduler(getApplication<Application>().applicationContext, alarmRepository)
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
                    // Asumiendo que rutina.lugarEntrenamiento es List<LugarEntrenamiento>
                    rutina.lugarEntrenamiento.any { lugarRutinaEnum -> // lugarRutinaEnum es de tipo LugarEntrenamiento
                        lugarRutinaEnum.name.lowercase() == nombreLugarFiltroMinusculas
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
    // IDs de ejemplo. En una app real, estos serían dinámicos o basados en la rutina.
    companion object {
        // Un prefijo para asegurar que los IDs de alarma de rutina no colisionen con otros tipos de alarmas
        const val ROUTINE_ALARM_ID_PREFIX = 20000
    }
    init {
        loadAllRutinas()
    }
    fun scheduleRoutineReminder(
        rutina: Rutina,
        timeInMillis: Long,
        isRepeating: Boolean = false,
        intervalMillis: Long? = null
    ) {
        if (rutina.id.isBlank()) {
            Log.e(TAG, "No se puede programar recordatorio: ID de rutina vacío.")
            _errorMessage.value = "Error: ID de rutina no válido para programar recordatorio."
            return
        }

        // Generar un ID único para esta alarma de rutina específica.
        // Usamos el hashcode del ID de la rutina más un prefijo.
        // Considera una estrategia de ID más robusta si los IDs de rutina no son suficientemente únicos
        // o si pueden cambiar y necesitas persistencia del recordatorio.
        val alarmIdForRoutine = ROUTINE_ALARM_ID_PREFIX + rutina.id.hashCode()

        val routineReminderAlarm = AlarmItem(
            id = alarmIdForRoutine,
            timeMillis = timeInMillis,
            title = rutina.nombre,
            message = "Hora de entrenar!",
            // --- ACCESO CORRECTO A LAS CONSTANTES ---
            channelId = KalisFitApplication.TRAINING_REMINDER_CHANNEL_ID,
            isRepeating = isRepeating,
            intervalMillis = intervalMillis,
            largeIconResId = R.drawable.ic_stat_notification, // Asegúrate que este drawable exista
            dataPayload = rutina.id
        )

        alarmScheduler.schedule(routineReminderAlarm)
        Log.d(TAG, "Recordatorio programado para rutina '${rutina.nombre}' (ID: ${rutina.id}) con AlarmID: $alarmIdForRoutine a las ${
            java.util.Date(
                timeInMillis
            )
        }")
        // Podrías mostrar un mensaje de éxito en la UI
        // _successMessage.value = "Recordatorio para '${rutina.nombre}' programado."
    }
    /**
     * Cancela un recordatorio previamente programado para una rutina específica.
     *
     * @param rutinaId El ID de la rutina cuyo recordatorio se desea cancelar.
     */
    fun cancelRoutineReminder(rutinaId: String) {
        if (rutinaId.isBlank()) {
            Log.e(TAG, "No se puede cancelar recordatorio: ID de rutina vacío.")
            _errorMessage.value = "Error: ID de rutina no válido para cancelar recordatorio."
            return
        }

        val alarmIdToCancel = ROUTINE_ALARM_ID_PREFIX + rutinaId.hashCode()
        val alarmToCancel =
            AlarmItem( // Solo el ID y el channelId son estrictamente necesarios para el cancel
                id = alarmIdToCancel,
                timeMillis = 0, // No relevante para cancelar por ID
                title = "", message = "",
                channelId = KalisFitApplication.TRAINING_REMINDER_CHANNEL_ID, // Útil para el scheduler si lo usa
                largeIconResId = null // No relevante
            )

        alarmScheduler.cancel(alarmToCancel)
        Log.d(TAG, "Recordatorio cancelado para rutina con ID: $rutinaId (AlarmID: $alarmIdToCancel)")
        // Podrías mostrar un mensaje de éxito en la UI
        // _successMessage.value = "Recordatorio cancelado."
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