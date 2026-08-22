package com.zleeper.sleepapp.platform.alarm

import java.util.Calendar
import java.util.TimeZone

object ScheduleTimes {
    fun nextOccurrence(nowEpochMs: Long, minuteOfDay: Int, timeZone: TimeZone = TimeZone.getDefault()): Long {
        require(minuteOfDay in 0..1439)
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMs
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= nowEpochMs) calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }
}
