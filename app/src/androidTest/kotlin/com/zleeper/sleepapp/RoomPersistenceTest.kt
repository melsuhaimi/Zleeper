package com.zleeper.sleepapp

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zleeper.sleepapp.data.local.database.ExpeditionEntity
import com.zleeper.sleepapp.data.local.database.InventoryStackEntity
import com.zleeper.sleepapp.data.local.database.SleepSessionEntity
import com.zleeper.sleepapp.data.local.database.SleepSignalEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
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

    private fun trackingSession(id: String) = SleepSessionEntity(
        id = id,
        state = "TRACKING",
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
