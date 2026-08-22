package com.zleeper.sleepapp.feature.journal

import kotlin.math.abs

data class JournalJourneyStep(
    val sequence: Int,
    val nodeType: String,
    val outcomeTextKey: String,
    val narrative: String,
)

data class JournalReward(
    val rewardType: String,
    val contentId: String?,
    val quantity: Int,
)

data class JournalNight(
    val sessionId: String,
    val startedAtEpochMs: Long,
    val estimatedSleepStartEpochMs: Long?,
    val estimatedSleepEndEpochMs: Long?,
    val estimatedSleepMinutes: Int,
    val targetSleepMinutes: Int,
    val targetWakeMinutes: Int,
    val timingOffsetMinutes: Int,
    val confidence: String,
    val resolutionMethod: String,
    val windDownCompleted: Boolean,
    val progressionQuality: Int?,
    val xpGranted: Int?,
    val reachBand: Int?,
    val regionId: String?,
    val morningMood: Int?,
    val morningNote: String?,
    val journey: List<JournalJourneyStep>,
    val rewards: List<JournalReward>,
)

data class JournalTrends(
    val sampleSize: Int,
    val averageDurationMinutes: Int,
    val durationRangeMinutes: Int,
    val averageTimingDistanceMinutes: Int,
    val durationSeries: List<Int>,
    val timingDistanceSeries: List<Int>,
) {
    companion object {
        fun from(nights: List<JournalNight>, maximumNights: Int = 7): JournalTrends? {
            val recent = nights.take(maximumNights)
            if (recent.size < 3) return null
            val durations = recent.map { it.estimatedSleepMinutes }
            val timing = recent.map { abs(it.timingOffsetMinutes) }
            return JournalTrends(
                sampleSize = recent.size,
                averageDurationMinutes = durations.average().toInt(),
                durationRangeMinutes = (durations.maxOrNull() ?: 0) - (durations.minOrNull() ?: 0),
                averageTimingDistanceMinutes = timing.average().toInt(),
                durationSeries = durations,
                timingDistanceSeries = timing,
            )
        }
    }
}
