package com.jcmateus.kalisfit.notifications.scheduler

import com.jcmateus.kalisfit.model.AlarmItem

interface AlarmScheduler {
    fun schedule(item: AlarmItem)
    fun cancel(item: AlarmItem) // O puedes cancelar por ID: fun cancel(alarmId: Int)
    fun rescheduleAllPersistentAlarms()
}