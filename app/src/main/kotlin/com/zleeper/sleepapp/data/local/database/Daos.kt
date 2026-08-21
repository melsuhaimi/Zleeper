package com.zleeper.sleepapp.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSession(value: SleepSessionEntity)
    @Update suspend fun updateSession(value: SleepSessionEntity)
    @Query("SELECT * FROM sleep_session WHERE id = :id") suspend fun session(id: String): SleepSessionEntity?
    @Query("SELECT * FROM sleep_session ORDER BY sessionStartEpochMs DESC") fun sessions(): Flow<List<SleepSessionEntity>>
    @Query("SELECT * FROM sleep_session WHERE state NOT IN ('FINALIZED','EXPEDITION_RESOLVED','ABORTED') ORDER BY sessionStartEpochMs DESC LIMIT 1") suspend fun activeSession(): SleepSessionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSignals(values: List<SleepSignalEntity>)
    @Query("SELECT * FROM sleep_signal WHERE sessionId = :sessionId ORDER BY occurredAtEpochMs") suspend fun signals(sessionId: String): List<SleepSignalEntity>
    @Query("DELETE FROM sleep_signal WHERE createdAtEpochMs < :cutoffEpochMs") suspend fun deleteSignalsOlderThan(cutoffEpochMs: Long): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertOutcome(value: NightOutcomeEntity)
    @Query("SELECT * FROM night_outcome WHERE sleepSessionId = :sessionId") suspend fun outcome(sessionId: String): NightOutcomeEntity?
}

@Dao interface ExpeditionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(value: ExpeditionEntity)
    @Update suspend fun update(value: ExpeditionEntity)
    @Query("SELECT * FROM expedition WHERE id = :id") suspend fun expedition(id: String): ExpeditionEntity?
    @Query("SELECT * FROM expedition WHERE sleepSessionId = :sessionId") suspend fun forSession(sessionId: String): ExpeditionEntity?
    @Query("SELECT * FROM expedition ORDER BY startedAtEpochMs DESC") fun expeditions(): Flow<List<ExpeditionEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPath(values: List<ExpeditionPathNodeEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRewards(values: List<ExpeditionRewardEntity>)
    @Query("SELECT * FROM expedition_path_node WHERE expeditionId = :id ORDER BY sequence") suspend fun path(id: String): List<ExpeditionPathNodeEntity>
    @Query("SELECT * FROM expedition_reward WHERE expeditionId = :id") suspend fun rewards(id: String): List<ExpeditionRewardEntity>
}

@Dao interface PetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(value: PetEntity)
    @Update suspend fun update(value: PetEntity)
    @Query("SELECT * FROM pet LIMIT 1") fun observePet(): Flow<PetEntity?>
    @Query("SELECT * FROM pet LIMIT 1") suspend fun pet(): PetEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEvents(values: List<PetProgressionEventEntity>)
}

@Dao interface InventoryDao {
    @Query("SELECT * FROM inventory_stack ORDER BY itemId") fun stacks(): Flow<List<InventoryStackEntity>>
    @Query("SELECT * FROM inventory_instance ORDER BY acquiredAtEpochMs DESC") fun instances(): Flow<List<InventoryInstanceEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putStack(value: InventoryStackEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertInstances(values: List<InventoryInstanceEntity>)
    @Update suspend fun updateInstance(value: InventoryInstanceEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTransactions(values: List<InventoryTransactionEntity>)
    @Query("SELECT * FROM inventory_stack WHERE itemId = :itemId") suspend fun stack(itemId: String): InventoryStackEntity?
    @Query("SELECT * FROM inventory_instance WHERE instanceId = :id") suspend fun instance(id: String): InventoryInstanceEntity?
    @Query("SELECT * FROM equipment_slot ORDER BY slot") fun equipment(): Flow<List<EquipmentSlotEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEquipment(value: EquipmentSlotEntity)
}

@Dao interface QuestDao {
    @Query("SELECT * FROM quest_progress ORDER BY acceptedAtEpochMs") fun quests(): Flow<List<QuestProgressEntity>>
    @Query("SELECT * FROM quest_progress WHERE questId = :id") suspend fun quest(id: String): QuestProgressEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putQuest(value: QuestProgressEntity)
    @Query("SELECT * FROM quest_objective_progress WHERE questId = :id") suspend fun objectives(id: String): List<QuestObjectiveProgressEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putObjectives(values: List<QuestObjectiveProgressEntity>)
}

@Dao interface WorldDao {
    @Query("SELECT * FROM world_unlock ORDER BY unlockedAtEpochMs") fun unlocks(): Flow<List<WorldUnlockEntity>>
    @Query("SELECT * FROM world_discovery ORDER BY discoveredAtEpochMs") fun discoveries(): Flow<List<WorldDiscoveryEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun unlock(value: WorldUnlockEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun discover(value: WorldDiscoveryEntity): Long
    @Query("SELECT * FROM collection_entry ORDER BY category, entryId") fun collection(): Flow<List<CollectionEntryEntity>>
    @Query("SELECT * FROM collection_entry WHERE entryId = :id") suspend fun collectionEntry(id: String): CollectionEntryEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCollection(value: CollectionEntryEntity)
}

@Dao interface MorningDao {
    @Query("SELECT * FROM morning_note WHERE sleepSessionId = :sessionId") suspend fun note(sessionId: String): MorningNoteEntity?
    @Query("SELECT * FROM morning_note ORDER BY createdAtEpochMs DESC") fun notes(): Flow<List<MorningNoteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(value: MorningNoteEntity)
}
