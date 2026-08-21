package com.zleeper.sleepapp.data.local.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_session", indices = [Index("state"), Index("finalizedAtEpochMs")])
data class SleepSessionEntity(@PrimaryKey val id: String, val state: String, val sessionStartEpochMs: Long, val sessionEndEpochMs: Long?, val estimatedSleepStartEpochMs: Long?, val estimatedSleepEndEpochMs: Long?, val targetSleepMinutes: Int, val targetWakeMinutes: Int, val estimatedSleepMinutes: Int?, val timingOffsetMinutes: Int?, val windDownCompleted: Boolean, val resolutionMethod: String?, val confidence: String?, val expeditionSeed: Long, val finalizedAtEpochMs: Long?)

@Entity(tableName = "sleep_signal", indices = [Index("sessionId"), Index("occurredAtEpochMs")])
data class SleepSignalEntity(@PrimaryKey val id: String, val sessionId: String, val type: String, val occurredAtEpochMs: Long, val durationMillis: Long?, val confidencePercent: Int?, val createdAtEpochMs: Long)

@Entity(tableName = "night_outcome", indices = [Index(value = ["sleepSessionId"], unique = true)])
data class NightOutcomeEntity(@PrimaryKey val id: String, val sleepSessionId: String, val progressionQuality: Int, val reachBand: Int, val xpGranted: Int, val energyAffinity: Int, val focusAffinity: Int, val resilienceAffinity: Int, val morningReflectionCompleted: Boolean, val createdAtEpochMs: Long)

@Entity(tableName = "expedition", indices = [Index(value = ["sleepSessionId"], unique = true), Index("status")])
data class ExpeditionEntity(@PrimaryKey val id: String, val sleepSessionId: String, val regionId: String, val seed: Long, val status: String, val reachBand: Int?, val contentPackVersion: Int, val progressionRulesVersion: Int, val startedAtEpochMs: Long, val resolvedAtEpochMs: Long?)

@Entity(tableName = "expedition_path_node", primaryKeys = ["expeditionId", "sequence"])
data class ExpeditionPathNodeEntity(val expeditionId: String, val sequence: Int, val nodeId: String, val nodeType: String, val outcomeTextKey: String)

@Entity(tableName = "expedition_reward", indices = [Index("expeditionId")])
data class ExpeditionRewardEntity(@PrimaryKey val id: String, val expeditionId: String, val rewardType: String, val contentId: String?, val quantity: Int, val grantedAtEpochMs: Long)

@Entity(tableName = "pet")
data class PetEntity(@PrimaryKey val instanceId: String, val speciesId: String, val displayName: String, val formId: String, val level: Int, val totalXp: Long, val energy: Int, val focus: Int, val resilience: Int, val energyAffinity: Int, val focusAffinity: Int, val resilienceAffinity: Int, val createdAtEpochMs: Long, val updatedAtEpochMs: Long)

@Entity(tableName = "pet_progression_event", indices = [Index("petId"), Index("occurredAtEpochMs")])
data class PetProgressionEventEntity(@PrimaryKey val id: String, val petId: String, val type: String, val amount: Int, val sourceId: String, val rulesVersion: Int, val occurredAtEpochMs: Long)

@Entity(tableName = "inventory_stack")
data class InventoryStackEntity(@PrimaryKey val itemId: String, val quantity: Int, val updatedAtEpochMs: Long)

@Entity(tableName = "inventory_instance", indices = [Index("itemId")])
data class InventoryInstanceEntity(@PrimaryKey val instanceId: String, val itemId: String, val acquiredAtEpochMs: Long, val equippedSlot: String?)

@Entity(tableName = "inventory_transaction", indices = [Index("itemId"), Index("occurredAtEpochMs")])
data class InventoryTransactionEntity(@PrimaryKey val id: String, val itemId: String, val instanceId: String?, val quantityDelta: Int, val reason: String, val sourceId: String, val occurredAtEpochMs: Long)

@Entity(tableName = "equipment_slot")
data class EquipmentSlotEntity(@PrimaryKey val slot: String, val inventoryInstanceId: String?, val updatedAtEpochMs: Long)

@Entity(tableName = "quest_progress", indices = [Index("status")])
data class QuestProgressEntity(@PrimaryKey val questId: String, val status: String, val acceptedAtEpochMs: Long, val completedAtEpochMs: Long?, val claimedAtEpochMs: Long?)

@Entity(tableName = "quest_objective_progress", primaryKeys = ["questId", "objectiveId"])
data class QuestObjectiveProgressEntity(val questId: String, val objectiveId: String, val currentCount: Int, val requiredCount: Int, val windowStartedAtEpochMs: Long?, val updatedAtEpochMs: Long)

@Entity(tableName = "world_unlock")
data class WorldUnlockEntity(@PrimaryKey val contentId: String, val contentType: String, val sourceId: String, val unlockedAtEpochMs: Long)

@Entity(tableName = "world_discovery", indices = [Index("regionId")])
data class WorldDiscoveryEntity(@PrimaryKey val discoveryId: String, val regionId: String, val sourceId: String, val discoveredAtEpochMs: Long)

@Entity(tableName = "collection_entry")
data class CollectionEntryEntity(@PrimaryKey val entryId: String, val category: String, val quantity: Int, val firstDiscoveredAtEpochMs: Long, val updatedAtEpochMs: Long)

@Entity(tableName = "morning_note", indices = [Index(value = ["sleepSessionId"], unique = true)])
data class MorningNoteEntity(@PrimaryKey val id: String, val sleepSessionId: String, val mood: Int, val note: String, val createdAtEpochMs: Long, val updatedAtEpochMs: Long)
