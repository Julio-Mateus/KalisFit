package com.jcmateus.kalisfit.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.routineDataStore: DataStore<Preferences> by preferencesDataStore(name = "routine_progress")

class RoutineRepository(private val context: Context) {
    private val dataStore = context.routineDataStore

    val activeRoutineId: Flow<String?> = dataStore.data.map { it[RoutinePersistenceKeys.ACTIVE_ROUTINE_ID] }
    val activeCustomRoutineId: Flow<String?> = dataStore.data.map { it[RoutinePersistenceKeys.ACTIVE_CUSTOM_ROUTINE_ID] }
    val currentRound: Flow<Int> = dataStore.data.map { it[RoutinePersistenceKeys.CURRENT_ROUND] ?: 1 }
    val currentExerciseIndex: Flow<Int> = dataStore.data.map { it[RoutinePersistenceKeys.CURRENT_EXERCISE_INDEX] ?: 0 }
    val currentSet: Flow<Int> = dataStore.data.map { it[RoutinePersistenceKeys.CURRENT_SET] ?: 1 }
    val isSessionActive: Flow<Boolean> = dataStore.data.map { it[RoutinePersistenceKeys.IS_SESSION_ACTIVE] ?: false }

    suspend fun saveProgress(
        routineId: String?,
        customRoutineId: String?,
        round: Int,
        exerciseIndex: Int,
        set: Int,
        isActive: Boolean
    ) {
        dataStore.edit { preferences ->
            routineId?.let { preferences[RoutinePersistenceKeys.ACTIVE_ROUTINE_ID] = it }
            customRoutineId?.let { preferences[RoutinePersistenceKeys.ACTIVE_CUSTOM_ROUTINE_ID] = it }
            preferences[RoutinePersistenceKeys.CURRENT_ROUND] = round
            preferences[RoutinePersistenceKeys.CURRENT_EXERCISE_INDEX] = exerciseIndex
            preferences[RoutinePersistenceKeys.CURRENT_SET] = set
            preferences[RoutinePersistenceKeys.IS_SESSION_ACTIVE] = isActive
        }
    }

    suspend fun clearProgress() {
        dataStore.edit { it.clear() }
    }
}
