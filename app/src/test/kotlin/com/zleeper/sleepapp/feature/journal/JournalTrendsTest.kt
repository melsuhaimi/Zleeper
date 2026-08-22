package com.zleeper.sleepapp.feature.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JournalTrendsTest {
    @Test fun waitsForThreeNightsBeforeSummarizing() {
        assertNull(JournalTrends.from(listOf(night(420, 10), night(480, -20))))
    }

    @Test fun derivesNeutralDurationAndTimingPatternsFromResolvedNights() {
        val trends = requireNotNull(JournalTrends.from(listOf(night(420, 10), night(480, -20), night(540, 30))))
        assertEquals(480, trends.averageDurationMinutes)
        assertEquals(120, trends.durationRangeMinutes)
        assertEquals(20, trends.averageTimingDistanceMinutes)
        assertEquals(listOf(420, 480, 540), trends.durationSeries)
        assertEquals(listOf(10, 20, 30), trends.timingDistanceSeries)
    }

    private fun night(duration: Int, offset: Int) = JournalNight(
        sessionId = "$duration:$offset",
        startedAtEpochMs = 0L,
        estimatedSleepStartEpochMs = 0L,
        estimatedSleepEndEpochMs = 0L,
        estimatedSleepMinutes = duration,
        targetSleepMinutes = 0,
        targetWakeMinutes = 0,
        timingOffsetMinutes = offset,
        confidence = "MEDIUM",
        resolutionMethod = "MANUAL",
        windDownCompleted = false,
        progressionQuality = null,
        xpGranted = null,
        reachBand = null,
        regionId = null,
        morningMood = null,
        morningNote = null,
        journey = emptyList(),
        rewards = emptyList(),
    )
}
