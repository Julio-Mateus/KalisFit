package com.jcmateus.kalisfit.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.icu.util.Calendar
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.KalisFitApplication
import com.jcmateus.kalisfit.data.AlarmRepository
import com.jcmateus.kalisfit.data.SettingsKeys
import com.jcmateus.kalisfit.data.SharedPreferencesAlarmRepository
import com.jcmateus.kalisfit.data.settingsDataStore
import com.jcmateus.kalisfit.model.AlarmItem
import com.jcmateus.kalisfit.notifications.scheduler.AlarmScheduler
import com.jcmateus.kalisfit.notifications.scheduler.AndroidAlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import kotlin.text.uppercase
import com.jcmateus.kalisfit.R

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Accede a tu instancia de DataStore
    private val dataStore = application.applicationContext.settingsDataStore

    // --- INICIO: Dependencias para AlarmScheduler ---
    private val alarmRepository: AlarmRepository = SharedPreferencesAlarmRepository(application.applicationContext)
    private val alarmScheduler: AlarmScheduler = AndroidAlarmScheduler(application.applicationContext, alarmRepository)

    companion object {
        // ID único y estable para la alarma de recordatorio diario general
        const val DAILY_REMINDER_ALARM_ID = 99001
        const val DAILY_REMINDER_HOUR = 10 // Ejemplo: 10 AM
        const val DAILY_REMINDER_MINUTE = 0  // Ejemplo: 00 minutos
    }
    // --- FIN: Dependencias para AlarmScheduler ---


    val notificationsEnabled: StateFlow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SettingsKeys.NOTIFICATIONS_ENABLED] ?: false // Default a false para que se active al cambiar
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // O el valor que consideres inicial antes de leer DataStore
        )

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // Guardar la preferencia en DataStore
            dataStore.edit { settings ->
                settings[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled
            }

            // Programar o cancelar la alarma basada en el nuevo estado
            if (enabled) {
                scheduleDailyReminder()
            } else {
                cancelDailyReminder()
            }
        }
    }

    private fun scheduleDailyReminder() {
        // Crear el objeto AlarmItem para el recordatorio diario
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, DAILY_REMINDER_HOUR)
            set(Calendar.MINUTE, DAILY_REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // Si la hora ya pasó hoy, programar para mañana
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val dailyReminderAlarm = AlarmItem(
            id = DAILY_REMINDER_ALARM_ID,
            timeMillis = calendar.timeInMillis,
            title = getApplication<Application>().getString(R.string.daily_reminder_notification_title), // "Recordatorio KalisFit"
            message = getApplication<Application>().getString(R.string.daily_reminder_notification_message), // "¡Es hora de tu dosis de bienestar!"
            channelId = KalisFitApplication.GENERAL_REMINDERS_CHANNEL_ID, // Usar el canal general o uno específico
            isRepeating = true,
            intervalMillis = AlarmManager.INTERVAL_DAY // Repetir diariamente
        )

        alarmScheduler.schedule(dailyReminderAlarm)
        // Log.d("SettingsViewModel", "Recordatorio diario programado para las ${DAILY_REMINDER_HOUR}:${DAILY_REMINDER_MINUTE}")
    }

    private fun cancelDailyReminder() {
        // Para cancelar, solo necesitamos un AlarmItem con el ID correcto.
        // Los otros campos no son estrictamente necesarios para la lógica de cancelación
        // que busca por ID en PendingIntent o en el repositorio.
        val alarmToCancel = AlarmItem(
            id = DAILY_REMINDER_ALARM_ID,
            timeMillis = 0, // No relevante para la cancelación por ID
            title = "",     // No relevante
            message = "",   // No relevante
            channelId = ""  // No relevante
        )
        alarmScheduler.cancel(alarmToCancel)
        // Log.d("SettingsViewModel", "Recordatorio diario cancelado.")
    }

    // --- Tema de la App ---
    val appTheme: StateFlow<AppTheme> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                // Log.e("SettingsViewModel", "Error reading app theme preference.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeName = preferences[SettingsKeys.APP_THEME] ?: AppTheme.SYSTEM.name.lowercase()
            try {
                AppTheme.valueOf(themeName.uppercase())
            } catch (e: IllegalArgumentException) {
                // Log.w("SettingsViewModel", "Invalid theme name in DataStore: $themeName", e)
                AppTheme.SYSTEM // Valor por defecto si el string guardado no es un enum válido
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsKeys.APP_THEME] = theme.name.lowercase()
            }
        }
    }

    // --- Unidad de Peso ---
    val weightUnit: StateFlow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                // Log.e("SettingsViewModel", "Error reading weight unit preference.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SettingsKeys.WEIGHT_UNIT] ?: "kg" // "kg" o "lbs"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "kg"
        )

    fun setWeightUnit(unit: String) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsKeys.WEIGHT_UNIT] = unit
            }
        }
    }

    // --- Versión de la App (Ejemplo) ---
    // En una app real, obtendrías esto de BuildConfig o PackageInfo
    val appVersion: String by lazy { // Usamos 'by lazy' para obtenerlo solo cuando se necesite
        try {
            val context = getApplication<Application>().applicationContext
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "N/A"
        } catch (e: Exception) {
            // Log.e("SettingsViewModel", "Error getting app version", e)
            "N/A"
        }
    }

    // El bloque init que tenías comentado ya no es necesario de la misma forma,
    // porque los StateFlows se inicializan y actualizan directamente desde el Flow de DataStore.
    // La recolección (collect) se hará en la UI (por ejemplo, con collectAsStateWithLifecycle).
}