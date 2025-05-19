package com.jcmateus.kalisfit.viewmodel

import android.app.Application
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcmateus.kalisfit.data.SettingsKeys
import com.jcmateus.kalisfit.data.settingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import kotlin.text.uppercase

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Accede a tu instancia de DataStore
    private val dataStore = application.applicationContext.settingsDataStore

    // --- Notificaciones ---
    val notificationsEnabled: StateFlow<Boolean> = dataStore.data
        .catch { exception ->
            // En caso de error (ej. IOException), emite preferencias vacías
            // y registra el error o maneja de otra forma.
            if (exception is IOException) {
                // Log.e("SettingsViewModel", "Error reading notifications preference.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // Lee el valor usando la clave de SettingsKeys. Provee un valor por defecto.
            preferences[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true
        }
        .stateIn( // Convierte el Flow en un StateFlow para ser observado en la UI
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // El flujo se mantiene activo mientras haya suscriptores
            initialValue = true // Valor inicial mientras se carga el real desde DataStore
        )

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled
            }
        }
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