package com.jcmateus.kalisfit.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.*

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
        const val DAILY_REMINDER_HOUR = 16 // Ejemplo: 10 AM
        const val DAILY_REMINDER_MINUTE = 0  // Ejemplo: 00 minutos
        val DAILY_REMINDER_INITIALLY_SCHEDULED =
            booleanPreferencesKey("daily_reminder_initially_scheduled")
    }
    // Estado para saber si el permiso de notificación está concedido
    private val _notificationPermissionGranted = MutableStateFlow(hasNotificationPermission())
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    // Preferencia del usuario para habilitar/deshabilitar notificaciones (controlado por el Switch)
    // Este es el StateFlow que refleja SettingsKeys.NOTIFICATIONS_ENABLED de DataStore.
    val userNotificationsPreference: StateFlow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                // Log.e("SettingsViewModel", "Error reading notifications preference.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // Default a TRUE: Queremos que las notificaciones estén activadas por defecto
            // si el permiso del sistema se concede. El usuario puede luego desactivarlas.
            preferences[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // Coincide con el default del map
        )

    // Este Flow combina el estado del permiso y la preferencia del usuario
    // para determinar si las notificaciones deben estar *efectivamente* activas.
    val notificationsEffectivelyEnabled: StateFlow<Boolean> =
        combine( // Asegúrate de que kotlinx.coroutines.flow.combine esté importado
            _notificationPermissionGranted,
            userNotificationsPreference // Usamos el StateFlow unificado
        ) { permissionGranted, userPreferenceEnabled ->
            permissionGranted && userPreferenceEnabled
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = hasNotificationPermission() && userNotificationsPreference.value // Un valor inicial más preciso
        )

    init {
        // Observar los cambios en notificationsEffectivelyEnabled para programar/cancelar
        viewModelScope.launch {
            notificationsEffectivelyEnabled.collect { effectivelyEnabled ->
                if (effectivelyEnabled) {
                    // Si las notificaciones están efectivamente habilitadas,
                    // programar (o asegurarse de que esté programado) el recordatorio diario.
                    scheduleDailyReminder()
                } else {
                    // Si no están efectivamente habilitadas (ya sea por permiso o preferencia del usuario),
                    // cancelar el recordatorio diario.
                    cancelDailyReminder()
                }
            }
        }
    }
    fun refreshNotificationPermissionStatus() {
        _notificationPermissionGranted.value = hasNotificationPermission()
    }
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // En versiones anteriores a Android 13 (TIRAMISU),
            // el permiso se considera otorgado si está en el Manifest.
            true
        }
    }
    // Esta función es llamada por el Switch en SettingsScreen para cambiar la PREFERENCIA DEL USUARIO
    fun setUserNotificationsPreference(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled
            }
            // No es necesario llamar a schedule/cancel aquí directamente.
            // El `collect` en el bloque `init` reaccionará al cambio en
            // `userNotificationsPreference`, que a su vez afecta a `notificationsEffectivelyEnabled`.
        }
    }
    private fun scheduleDailyReminder() {
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
            title = getApplication<Application>().getString(R.string.daily_reminder_notification_title),
            message = getApplication<Application>().getString(R.string.daily_reminder_notification_message),
            channelId = KalisFitApplication.GENERAL_REMINDERS_CHANNEL_ID,
            isRepeating = true,
            intervalMillis = AlarmManager.INTERVAL_DAY,
            largeIconResId = R.drawable.ic_logo2, // **ASEGÚRATE QUE R.drawable.ic_logo2 EXISTE**
            smallIconResId = R.drawable.ic_stat_kalisfit_notification
        )
        alarmScheduler.schedule(dailyReminderAlarm)
        // Log.d("SettingsViewModel", "Recordatorio diario programado para las ${DAILY_REMINDER_HOUR}:${DAILY_REMINDER_MINUTE}")
    }
    private fun cancelDailyReminder() {
        // Para cancelar, solo necesitamos un AlarmItem con el ID correcto.
        // El AlarmScheduler (específicamente AndroidAlarmScheduler) debería ser capaz
        // de cancelar basado solo en el ID si así está implementado.
        // Si tu SharedPreferencesAlarmRepository necesita más datos, ajústalo.
        val alarmToCancel = AlarmItem(
            id = DAILY_REMINDER_ALARM_ID,
            timeMillis = 0, // No estrictamente necesario para la cancelación por ID
            title = "",     // No relevante
            message = "",   // No relevante
            channelId = KalisFitApplication.GENERAL_REMINDERS_CHANNEL_ID, // Puede ser útil si el repositorio lo usa
            largeIconResId = null // No relevante para cancelar
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