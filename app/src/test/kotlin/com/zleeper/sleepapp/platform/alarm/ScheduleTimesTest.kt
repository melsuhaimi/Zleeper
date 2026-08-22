package com.zleeper.sleepapp.platform.alarm

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleTimesTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test fun lateAfterMidnightStillSchedulesTonightInsteadOfSkippingADay() {
        val now = instant(2026, Calendar.AUGUST, 22, 0, 10)
        val next = ScheduleTimes.nextOccurrence(now, 22 * 60 + 30, utc)
        assertEquals(instant(2026, Calendar.AUGUST, 22, 22, 30), next)
    }

    @Test fun passedClockTimeSchedulesTomorrow() {
        val now = instant(2026, Calendar.AUGUST, 22, 23, 50)
        val next = ScheduleTimes.nextOccurrence(now, 22 * 60 + 30, utc)
        assertEquals(instant(2026, Calendar.AUGUST, 23, 22, 30), next)
    }

    private fun instant(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
