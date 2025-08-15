package com.jcmateus.kalisfit.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jcmateus.kalisfit.model.AlarmItem

interface AlarmRepository {
    fun saveAlarm(alarmItem: AlarmItem)
    fun getAlarm(alarmId: Int): AlarmItem?
    fun getAllAlarms(): List<AlarmItem>
    fun deleteAlarm(alarmId: Int)
    fun clearAllAlarms() // Podría ser útil
}
class SharedPreferencesAlarmRepository(context: Context) : AlarmRepository {
    private val prefs = context.getSharedPreferences("kalisfit_alarms_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmsKey = "persistent_alarms_list"
    override fun saveAlarm(alarmItem: AlarmItem) {
        val alarms = getAllAlarms().toMutableList()
        // Eliminar si ya existe para actualizar
        alarms.removeAll { it.id == alarmItem.id }
        alarms.add(alarmItem)
        prefs.edit().putString(alarmsKey, gson.toJson(alarms)).apply()
    }
    override fun getAlarm(alarmId: Int): AlarmItem? {
        return getAllAlarms().find { it.id == alarmId }
    }
    override fun getAllAlarms(): List<AlarmItem> {
        val json = prefs.getString(alarmsKey, null)
        return if (json != null) {
            val type = object : TypeToken<List<AlarmItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
    override fun deleteAlarm(alarmId: Int) {
        val alarms = getAllAlarms().toMutableList()
        val removed = alarms.removeAll { it.id == alarmId }
        if (removed) {
            prefs.edit().putString(alarmsKey, gson.toJson(alarms)).apply()
        }
    }
    override fun clearAllAlarms() {
        prefs.edit().remove(alarmsKey).apply()
    }
}