package com.jcmateus.kalisfit.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val APP_THEME = stringPreferencesKey("app_theme") // "light", "dark", "system"
    val WEIGHT_UNIT = stringPreferencesKey("weight_unit") // "kg", "lbs"
    val VOICE_COACH_ENABLED = booleanPreferencesKey("voice_coach_enabled")
    val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
}