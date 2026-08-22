package com.zleeper.sleepapp

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.data.local.database.ZleeperMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ZleeperDatabase::class.java,
    )

    @Test
    fun migrationPreservesLegacyProgressAndCreatesV2State() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO pet (
                    instanceId, speciesId, displayName, formId, level, totalXp,
                    energy, focus, resilience, energyAffinity, focusAffinity, resilienceAffinity,
                    createdAtEpochMs, updatedAtEpochMs
                ) VALUES ('pet', 'species', 'Lumi', 'form', 5, 900, 3, 4, 5, 6, 7, 8, 1000, 2000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO inventory_instance (instanceId, itemId, acquiredAtEpochMs, equippedSlot)
                VALUES ('gear', 'item_equipment_test', 1234, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            ZleeperMigrations.MIGRATION_1_2,
        )

        migrated.query(
            "SELECT level, totalXp, dreamSparksAvailable, dreamSparksEarned FROM pet WHERE instanceId = 'pet'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(5, cursor.getInt(0))
            assertEquals(900L, cursor.getLong(1))
            assertEquals(4, cursor.getInt(2))
            assertEquals(4, cursor.getInt(3))
        }
        migrated.query(
            "SELECT tier, upgradeLevel, quality, progressionSeed FROM inventory_instance WHERE instanceId = 'gear'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(75, cursor.getInt(2))
            assertEquals(1234L, cursor.getLong(3))
        }
        val v2Tables = setOf(
            "pet_specialization",
            "pet_memory",
            "equipment_progression_event",
            "world_scene_completion",
            "hearth_progress",
            "hearth_progress_event",
            "player_title",
        )
        migrated.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            val names = buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            assertTrue(names.containsAll(v2Tables))
        }
    }

    private companion object {
        const val TEST_DB = "migration-1-2"
    }
}
