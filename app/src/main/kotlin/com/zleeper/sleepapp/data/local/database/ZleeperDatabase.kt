package com.zleeper.sleepapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SleepSessionEntity::class, SleepSignalEntity::class, NightOutcomeEntity::class, ExpeditionEntity::class, ExpeditionPathNodeEntity::class, ExpeditionRewardEntity::class, PetEntity::class, PetProgressionEventEntity::class, InventoryStackEntity::class, InventoryInstanceEntity::class, InventoryTransactionEntity::class, EquipmentSlotEntity::class, QuestProgressEntity::class, QuestObjectiveProgressEntity::class, WorldUnlockEntity::class, WorldDiscoveryEntity::class, CollectionEntryEntity::class, MorningNoteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ZleeperDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun expeditionDao(): ExpeditionDao
    abstract fun petDao(): PetDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun questDao(): QuestDao
    abstract fun worldDao(): WorldDao
    abstract fun morningDao(): MorningDao
}
