package com.zleeper.sleepapp.domain.sleep

import javax.inject.Inject
import kotlin.math.abs

class SleepResolver @Inject constructor() {
    fun resolve(session: SleepSession, signals: List<SleepSignal>, wakeEpochMs: Long, finalizedAtEpochMs: Long): ResolvedSleepSession {
        require(wakeEpochMs > session.sessionStartEpochMs) { "Wake must follow session start" }
        val segment = signals.lastOrNull { it.type == SleepSignalType.SEGMENT && it.durationMillis != null }
        val classifications = signals.filter { it.type == SleepSignalType.CLASSIFY && (it.confidencePercent ?: 0) >= 50 }
        val method = when {
            segment != null && classifications.isNotEmpty() -> SleepResolutionMethod.SEGMENT_AND_CLASSIFY
            classifications.isNotEmpty() -> SleepResolutionMethod.CLASSIFY
            segment != null -> SleepResolutionMethod.SEGMENT
            else -> SleepResolutionMethod.MANUAL
        }
        val start = when {
            segment != null -> (segment.occurredAtEpochMs - segment.durationMillis!!).coerceAtLeast(session.sessionStartEpochMs)
            classifications.isNotEmpty() -> classifications.first().occurredAtEpochMs.coerceAtLeast(session.sessionStartEpochMs)
            else -> session.sessionStartEpochMs
        }
        val end = when {
            segment != null -> segment.occurredAtEpochMs.coerceAtMost(wakeEpochMs)
            classifications.isNotEmpty() -> classifications.last().occurredAtEpochMs.coerceAtMost(wakeEpochMs)
            else -> wakeEpochMs
        }.coerceAtLeast(start)
        val confidence = when (method) {
            SleepResolutionMethod.SEGMENT_AND_CLASSIFY -> SleepConfidence.HIGH
            SleepResolutionMethod.SEGMENT, SleepResolutionMethod.CLASSIFY -> SleepConfidence.MEDIUM
            SleepResolutionMethod.MANUAL -> SleepConfidence.LOW
        }
        val startMinute = ((start / 60_000L) % 1440).toInt()
        val circularOffset = minOf(abs(startMinute - session.targetSleepMinutes), 1440 - abs(startMinute - session.targetSleepMinutes))
        return ResolvedSleepSession(session.id, session.sessionStartEpochMs, wakeEpochMs, start, end, session.targetSleepMinutes, session.targetWakeMinutes, ((end - start) / 60_000L).toInt(), circularOffset, session.windDownCompleted, method, confidence, finalizedAtEpochMs)
    }

    fun applyCorrection(value: ResolvedSleepSession, correction: SleepReviewCorrection, finalizedAtEpochMs: Long): ResolvedSleepSession {
        require(correction.sleepEndEpochMs >= correction.sleepStartEpochMs)
        require(correction.sleepStartEpochMs >= value.sessionStartEpochMs && correction.sleepEndEpochMs <= value.sessionEndEpochMs)
        val startMinute = ((correction.sleepStartEpochMs / 60_000L) % 1440).toInt()
        val difference = abs(startMinute - value.targetSleepMinutes)
        return value.copy(estimatedSleepStartEpochMs = correction.sleepStartEpochMs, estimatedSleepEndEpochMs = correction.sleepEndEpochMs, estimatedSleepMinutes = ((correction.sleepEndEpochMs - correction.sleepStartEpochMs) / 60_000L).toInt(), timingOffsetMinutes = minOf(difference, 1440 - difference), resolutionMethod = SleepResolutionMethod.MANUAL, confidence = SleepConfidence.HIGH, finalizedAtEpochMs = finalizedAtEpochMs)
    }
}
