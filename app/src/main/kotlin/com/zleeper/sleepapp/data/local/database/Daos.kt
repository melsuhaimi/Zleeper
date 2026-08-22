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
    @Query("SELECT * FROM sleep_session ORDER BY sessionStartEpochMs DESC") suspend fun sessionsOnce(): List<SleepSessionEntity>
    @Query("SELECT * FROM sleep_session WHERE state IN ('FINALIZED','EXPEDITION_RESOLVED') ORDER BY sessionStartEpochMs DESC LIMIT :limit") suspend fun recentResolved(limit: Int): List<SleepSessionEntity>
    @Query("SELECT * FROM sleep_session WHERE state NOT IN ('FINALIZED','EXPEDITION_RESOLVED','ABORTED') ORDER BY sessionStartEpochMs DESC LIMIT 1") suspend fun activeSession(): SleepSessionEntity?
    @Query("SELECT * FROM sleep_session WHERE state IN ('ARMED','TRACKING') ORDER BY sessionStartEpochMs DESC LIMIT 1") suspend fun trackingSession(): SleepSessionEntity?
    @Query("SELECT * FROM sleep_session WHERE state = 'FINALIZED' ORDER BY finalizedAtEpochMs ASC") suspend fun pendingRewardResolution(): List<SleepSessionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSignals(values: List<SleepSignalEntity>)
    @Query("SELECT * FROM sleep_signal WHERE sessionId = :sessionId ORDER BY occurredAtEpochMs") suspend fun signals(sessionId: String): List<SleepSignalEntity>
    @Query("DELETE FROM sleep_signal WHERE createdAtEpochMs < :cutoffEpochMs") suspend fun deleteSignalsOlderThan(cutoffEpochMs: Long): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertOutcome(value: NightOutcomeEntity)
    @Query("SELECT * FROM night_outcome WHERE sleepSessionId = :sessionId") suspend fun outcome(sessionId: String): NightOutcomeEntity?
    @Query("SELECT * FROM night_outcome ORDER BY createdAtEpochMs DESC") fun outcomes(): Flow<List<NightOutcomeEntity>>
}

@Dao interface ExpeditionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(value: ExpeditionEntity)
    @Update suspend fun update(value: ExpeditionEntity)
    @Query("SELECT * FROM expedition WHERE id = :id") suspend fun expedition(id: String): ExpeditionEntity?
    @Query("SELECT * FROM expedition WHERE sleepSessionId = :sessionId") suspend fun forSession(sessionId: String): ExpeditionEntity?
    @Query("SELECT * FROM expedition ORDER BY startedAtEpochMs DESC") fun expeditions(): Flow<List<ExpeditionEntity>>
    @Query("SELECT COUNT(*) FROM expedition WHERE status = 'RESOLVED'") suspend fun resolvedCount(): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPath(values: List<ExpeditionPathNodeEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRewards(values: List<ExpeditionRewardEntity>)
    @Query("SELECT * FROM expedition_path_node WHERE expeditionId = :id ORDER BY sequence") suspend fun path(id: String): List<ExpeditionPathNodeEntity>
    @Query("SELECT * FROM expedition_path_node ORDER BY expeditionId, sequence") fun allPaths(): Flow<List<ExpeditionPathNodeEntity>>
    @Query("SELECT * FROM expedition_reward WHERE expeditionId = :id ORDER BY grantedAtEpochMs, id") suspend fun rewards(id: String): List<ExpeditionRewardEntity>
    @Query("SELECT * FROM expedition_reward ORDER BY expeditionId, grantedAtEpochMs, id") fun allRewards(): Flow<List<ExpeditionRewardEntity>>
}

@Dao interface PetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(value: PetEntity)
    @Update suspend fun update(value: PetEntity)
    @Query("SELECT * FROM pet LIMIT 1") fun observePet(): Flow<PetEntity?>
    @Query("SELECT * FROM pet LIMIT 1") suspend fun pet(): PetEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEvents(values: List<PetProgressionEventEntity>)
    @Query("SELECT * FROM pet_progression_event WHERE sourceId = :sourceId ORDER BY occurredAtEpochMs, id") suspend fun eventsForSource(sourceId: String): List<PetProgressionEventEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSpecialization(value: PetSpecializationEntity)
    @Query("SELECT * FROM pet_specialization WHERE petId = :petId AND nodeId = :nodeId") suspend fun specialization(petId: String, nodeId: String): PetSpecializationEntity?
    @Query("SELECT * FROM pet_specialization ORDER BY nodeId") fun specializations(): Flow<List<PetSpecializationEntity>>
    @Query("SELECT * FROM pet_specialization ORDER BY nodeId") suspend fun specializationsSnapshot(): List<PetSpecializationEntity>
    @Query("SELECT * FROM pet_specialization ORDER BY nodeId") suspend fun specializationsNow(): List<PetSpecializationEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertMemory(value: PetMemoryEntity): Long
    @Query("SELECT * FROM pet_memory ORDER BY occurredAtEpochMs DESC, id DESC") fun memories(): Flow<List<PetMemoryEntity>>
    @Query("SELECT * FROM pet_memory ORDER BY occurredAtEpochMs DESC, id DESC") suspend fun memoriesSnapshot(): List<PetMemoryEntity>
    @Query("SELECT * FROM pet_memory WHERE sourceId = :sourceId ORDER BY occurredAtEpochMs DESC, id DESC") suspend fun memoriesForSource(sourceId: String): List<PetMemoryEntity>
}

@Dao interface InventoryDao {
    @Query("SELECT * FROM inventory_stack ORDER BY itemId") fun stacks(): Flow<List<InventoryStackEntity>>
    @Query("SELECT * FROM inventory_instance ORDER BY acquiredAtEpochMs DESC") fun instances(): Flow<List<InventoryInstanceEntity>>
    @Query("SELECT * FROM inventory_transaction ORDER BY occurredAtEpochMs DESC, id DESC") fun transactions(): Flow<List<InventoryTransactionEntity>>
    @Query("SELECT * FROM inventory_instance ORDER BY acquiredAtEpochMs DESC") suspend fun instancesOnce(): List<InventoryInstanceEntity>
    @Query("SELECT * FROM inventory_instance WHERE itemId = :itemId ORDER BY acquiredAtEpochMs DESC") suspend fun instancesByItem(itemId: String): List<InventoryInstanceEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putStack(value: InventoryStackEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertInstances(values: List<InventoryInstanceEntity>)
    @Update suspend fun updateInstance(value: InventoryInstanceEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTransactions(values: List<InventoryTransactionEntity>)
    @Query("SELECT * FROM inventory_stack WHERE itemId = :itemId") suspend fun stack(itemId: String): InventoryStackEntity?
    @Query("SELECT * FROM inventory_instance WHERE instanceId = :id") suspend fun instance(id: String): InventoryInstanceEntity?
    @Query("SELECT * FROM equipment_slot ORDER BY slot") fun equipment(): Flow<List<EquipmentSlotEntity>>
    @Query("SELECT * FROM equipment_slot ORDER BY slot") suspend fun equipmentOnce(): List<EquipmentSlotEntity>
    @Query("SELECT * FROM equipment_slot ORDER BY slot") suspend fun equipmentNow(): List<EquipmentSlotEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEquipment(value: EquipmentSlotEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProgressionEvents(values: List<EquipmentProgressionEventEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEquipmentProgressionEvent(value: EquipmentProgressionEventEntity)
    @Query("DELETE FROM inventory_instance WHERE instanceId = :instanceId") suspend fun deleteInstance(instanceId: String)
    @Query("SELECT * FROM equipment_progression_event WHERE inventoryInstanceId = :instanceId ORDER BY occurredAtEpochMs, id") suspend fun progressionEvents(instanceId: String): List<EquipmentProgressionEventEntity>
    @Query("SELECT * FROM equipment_progression_event WHERE sourceId = :sourceId ORDER BY occurredAtEpochMs, id") suspend fun progressionEventsForSource(sourceId: String): List<EquipmentProgressionEventEntity>
    @Query("SELECT COUNT(*) FROM equipment_progression_event WHERE action = 'CRAFT'") suspend fun craftCount(): Int
}

@Dao interface QuestDao {
    @Query("SELECT * FROM quest_progress ORDER BY acceptedAtEpochMs, questId") fun quests(): Flow<List<QuestProgressEntity>>
    @Query("SELECT * FROM quest_progress ORDER BY acceptedAtEpochMs, questId") suspend fun questsSnapshot(): List<QuestProgressEntity>
    @Query("SELECT * FROM quest_progress WHERE questId = :id") suspend fun quest(id: String): QuestProgressEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putQuest(value: QuestProgressEntity)
    @Query("UPDATE quest_progress SET tracked = 0 WHERE questId != :exceptQuestId") suspend fun clearOtherTracked(exceptQuestId: String)
    @Query("SELECT * FROM quest_objective_progress WHERE questId = :id ORDER BY objectiveId") suspend fun objectives(id: String): List<QuestObjectiveProgressEntity>
    @Query("SELECT * FROM quest_objective_progress ORDER BY questId, objectiveId") fun allObjectives(): Flow<List<QuestObjectiveProgressEntity>>
    @Query("SELECT * FROM quest_objective_progress ORDER BY questId, objectiveId") suspend fun allObjectivesSnapshot(): List<QuestObjectiveProgressEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putObjectives(values: List<QuestObjectiveProgressEntity>)
}

@Dao interface WorldDao {
    @Query("SELECT * FROM world_unlock ORDER BY unlockedAtEpochMs") fun unlocks(): Flow<List<WorldUnlockEntity>>
    @Query("SELECT * FROM world_discovery ORDER BY discoveredAtEpochMs") fun discoveries(): Flow<List<WorldDiscoveryEntity>>
    @Query("SELECT * FROM world_discovery WHERE sourceId = :sourceId ORDER BY discoveredAtEpochMs") suspend fun discoveriesForSource(sourceId: String): List<WorldDiscoveryEntity>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun unlock(value: WorldUnlockEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun discover(value: WorldDiscoveryEntity): Long
    @Query("SELECT COUNT(*) FROM world_discovery") suspend fun discoveryCount(): Int
    @Query("SELECT * FROM collection_entry ORDER BY category, entryId") fun collection(): Flow<List<CollectionEntryEntity>>
    @Query("SELECT * FROM collection_entry WHERE entryId = :id") suspend fun collectionEntry(id: String): CollectionEntryEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCollection(value: CollectionEntryEntity)
    @Query("SELECT COUNT(*) FROM collection_entry") suspend fun collectionCount(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHearth(value: HearthProgressEntity)
    @Query("SELECT * FROM hearth_progress WHERE id = 'hearth'") fun hearth(): Flow<HearthProgressEntity?>
    @Query("SELECT * FROM hearth_progress WHERE id = 'hearth'") suspend fun hearthSnapshot(): HearthProgressEntity?
    @Query("SELECT * FROM hearth_progress WHERE id = 'hearth'") suspend fun hearthNow(): HearthProgressEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertHearthEvent(value: HearthProgressionEventEntity): Long
    @Query("SELECT * FROM hearth_progress_event WHERE sourceId = :sourceId ORDER BY occurredAtEpochMs, id") suspend fun hearthEventsForSource(sourceId: String): List<HearthProgressionEventEntity>
    @Query("SELECT * FROM hearth_progress_event WHERE sourceId = :sourceId OR sourceId LIKE :sourceId || '%' ORDER BY occurredAtEpochMs, id") suspend fun hearthEventsForSourceTree(sourceId: String): List<HearthProgressionEventEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTitle(value: PlayerTitleEntity)
    @Query("SELECT * FROM player_title ORDER BY unlockedAtEpochMs, titleId") fun titles(): Flow<List<PlayerTitleEntity>>
    @Query("SELECT * FROM player_title ORDER BY unlockedAtEpochMs, titleId") suspend fun titlesSnapshot(): List<PlayerTitleEntity>
    @Query("SELECT * FROM player_title WHERE titleId = :titleId") suspend fun title(titleId: String): PlayerTitleEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSceneCompletion(value: WorldSceneCompletionEntity)
    @Query("SELECT * FROM world_scene_completion WHERE sceneId = :sceneId") suspend fun sceneCompletion(sceneId: String): WorldSceneCompletionEntity?
    @Query("SELECT * FROM world_scene_completion ORDER BY lastCompletedAtEpochMs DESC") fun sceneCompletions(): Flow<List<WorldSceneCompletionEntity>>
}

@Dao interface MorningDao {
    @Query("SELECT * FROM morning_note WHERE sleepSessionId = :sessionId") suspend fun note(sessionId: String): MorningNoteEntity?
    @Query("SELECT * FROM morning_note ORDER BY createdAtEpochMs DESC") fun notes(): Flow<List<MorningNoteEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(value: MorningNoteEntity)
}
