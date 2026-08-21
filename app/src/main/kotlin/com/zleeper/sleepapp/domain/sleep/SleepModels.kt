package com.zleeper.sleepapp.domain.sleep

enum class SleepSessionState { IDLE, ARMED, TRACKING, WAKE_PENDING, REVIEW_PENDING, FINALIZED, EXPEDITION_RESOLVED, ABORTED }
enum class SleepSignalType { CLASSIFY, SEGMENT, MANUAL_START, MANUAL_WAKE, ALARM }
enum class SleepConfidence { HIGH, MEDIUM, LOW }
enum class SleepResolutionMethod { SEGMENT_AND_CLASSIFY, CLASSIFY, SEGMENT, MANUAL }

data class SleepSignal(val id: String, val sessionId: String, val type: SleepSignalType, val occurredAtEpochMs: Long, val durationMillis: Long? = null, val confidencePercent: Int? = null)

data class SleepSession(
    val id: String,
    val state: SleepSessionState,
    val sessionStartEpochMs: Long,
    val sessionEndEpochMs: Long?,
    val targetSleepMinutes: Int,
    val targetWakeMinutes: Int,
    val windDownCompleted: Boolean,
    val expeditionSeed: Long,
)

data class ResolvedSleepSession(
    val id: String,
    val sessionStartEpochMs: Long,
    val sessionEndEpochMs: Long,
    val estimatedSleepStartEpochMs: Long,
    val estimatedSleepEndEpochMs: Long,
    val targetSleepMinutes: Int,
    val targetWakeMinutes: Int,
    val estimatedSleepMinutes: Int,
    val timingOffsetMinutes: Int,
    val windDownCompleted: Boolean,
    val resolutionMethod: SleepResolutionMethod,
    val confidence: SleepConfidence,
    val finalizedAtEpochMs: Long,
)

data class SleepReviewCorrection(val sleepStartEpochMs: Long, val sleepEndEpochMs: Long)

interface SleepSessionRepository {
    suspend fun begin(targetSleepMinutes: Int, targetWakeMinutes: Int, windDownCompleted: Boolean): SleepSession
    suspend fun recordSignals(signals: List<SleepSignal>)
    suspend fun requestWake(sessionId: String, atEpochMs: Long): ResolvedSleepSession
    suspend fun finalize(sessionId: String, correction: SleepReviewCorrection?): ResolvedSleepSession
    suspend fun abort(sessionId: String)
}
