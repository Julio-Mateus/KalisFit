package com.jcmateus.kalisfit.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object RoutinePersistenceKeys {
    val ACTIVE_ROUTINE_ID = stringPreferencesKey("active_routine_id")
    val ACTIVE_CUSTOM_ROUTINE_ID = stringPreferencesKey("active_custom_routine_id")
    val CURRENT_ROUND = intPreferencesKey("current_round")
    val CURRENT_EXERCISE_INDEX = intPreferencesKey("current_exercise_index")
    val CURRENT_SET = intPreferencesKey("current_set")
    val CURRENT_COMPONENT_INDEX = intPreferencesKey("current_component_index")
    val IS_SESSION_ACTIVE = booleanPreferencesKey("is_session_active")
    val ELAPSED_SECONDS = intPreferencesKey("elapsed_seconds")
}