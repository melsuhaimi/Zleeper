package com.zleeper.sleepapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.PetEntity
import com.zleeper.sleepapp.data.local.database.SleepSessionEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.data.repository.GameEffectsService
import com.zleeper.sleepapp.data.repository.NightResolutionService
import com.zleeper.sleepapp.data.repository.PetMemoryService
import com.zleeper.sleepapp.data.repository.QuestProgressService
import com.zleeper.sleepapp.data.repository.TitleService
import com.zleeper.sleepapp.data.repository.WorldProgressService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RewardIdempotencyTest {
    private lateinit var database: ZleeperDatabase
    private lateinit var resolver: NightResolutionService

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, ZleeperDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val content = GameContentRepository(context)
        val petDao = database.petDao()
        val inventoryDao = database.inventoryDao()
        val worldDao = database.worldDao()
        val memoryService = PetMemoryService(petDao)
        val worldProgressService = WorldProgressService(database, worldDao, content, memoryService)
        val questProgressService = QuestProgressService(
            database,
            database.questDao(),
            petDao,
            inventoryDao,
            content,
            worldProgressService,
            memoryService,
        )
        val titleService = TitleService(
            database,
            database.expeditionDao(),
            inventoryDao,
            worldDao,
            content,
            memoryService,
        )
        resolver = NightResolutionService(
            database,
            database.sleepDao(),
            database.expeditionDao(),
            petDao,
            inventoryDao,
            worldDao,
            content,
            GameEffectsService(inventoryDao, petDao, content),
            questProgressService,
            worldProgressService,
            memoryService,
            titleService,
        )

        val species = content.species.first()
        val form = content.forms.first { it.id == species.initialFormId }
        petDao.insert(
            PetEntity(
                instanceId = "pet",
                speciesId = species.id,
                displayName = "Lumi",
                formId = form.id,
                level = 1,
                totalXp = 0,
                energy = 3,
                focus = 3,
                resilience = 3,
                energyAffinity = 0,
                focusAffinity = 0,
                resilienceAffinity = 0,
                createdAtEpochMs = 1_000L,
                updatedAtEpochMs = 1_000L,
            ),
        )
        database.sleepDao().insertSession(finalizedNight())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun duplicateMorningResolutionReturnsPersistedResultWithoutGrantingAgain() = runBlocking {
        val first = resolver.resolve("night", reflected = false)
        val firstXp = database.petDao().pet()!!.totalXp
        val firstInventoryLedgerCount = countRows("inventory_transaction")
        val firstPetLedgerCount = countRows("pet_progression_event")
        val firstRewardCount = countRows("expedition_reward")

        val second = resolver.resolve("night", reflected = false)

        assertEquals(first, second)
        assertEquals(firstXp, database.petDao().pet()!!.totalXp)
        assertEquals(firstInventoryLedgerCount, countRows("inventory_transaction"))
        assertEquals(firstPetLedgerCount, countRows("pet_progression_event"))
        assertEquals(firstRewardCount, countRows("expedition_reward"))
        assertEquals(1, database.expeditionDao().resolvedCount())
    }

    private fun countRows(table: String): Int = database.openHelper.readableDatabase
        .query("SELECT COUNT(*) FROM $table")
        .use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun finalizedNight() = SleepSessionEntity(
        id = "night",
        state = "FINALIZED",
        sessionStartEpochMs = 1_700_000_000_000L,
        sessionEndEpochMs = 1_700_028_800_000L,
        estimatedSleepStartEpochMs = 1_700_000_000_000L,
        estimatedSleepEndEpochMs = 1_700_028_800_000L,
        targetSleepMinutes = 22 * 60,
        targetWakeMinutes = 6 * 60,
        estimatedSleepMinutes = 480,
        timingOffsetMinutes = 0,
        windDownCompleted = true,
        resolutionMethod = "MANUAL",
        confidence = "LOW",
        expeditionSeed = 42L,
        finalizedAtEpochMs = 1_700_028_900_000L,
    )
}
