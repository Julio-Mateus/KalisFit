package com.jcmateus.kalisfit.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import com.jcmateus.kalisfit.R

enum class AppTheme { LIGHT, DARK, SYSTEM }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.applicationContext.settingsDataStore
    private val alarmRepository: AlarmRepository = SharedPreferencesAlarmRepository(application.applicationContext)
    private val alarmScheduler: AlarmScheduler = AndroidAlarmScheduler(application.applicationContext, alarmRepository)
    
    companion object {
        const val DAILY_REMINDER_ALARM_ID = 99001
        const val DAILY_REMINDER_HOUR = 18
        const val DAILY_REMINDER_MINUTE = 0
    }

    private val _notificationPermissionGranted = MutableStateFlow(hasNotificationPermission())
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    // Preferencias de Notificaciones
    val userNotificationsPreference: StateFlow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Entrenador de Voz
    val voiceCoachEnabled: StateFlow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SettingsKeys.VOICE_COACH_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Vibración
    val vibrationEnabled: StateFlow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SettingsKeys.VIBRATION_ENABLED] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationsEffectivelyEnabled: StateFlow<Boolean> = combine(_notificationPermissionGranted, userNotificationsPreference) { perm, pref -> perm && pref }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            notificationsEffectivelyEnabled.collect { if (it) scheduleDailyReminder() else cancelDailyReminder() }
        }
    }

    fun setVoiceCoachEnabled(enabled: Boolean) = viewModelScope.launch { dataStore.edit { it[SettingsKeys.VOICE_COACH_ENABLED] = enabled } }
    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch { dataStore.edit { it[SettingsKeys.VIBRATION_ENABLED] = enabled } }
    fun setUserNotificationsPreference(enabled: Boolean) = viewModelScope.launch { dataStore.edit { it[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled } }

    private fun scheduleDailyReminder() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, DAILY_REMINDER_HOUR)
            set(Calendar.MINUTE, DAILY_REMINDER_MINUTE)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmScheduler.schedule(AlarmItem(
            id = DAILY_REMINDER_ALARM_ID,
            timeMillis = calendar.timeInMillis,
            title = getApplication<Application>().getString(R.string.daily_reminder_notification_title),
            message = getApplication<Application>().getString(R.string.daily_reminder_notification_message),
            channelId = KalisFitApplication.GENERAL_REMINDERS_CHANNEL_ID,
            isRepeating = true,
            intervalMillis = AlarmManager.INTERVAL_DAY,
            smallIconResId = R.drawable.ic_launcher_playstore_2_,
            largeIconResId = R.drawable.ic_logo2
        ))
    }

    private fun cancelDailyReminder() {
        alarmScheduler.cancel(AlarmItem(DAILY_REMINDER_ALARM_ID, 0, "", "", KalisFitApplication.GENERAL_REMINDERS_CHANNEL_ID))
    }

    // Tema
    val appTheme: StateFlow<AppTheme> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { 
            val name = it[SettingsKeys.APP_THEME] ?: AppTheme.SYSTEM.name.lowercase()
            try { AppTheme.valueOf(name.uppercase()) } catch (e: Exception) { AppTheme.SYSTEM }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)

    fun setAppTheme(theme: AppTheme) = viewModelScope.launch { dataStore.edit { it[SettingsKeys.APP_THEME] = theme.name.lowercase() } }

    // Unidades de peso
    val weightUnit: StateFlow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[SettingsKeys.WEIGHT_UNIT] ?: "kg" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "kg")

    fun setWeightUnit(unit: String) = viewModelScope.launch { dataStore.edit { it[SettingsKeys.WEIGHT_UNIT] = unit } }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun refreshNotificationPermissionStatus() { _notificationPermissionGranted.value = hasNotificationPermission() }
}
