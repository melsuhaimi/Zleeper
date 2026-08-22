package com.zleeper.sleepapp.domain.sleep

import kotlin.math.abs

object SleepSchedule {
    fun durationBetween(sleepMinutes: Int, wakeMinutes: Int): Int {
        require(sleepMinutes in 0..1439 && wakeMinutes in 0..1439)
        val forward = (wakeMinutes - sleepMinutes + 1440) % 1440
        return if (forward == 0) 1440 else forward
    }

    /** Canonical planned sleep duration derived only from bedtime and wake target. */
    fun plannedDurationMinutes(sleepMinutes: Int, wakeMinutes: Int): Int =
        durationBetween(sleepMinutes, wakeMinutes)

    fun validateConfiguredWindow(sleepMinutes: Int, wakeMinutes: Int): Int {
        val duration = durationBetween(sleepMinutes, wakeMinutes)
        require(duration in 180..900) { "Planned sleep window must be between 3 and 15 hours" }
        return duration
    }

    fun circularOffsetMinutes(a: Int, b: Int): Int {
        require(a in 0..1439 && b in 0..1439)
        val raw = abs(a - b)
        return minOf(raw, 1440 - raw)
    }
}

object ScheduleConsistencyCalculator {
    fun averageOffsetMinutes(recentSleepStartMinutes: List<Int>): Int {
        if (recentSleepStartMinutes.size < 2) return 0
        val values = recentSleepStartMinutes.take(7)
        var total = 0
        var pairs = 0
        for (i in 1 until values.size) {
            total += SleepSchedule.circularOffsetMinutes(values[i - 1], values[i])
            pairs++
        }
        return if (pairs == 0) 0 else total / pairs
    }
}
