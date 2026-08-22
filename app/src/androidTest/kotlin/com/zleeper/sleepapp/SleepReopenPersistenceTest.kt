package com.zleeper.sleepapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zleeper.sleepapp.data.local.database.SleepSessionEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.domain.sleep.SleepSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepReopenPersistenceTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun trackingSessionAndExpeditionSeedSurviveDatabaseReopen() = runBlocking {
        withFreshDatabase(TEST_TRACKING_DB) { first ->
            first.sleepDao().insertSession(session("tracking", SleepSessionState.TRACKING, endAt = null, seed = 9_876_543L))
        }

        val reopened = open(TEST_TRACKING_DB)
        try {
            val restored = reopened.sleepDao().activeSession()
            assertNotNull(restored)
            assertEquals(SleepSessionState.TRACKING.name, restored!!.state)
            assertEquals(9_876_543L, restored.expeditionSeed)
            assertEquals(1_000L, restored.sessionStartEpochMs)
        } finally {
            reopened.close()
            context.deleteDatabase(TEST_TRACKING_DB)
        }
    }

    @Test
    fun wakePendingAnchorSurvivesDatabaseReopen() = runBlocking {
        withFreshDatabase(TEST_WAKE_DB) { first ->
            first.sleepDao().insertSession(session("wake", SleepSessionState.WAKE_PENDING, endAt = 8_000L, seed = 55L))
        }

        val reopened = open(TEST_WAKE_DB)
        try {
            val restored = reopened.sleepDao().activeSession()
            assertNotNull(restored)
            assertEquals(SleepSessionState.WAKE_PENDING.name, restored!!.state)
            assertEquals(8_000L, restored.sessionEndEpochMs)
            assertEquals(55L, restored.expeditionSeed)
        } finally {
            reopened.close()
            context.deleteDatabase(TEST_WAKE_DB)
        }
    }

    private suspend fun withFreshDatabase(name: String, block: suspend (ZleeperDatabase) -> Unit) {
        context.deleteDatabase(name)
        val database = open(name)
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun open(name: String): ZleeperDatabase = Room.databaseBuilder(context, ZleeperDatabase::class.java, name)
        .allowMainThreadQueries()
        .build()

    private fun session(id: String, state: SleepSessionState, endAt: Long?, seed: Long) = SleepSessionEntity(
        id = id,
        state = state.name,
        sessionStartEpochMs = 1_000L,
        sessionEndEpochMs = endAt,
        estimatedSleepStartEpochMs = null,
        estimatedSleepEndEpochMs = null,
        targetSleepMinutes = 22 * 60,
        targetWakeMinutes = 7 * 60,
        estimatedSleepMinutes = null,
        timingOffsetMinutes = null,
        windDownCompleted = false,
        resolutionMethod = null,
        confidence = null,
        expeditionSeed = seed,
        finalizedAtEpochMs = null,
    )

    private companion object {
        const val TEST_TRACKING_DB = "sleep-reopen-tracking"
        const val TEST_WAKE_DB = "sleep-reopen-wake"
    }
}
