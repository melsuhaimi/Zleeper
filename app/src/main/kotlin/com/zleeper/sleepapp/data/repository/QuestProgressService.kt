package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.core.id.stableId
import com.zleeper.sleepapp.core.id.stableLong
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.content.QuestDefinition
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.InventoryInstanceEntity
import com.zleeper.sleepapp.data.local.database.InventoryStackEntity
import com.zleeper.sleepapp.data.local.database.InventoryTransactionEntity
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.data.local.database.PetProgressionEventEntity
import com.zleeper.sleepapp.data.local.database.QuestDao
import com.zleeper.sleepapp.data.local.database.QuestObjectiveProgressEntity
import com.zleeper.sleepapp.data.local.database.QuestProgressEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.domain.progression.LevelCurve
import com.zleeper.sleepapp.domain.progression.LevelProgress
import com.zleeper.sleepapp.domain.progression.ProgressionRules
import com.zleeper.sleepapp.domain.progression.ReachThresholds
import com.zleeper.sleepapp.domain.quest.DomainEvent
import com.zleeper.sleepapp.domain.quest.ObjectiveType
import com.zleeper.sleepapp.domain.quest.QuestEngine
import com.zleeper.sleepapp.domain.quest.QuestLifecycle
import com.zleeper.sleepapp.domain.quest.QuestObjectiveRule
import com.zleeper.sleepapp.domain.quest.QuestObjectiveState
import com.zleeper.sleepapp.domain.quest.QuestState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestProgressService @Inject constructor(
    private val database: ZleeperDatabase,
    private val questDao: QuestDao,
    private val petDao: PetDao,
    private val inventoryDao: InventoryDao,
    private val content: GameContentRepository,
    private val worldProgressService: WorldProgressService,
    private val memoryService: PetMemoryService,
) {
    suspend fun initialize(now: Long = System.currentTimeMillis()) = database.withTransaction {
        if (questDao.questsSnapshot().isNotEmpty()) return@withTransaction
        content.quests.forEachIndexed { index, quest ->
            val lifecycle = when {
                index == 0 -> QuestLifecycle.ACTIVE
                quest.prerequisiteQuests.isEmpty() -> QuestLifecycle.AVAILABLE
                else -> QuestLifecycle.LOCKED
            }
            questDao.putQuest(QuestProgressEntity(quest.id, lifecycle.name, if (lifecycle == QuestLifecycle.ACTIVE) now else 0L, null, null, index == 0, null))
            questDao.putObjectives(quest.objectives.map { QuestObjectiveProgressEntity(quest.id, it.id, 0, it.requiredCount, null, now) })
        }
    }

    suspend fun accept(questId: String) = database.withTransaction {
        val progress = requireNotNull(questDao.quest(questId)) { "Unknown quest" }
        val next = QuestEngine.accept(progress.toDomain(questDao.objectives(questId)))
        questDao.putQuest(progress.copy(status = next.lifecycle.name, acceptedAtEpochMs = System.currentTimeMillis(), abandonedAtEpochMs = null))
    }

    suspend fun abandon(questId: String) = database.withTransaction {
        val definition = requireNotNull(content.quests.firstOrNull { it.id == questId })
        val progress = requireNotNull(questDao.quest(questId))
        val now = System.currentTimeMillis()
        val next = QuestEngine.abandon(progress.toDomain(questDao.objectives(questId)), definition.abandonResetsProgress)
        questDao.putQuest(progress.copy(status = next.lifecycle.name, tracked = false, abandonedAtEpochMs = now))
        if (definition.abandonResetsProgress) questDao.putObjectives(definition.objectives.map { QuestObjectiveProgressEntity(questId, it.id, 0, it.requiredCount, null, now) })
    }

    suspend fun track(questId: String, tracked: Boolean) = database.withTransaction {
        val progress = requireNotNull(questDao.quest(questId))
        QuestEngine.track(progress.toDomain(questDao.objectives(questId)), tracked)
        if (tracked) questDao.clearOtherTracked(questId)
        questDao.putQuest(progress.copy(tracked = tracked))
    }

    suspend fun record(event: DomainEvent, now: Long = System.currentTimeMillis()) = database.withTransaction { recordInternal(event, now) }

    suspend fun claim(questId: String, now: Long = System.currentTimeMillis()) = database.withTransaction {
        val definition = requireNotNull(content.quests.firstOrNull { it.id == questId })
        val progress = requireNotNull(questDao.quest(questId))
        QuestEngine.claim(progress.toDomain(questDao.objectives(questId)))
        val emitted = mutableListOf<DomainEvent>()
        definition.rewards.forEachIndexed { index, reward ->
            when (reward.type) {
                "ITEM" -> emitted += grantItem(requireNotNull(reward.id), reward.quantity, "quest:$questId:$index", now)
                "XP" -> grantXp(reward.quantity, "quest:$questId:$index", now, emitted)
                "HEARTH_MEMORY" -> worldProgressService.addHearthMemories(reward.quantity, "QUEST_CLAIM", questId, now)
                else -> error("Unsupported quest reward ${reward.type}")
            }
        }
        questDao.putQuest(progress.copy(status = QuestLifecycle.CLAIMED.name, tracked = false, claimedAtEpochMs = now))
        worldProgressService.recordCollection(questId, "QUEST_STORY", 1, "quest-claim:$questId", now)
        memoryService.remember("QUEST_STORY", questId, "thought.quest_story", now)
        unlockEligibleQuests()
        emitted.forEach { recordInternal(it, now) }
    }

    private suspend fun recordInternal(event: DomainEvent, now: Long) {
        content.quests.forEach { definition ->
            val progress = questDao.quest(definition.id) ?: return@forEach
            if (progress.status != QuestLifecycle.ACTIVE.name) return@forEach
            val persisted = questDao.objectives(definition.id)
            val normalized = persisted.map { objective ->
                val rule = definition.objectives.first { it.id == objective.objectiveId }
                val windowNights = rule.parameters["window_nights"]?.toIntOrNull()
                if (windowNights != null && objective.windowStartedAtEpochMs != null && now - objective.windowStartedAtEpochMs >= windowNights * DAY_MS) {
                    objective.copy(currentCount = 0, windowStartedAtEpochMs = null, updatedAtEpochMs = now)
                } else objective
            }
            val next = QuestEngine.apply(progress.toDomain(normalized), definition.rules(), event)
            questDao.putObjectives(normalized.map { entity ->
                val updated = next.objectives.first { it.objectiveId == entity.objectiveId }
                val hasWindow = definition.objectives.first { it.id == entity.objectiveId }.parameters.containsKey("window_nights")
                val started = when {
                    entity.currentCount == 0 && updated.currentCount > 0 && hasWindow -> now
                    updated.currentCount == 0 -> null
                    else -> entity.windowStartedAtEpochMs
                }
                entity.copy(currentCount = updated.currentCount, windowStartedAtEpochMs = started, updatedAtEpochMs = now)
            })
            if (next.lifecycle == QuestLifecycle.COMPLETED && progress.status != QuestLifecycle.COMPLETED.name) {
                questDao.putQuest(progress.copy(status = QuestLifecycle.COMPLETED.name, completedAtEpochMs = now))
            }
        }
    }

    private suspend fun grantItem(itemId: String, quantity: Int, sourceId: String, now: Long): DomainEvent.ItemGranted {
        val item = requireNotNull(content.items.firstOrNull { it.id == itemId })
        if (item.stackable) {
            val current = inventoryDao.stack(itemId)?.quantity ?: 0
            inventoryDao.putStack(InventoryStackEntity(itemId, current + quantity, now))
            inventoryDao.insertTransactions(listOf(InventoryTransactionEntity(stableId("inventory-ledger", sourceId, itemId), itemId, null, quantity, "QUEST_REWARD", sourceId, now)))
        } else {
            val instances = (0 until quantity).map { index ->
                val instanceId = stableId("quest-item", sourceId, itemId, index.toString())
                InventoryInstanceEntity(instanceId, itemId, now, null, progressionSeed = stableLong(instanceId))
            }
            inventoryDao.insertInstances(instances)
            inventoryDao.insertTransactions(instances.map { InventoryTransactionEntity(stableId("inventory-ledger", it.instanceId), itemId, it.instanceId, 1, "QUEST_REWARD", sourceId, now) })
        }
        worldProgressService.recordCollection(itemId, item.category, quantity, sourceId, now)
        return DomainEvent.ItemGranted(sourceId, itemId, quantity)
    }

    private suspend fun grantXp(amount: Int, sourceId: String, now: Long, emitted: MutableList<DomainEvent>) {
        val pet = requireNotNull(petDao.pet())
        val rules = content.progressionRules.toDomain()
        val advance = LevelProgress.advance(pet.level, pet.totalXp, amount, rules)
        val newTotal = pet.totalXp + amount
        petDao.update(pet.copy(level = advance.newLevel, totalXp = newTotal, dreamSparksAvailable = pet.dreamSparksAvailable + advance.dreamSparksGranted, dreamSparksEarned = pet.dreamSparksEarned + advance.dreamSparksGranted, updatedAtEpochMs = now))
        petDao.insertEvents(buildList {
            add(PetProgressionEventEntity(stableId("pet-event", sourceId, "xp"), pet.instanceId, "XP_GRANTED", amount, sourceId, rules.version, now))
            if (advance.levelsGained > 0) add(PetProgressionEventEntity(stableId("pet-event", sourceId, "levels"), pet.instanceId, "LEVEL_GAINED", advance.levelsGained, sourceId, rules.version, now))
            if (advance.dreamSparksGranted > 0) add(PetProgressionEventEntity(stableId("pet-event", sourceId, "sparks"), pet.instanceId, "DREAM_SPARKS_GRANTED", advance.dreamSparksGranted, sourceId, rules.version, now))
        })
        emitted += DomainEvent.PetXpGranted(sourceId, amount)
        if (advance.levelsGained > 0) {
            emitted += DomainEvent.PetLevelReached(sourceId, advance.newLevel)
            memoryService.remember("LEVEL_UP", sourceId, "thought.recent_level_up", now)
        }
    }

    private suspend fun unlockEligibleQuests() {
        val progress = questDao.questsSnapshot().associateBy { it.questId }
        content.quests.forEach { quest ->
            val current = progress[quest.id] ?: return@forEach
            if (current.status != QuestLifecycle.LOCKED.name) return@forEach
            if (quest.prerequisiteQuests.all { progress[it]?.status == QuestLifecycle.CLAIMED.name }) {
                questDao.putQuest(current.copy(status = QuestLifecycle.AVAILABLE.name, acceptedAtEpochMs = 0L, abandonedAtEpochMs = null))
            }
        }
    }

    private fun QuestProgressEntity.toDomain(objectives: List<QuestObjectiveProgressEntity>) = QuestState(questId, QuestLifecycle.valueOf(status), tracked, objectives.map { QuestObjectiveState(it.objectiveId, it.currentCount, it.requiredCount) })
    private fun QuestDefinition.rules() = objectives.map { QuestObjectiveRule(it.id, ObjectiveType.valueOf(it.type), it.requiredCount, it.parameters) }
    private fun com.zleeper.sleepapp.data.content.ProgressionRulesDefinition.toDomain() = ProgressionRules(
        version, baseParticipationXp, durationWeight, timingWeight, consistencyWeight, windDownWeight, reflectionWeight, dreamSparksPerLevel,
        LevelCurve(levelCurve.base, levelCurve.linear, levelCurve.exponent),
        ReachThresholds(expeditionDepthRules.band1, expeditionDepthRules.band2, expeditionDepthRules.band3, expeditionDepthRules.band4, expeditionDepthRules.band5),
    )
    private companion object { const val DAY_MS = 86_400_000L }
}
