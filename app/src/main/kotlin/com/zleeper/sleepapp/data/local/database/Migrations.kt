package com.zleeper.sleepapp.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ZleeperMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE pet ADD COLUMN dreamSparksAvailable INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE pet ADD COLUMN dreamSparksEarned INTEGER NOT NULL DEFAULT 0")
            database.execSQL("UPDATE pet SET dreamSparksAvailable = CASE WHEN level > 1 THEN level - 1 ELSE 0 END, dreamSparksEarned = CASE WHEN level > 1 THEN level - 1 ELSE 0 END")

            database.execSQL("ALTER TABLE inventory_instance ADD COLUMN tier INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE inventory_instance ADD COLUMN upgradeLevel INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE inventory_instance ADD COLUMN quality INTEGER NOT NULL DEFAULT 75")
            database.execSQL("ALTER TABLE inventory_instance ADD COLUMN traitId TEXT")
            database.execSQL("ALTER TABLE inventory_instance ADD COLUMN infusionId TEXT")
            database.execSQL("ALTER TABLE inventory_instance ADD COLUMN progressionSeed INTEGER NOT NULL DEFAULT 0")
            database.execSQL("UPDATE inventory_instance SET progressionSeed = acquiredAtEpochMs WHERE progressionSeed = 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_instance_tier` ON `inventory_instance` (`tier`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_instance_traitId` ON `inventory_instance` (`traitId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_instance_infusionId` ON `inventory_instance` (`infusionId`)")

            database.execSQL("ALTER TABLE quest_progress ADD COLUMN tracked INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE quest_progress ADD COLUMN abandonedAtEpochMs INTEGER")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_progress_tracked` ON `quest_progress` (`tracked`)")

            database.execSQL("CREATE INDEX IF NOT EXISTS `index_world_discovery_sourceId` ON `world_discovery` (`sourceId`)")

            database.execSQL("CREATE TABLE IF NOT EXISTS `pet_specialization` (`petId` TEXT NOT NULL, `nodeId` TEXT NOT NULL, `rank` INTEGER NOT NULL, `unlockedAtEpochMs` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`petId`, `nodeId`))")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_pet_specialization_nodeId` ON `pet_specialization` (`nodeId`)")

            database.execSQL("CREATE TABLE IF NOT EXISTS `pet_memory` (`id` TEXT NOT NULL, `petId` TEXT NOT NULL, `memoryType` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `subjectId` TEXT, `thoughtKey` TEXT NOT NULL, `detail` TEXT, `occurredAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_pet_memory_petId` ON `pet_memory` (`petId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_pet_memory_occurredAtEpochMs` ON `pet_memory` (`occurredAtEpochMs`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pet_memory_memoryType_sourceId` ON `pet_memory` (`memoryType`, `sourceId`)")

            database.execSQL("CREATE TABLE IF NOT EXISTS `equipment_progression_event` (`id` TEXT NOT NULL, `inventoryInstanceId` TEXT NOT NULL, `action` TEXT NOT NULL, `fromTier` INTEGER NOT NULL, `fromUpgradeLevel` INTEGER NOT NULL, `toTier` INTEGER NOT NULL, `toUpgradeLevel` INTEGER NOT NULL, `sourceId` TEXT NOT NULL, `occurredAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_progression_event_inventoryInstanceId` ON `equipment_progression_event` (`inventoryInstanceId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_progression_event_occurredAtEpochMs` ON `equipment_progression_event` (`occurredAtEpochMs`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_equipment_progression_event_action` ON `equipment_progression_event` (`action`)")

            database.execSQL("CREATE TABLE IF NOT EXISTS `world_scene_completion` (`sceneId` TEXT NOT NULL, `regionId` TEXT NOT NULL, `completionCount` INTEGER NOT NULL, `firstCompletedAtEpochMs` INTEGER NOT NULL, `lastCompletedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`sceneId`))")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_world_scene_completion_regionId` ON `world_scene_completion` (`regionId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_world_scene_completion_lastCompletedAtEpochMs` ON `world_scene_completion` (`lastCompletedAtEpochMs`)")

            database.execSQL("CREATE TABLE IF NOT EXISTS `hearth_progress` (`id` TEXT NOT NULL, `memories` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `hearth_progress_event` (`id` TEXT NOT NULL, `amount` INTEGER NOT NULL, `reason` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `occurredAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_hearth_progress_event_occurredAtEpochMs` ON `hearth_progress_event` (`occurredAtEpochMs`)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_hearth_progress_event_reason_sourceId` ON `hearth_progress_event` (`reason`, `sourceId`)")

            database.execSQL("CREATE TABLE IF NOT EXISTS `player_title` (`titleId` TEXT NOT NULL, `unlockedAtEpochMs` INTEGER NOT NULL, `equipped` INTEGER NOT NULL DEFAULT 0, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`titleId`))")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_player_title_equipped` ON `player_title` (`equipped`)")

            database.execSQL(
                """
                INSERT OR REPLACE INTO hearth_progress (id, memories, updatedAtEpochMs)
                SELECT 'hearth',
                       CASE WHEN historyMemories > levelMemories THEN historyMemories ELSE levelMemories END,
                       nowValue
                FROM (
                    SELECT
                      (SELECT COUNT(*) FROM sleep_session WHERE state IN ('FINALIZED','EXPEDITION_RESOLVED')) +
                      (SELECT COUNT(*) FROM world_discovery) AS historyMemories,
                      CASE WHEN COALESCE((SELECT level FROM pet LIMIT 1), 1) > 1 THEN COALESCE((SELECT level FROM pet LIMIT 1), 1) - 1 ELSE 0 END AS levelMemories,
                      COALESCE((SELECT MAX(updatedAtEpochMs) FROM pet), 0) AS nowValue
                )
                """.trimIndent(),
            )
        }
    }
}
