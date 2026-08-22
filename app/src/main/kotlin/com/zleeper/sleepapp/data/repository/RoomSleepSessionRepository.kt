package com.zleeper.sleepapp.data.repository

import com.zleeper.sleepapp.core.id.stableId
import com.zleeper.sleepapp.core.time.AppClock
import com.zleeper.sleepapp.data.local.database.SleepDao
import com.zleeper.sleepapp.data.local.database.SleepSessionEntity
import com.zleeper.sleepapp.data.local.database.SleepSignalEntity
import com.zleeper.sleepapp.domain.sleep.ResolvedSleepSession
import com.zleeper.sleepapp.domain.sleep.SleepResolutionMethod
import com.zleeper.sleepapp.domain.sleep.SleepResolver
import com.zleeper.sleepapp.domain.sleep.SleepReviewCorrection
import com.zleeper.sleepapp.domain.sleep.SleepSession
import com.zleeper.sleepapp.domain.sleep.SleepSessionRepository
import com.zleeper.sleepapp.domain.sleep.SleepSessionState
import com.zleeper.sleepapp.domain.sleep.SleepSignal
import com.zleeper.sleepapp.domain.sleep.SleepSignalType
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSleepSessionRepository @Inject constructor(
    private val dao: SleepDao,
    private val resolver: SleepResolver,
    private val clock: AppClock,
) : SleepSessionRepository {
    private val secureRandom = SecureRandom()

    override suspend fun begin(targetSleepMinutes: Int, targetWakeMinutes: Int, windDownCompleted: Boolean): SleepSession {
        check(dao.activeSession() == null) { "A sleep session is already active" }
        val now = clock.nowEpochMillis()
        val entity = SleepSessionEntity(
            UUID.randomUUID().toString(),
            SleepSessionState.ARMED.name,
            now,
            null,
            null,
            null,
            targetSleepMinutes,
            targetWakeMinutes,
            null,
            null,
            windDownCompleted,
            null,
            null,
            secureRandom.nextLong(),
            null,
        )
        dao.insertSession(entity)
        dao.insertSignals(
            listOf(
                SleepSignalEntity(
                    stableId("sleep-signal", entity.id, "manual-start"),
                    entity.id,
                    SleepSignalType.MANUAL_START.name,
                    now,
                    null,
                    100,
                    now,
                ),
            ),
        )
        return entity.toDomain()
    }

    override suspend fun recordSignals(signals: List<SleepSignal>) {
        val now = clock.nowEpochMillis()
        dao.insertSignals(signals.map { SleepSignalEntity(it.id, it.sessionId, it.type.name, it.occurredAtEpochMs, it.durationMillis, it.confidencePercent, now) })
    }

    override suspend fun requestWake(sessionId: String, atEpochMs: Long): ResolvedSleepSession {
        val entity = requireNotNull(dao.session(sessionId))
        require(entity.state in setOf(SleepSessionState.ARMED.name, SleepSessionState.TRACKING.name, SleepSessionState.WAKE_PENDING.name)) {
            "Sleep session is not awaiting wake resolution"
        }
        val wakeAt = if (entity.state == SleepSessionState.WAKE_PENDING.name) {
            requireNotNull(entity.sessionEndEpochMs) { "Wake-pending session is missing its wake anchor" }
        } else {
            atEpochMs
        }
        val pending = if (entity.state == SleepSessionState.WAKE_PENDING.name) {
            entity
        } else {
            val next = entity.copy(state = SleepSessionState.WAKE_PENDING.name, sessionEndEpochMs = wakeAt)
            dao.updateSession(next)
            next
        }
        val signals = dao.signals(sessionId).map {
            SleepSignal(it.id, it.sessionId, SleepSignalType.valueOf(it.type), it.occurredAtEpochMs, it.durationMillis, it.confidencePercent)
        }
        val resolved = resolver.resolve(pending.toDomain(), signals, wakeAt, clock.nowEpochMillis())
        val createdAt = clock.nowEpochMillis()
        dao.insertSignals(
            listOf(
                SleepSignalEntity(
                    stableId("sleep-signal", sessionId, "manual-wake", wakeAt.toString()),
                    sessionId,
                    SleepSignalType.MANUAL_WAKE.name,
                    wakeAt,
                    null,
                    100,
                    createdAt,
                ),
            ),
        )
        dao.updateSession(pending.fromResolved(resolved, SleepSessionState.REVIEW_PENDING))
        return resolved
    }

    override suspend fun finalize(sessionId: String, correction: SleepReviewCorrection?): ResolvedSleepSession {
        val entity = requireNotNull(dao.session(sessionId))
        require(entity.state == SleepSessionState.REVIEW_PENDING.name)
        val pending = entity.toResolved()
        val final = correction?.let { resolver.applyCorrection(pending, it, clock.nowEpochMillis()) }
            ?: pending.copy(finalizedAtEpochMs = clock.nowEpochMillis())
        dao.updateSession(entity.fromResolved(final, SleepSessionState.FINALIZED))
        return final
    }

    override suspend fun abort(sessionId: String) {
        val entity = requireNotNull(dao.session(sessionId))
        require(entity.state !in setOf(SleepSessionState.FINALIZED.name, SleepSessionState.EXPEDITION_RESOLVED.name))
        dao.updateSession(entity.copy(state = SleepSessionState.ABORTED.name, sessionEndEpochMs = clock.nowEpochMillis()))
    }

    private fun SleepSessionEntity.toDomain() = SleepSession(
        id,
        SleepSessionState.valueOf(state),
        sessionStartEpochMs,
        sessionEndEpochMs,
        targetSleepMinutes,
        targetWakeMinutes,
        windDownCompleted,
        expeditionSeed,
    )

    private fun SleepSessionEntity.toResolved() = ResolvedSleepSession(
        id,
        sessionStartEpochMs,
        requireNotNull(sessionEndEpochMs),
        requireNotNull(estimatedSleepStartEpochMs),
        requireNotNull(estimatedSleepEndEpochMs),
        targetSleepMinutes,
        targetWakeMinutes,
        requireNotNull(estimatedSleepMinutes),
        requireNotNull(timingOffsetMinutes),
        windDownCompleted,
        SleepResolutionMethod.valueOf(requireNotNull(resolutionMethod)),
        com.zleeper.sleepapp.domain.sleep.SleepConfidence.valueOf(requireNotNull(confidence)),
        requireNotNull(finalizedAtEpochMs),
    )

    private fun SleepSessionEntity.fromResolved(value: ResolvedSleepSession, state: SleepSessionState) = copy(
        state = state.name,
        sessionEndEpochMs = value.sessionEndEpochMs,
        estimatedSleepStartEpochMs = value.estimatedSleepStartEpochMs,
        estimatedSleepEndEpochMs = value.estimatedSleepEndEpochMs,
        estimatedSleepMinutes = value.estimatedSleepMinutes,
        timingOffsetMinutes = value.timingOffsetMinutes,
        resolutionMethod = value.resolutionMethod.name,
        confidence = value.confidence.name,
        finalizedAtEpochMs = value.finalizedAtEpochMs,
    )
}
