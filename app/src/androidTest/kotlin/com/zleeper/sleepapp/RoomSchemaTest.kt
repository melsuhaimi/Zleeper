package com.zleeper.sleepapp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSchemaTest {
    @Test fun versionOneCreatesAllEighteenSourceOfTruthTables() {
        val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ZleeperDatabase::class.java).build()
        database.openHelper.writableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            val names = buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            val expected = setOf("sleep_session", "sleep_signal", "night_outcome", "expedition", "expedition_path_node", "expedition_reward", "pet", "pet_progression_event", "inventory_stack", "inventory_instance", "inventory_transaction", "equipment_slot", "quest_progress", "quest_objective_progress", "world_unlock", "world_discovery", "collection_entry", "morning_note")
            assertEquals(expected, names.intersect(expected))
        }
        database.close()
    }
}
