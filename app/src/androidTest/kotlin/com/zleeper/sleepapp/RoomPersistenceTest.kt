package com.zleeper.sleepapp

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zleeper.sleepapp.core.time.AppClock
import com.zleeper.sleepapp.data.local.database.ExpeditionEntity
import com.zleeper.sleepapp.data.local.database.InventoryStackEntity
import com.zleeper.sleepapp.data.local.database.SleepSessionEntity
import com.zleeper.sleepapp.data.local.database.SleepSignalEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.data.repository.RoomSleepSessionRepository
import com.zleeper.sleepapp.domain.sleep.SleepResolver
import com.zleeper.sleepapp.domain.sleep.SleepSessionState
import com.zleeper.sleepapp.domain.sleep.SleepSignalType
import com.zleeper.sleepapp.platform.sleep.SleepSignalIdentity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPersistenceTest {
    private lateinit var database: ZleeperDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ZleeperDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun beginPersistsArmedStateBeforeTrackingPromotion() = runBlocking {
        val repository = repositoryAt(1_000L)

        val session = repository.begin(22 * 60, 7 * 60, windDownCompleted = false)

        assertEquals(SleepSessionState.ARMED, session.state)
        assertEquals(SleepSessionState.ARMED.name, database.sleepDao().session(session.id)?.state)
        assertEquals(1, database.sleepDao().signals(session.id).count { it.type == SleepSignalType.MANUAL_START.name })
    }

    @Test
    fun armedSessionIsEligibleForSignalRegistration() = runBlocking {
        database.sleepDao().insertSession(trackingSession("night").copy(state = SleepSessionState.ARMED.name))

        assertEquals(SleepSessionState.ARMED.name, database.sleepDao().trackingSession()?.state)
    }

    @Test
    fun wakeAndReviewStatesAreNotEligibleForSleepCallbacks() = runBlocking {
        val wakePending = trackingSession("wake").copy(
            state = SleepSessionState.WAKE_PENDING.name,
            sessionEndEpochMs = 2_000L,
        )
        database.sleepDao().insertSession(wakePending)

        assertNull(database.sleepDao().trackingSession())

        database.sleepDao().updateSession(wakePending.copy(state = SleepSessionState.REVIEW_PENDING.name))
        assertNull(database.sleepDao().trackingSession())
    }

    @Test
    fun wakePendingRetryReusesPersistedWakeAnchor() = runBlocking {
        val original = trackingSession("night")
        database.sleepDao().insertSession(original)
        database.sleepDao().updateSession(
            original.copy(
                state = SleepSessionState.WAKE_PENDING.name,
                sessionEndEpochMs = 2_000L,
            ),
        )
        val repository = repositoryAt(3_000L)

        val resolved = repository.requestWake("night", atEpochMs = 9_000L)

        assertEquals(2_000L, resolved.sessionEndEpochMs)
        val persisted = database.sleepDao().session("night")!!
        assertEquals(SleepSessionState.REVIEW_PENDING.name, persisted.state)
        assertEquals(2_000L, persisted.sessionEndEpochMs)
        assertEquals(1, database.sleepDao().signals("night").count { it.type == SleepSignalType.MANUAL_WAKE.name })
    }

    @Test
    fun duplicateSleepCallbackCollapsesToOneSignalRow() = runBlocking {
        val session = trackingSession("night")
        database.sleepDao().insertSession(session)
        val signalId = SleepSignalIdentity.id("night", SleepSignalType.CLASSIFY, 2_000L, null, 91)
        val first = SleepSignalEntity(signalId, "night", "CLASSIFY", 2_000L, null, 91, 3_000L)
        val duplicate = first.copy(createdAtEpochMs = 4_000L)

        database.sleepDao().insertSignals(listOf(first))
        database.sleepDao().insertSignals(listOf(duplicate))

        val signals = database.sleepDao().signals("night")
        assertEquals(1, signals.size)
        assertEquals(signalId, signals.single().id)
    }

    @Test
    fun databaseAllowsOnlyOneExpeditionPerSleepSession() = runBlocking {
        database.sleepDao().insertSession(trackingSession("night"))
        database.expeditionDao().insert(expedition("expedition-a", "night"))

        val duplicateSessionInsert = runCatching {
            database.expeditionDao().insert(expedition("expedition-b", "night"))
        }

        assertTrue(duplicateSessionInsert.isFailure)
        assertEquals("expedition-a", database.expeditionDao().forSession("night")?.id)
    }

    @Test
    fun failedTransactionRollsBackAllWrites() = runBlocking {
        val result = runCatching {
            database.withTransaction {
                database.inventoryDao().putStack(InventoryStackEntity("item_material_test", 4, 1_000L))
                error("force rollback")
            }
        }

        assertTrue(result.isFailure)
        assertNull(database.inventoryDao().stack("item_material_test"))
    }

    private fun repositoryAt(now: Long) = RoomSleepSessionRepository(
        database.sleepDao(),
        SleepResolver(),
        AppClock { now },
    )

    private fun trackingSession(id: String) = SleepSessionEntity(
        id = id,
        state = SleepSessionState.TRACKING.name,
        sessionStartEpochMs = 1_000L,
        sessionEndEpochMs = null,
        estimatedSleepStartEpochMs = null,
        estimatedSleepEndEpochMs = null,
        targetSleepMinutes = 22 * 60,
        targetWakeMinutes = 7 * 60,
        estimatedSleepMinutes = null,
        timingOffsetMinutes = null,
        windDownCompleted = false,
        resolutionMethod = null,
        confidence = null,
        expeditionSeed = 42L,
        finalizedAtEpochMs = null,
    )

    private fun expedition(id: String, sessionId: String) = ExpeditionEntity(
        id = id,
        sleepSessionId = sessionId,
        regionId = "region_test",
        seed = 42L,
        status = "RESOLVING",
        reachBand = null,
        contentPackVersion = 2,
        progressionRulesVersion = 2,
        startedAtEpochMs = 1_000L,
        resolvedAtEpochMs = null,
    )
}
