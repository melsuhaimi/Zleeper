package com.zleeper.sleepapp.feature.shell

import com.zleeper.sleepapp.data.local.database.ExpeditionEntity
import com.zleeper.sleepapp.data.local.database.SleepSessionEntity
import com.zleeper.sleepapp.data.local.preferences.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZleeperUiStateTest {
    @Test
    fun finalizedSessionRemainsPendingUntilResolutionCompletes() {
        val session = sleepSession("session-1", "FINALIZED")

        assertEquals(session, ZleeperUiState(sessions = listOf(session)).pendingResolution)
    }

    @Test
    fun latestResolvedExpeditionIsRevealedOnlyUntilAcknowledged() {
        val latest = expedition("expedition-2", "session-2", 2L)
        val older = expedition("expedition-1", "session-1", 1L)

        assertEquals(latest, ZleeperUiState(expeditions = listOf(latest, older)).pendingReveal)
        assertNull(
            ZleeperUiState(
                settings = AppSettings(lastRevealedExpeditionId = latest.id),
                expeditions = listOf(latest, older),
            ).pendingReveal,
        )
    }

    private fun sleepSession(id: String, state: String) = SleepSessionEntity(
        id = id,
        state = state,
        sessionStartEpochMs = 1L,
        sessionEndEpochMs = 2L,
        estimatedSleepStartEpochMs = 1L,
        estimatedSleepEndEpochMs = 2L,
        targetSleepMinutes = 1_320,
        targetWakeMinutes = 420,
        estimatedSleepMinutes = 1,
        timingOffsetMinutes = 0,
        windDownCompleted = true,
        resolutionMethod = "MANUAL",
        confidence = "LOW",
        expeditionSeed = 7L,
        finalizedAtEpochMs = 2L,
    )

    private fun expedition(id: String, sessionId: String, startedAt: Long) = ExpeditionEntity(
        id = id,
        sleepSessionId = sessionId,
        regionId = "region_whispering_grove",
        seed = 7L,
        status = "RESOLVED",
        reachBand = 1,
        contentPackVersion = 1,
        progressionRulesVersion = 1,
        startedAtEpochMs = startedAt,
        resolvedAtEpochMs = startedAt + 1,
    )
}
