package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.ExpeditionDao
import com.zleeper.sleepapp.data.local.database.ExpeditionEntity
import com.zleeper.sleepapp.data.local.database.ExpeditionPathNodeEntity
import com.zleeper.sleepapp.data.local.database.ExpeditionRewardEntity
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.InventoryStackEntity
import com.zleeper.sleepapp.data.local.database.InventoryInstanceEntity
import com.zleeper.sleepapp.data.local.database.InventoryTransactionEntity
import com.zleeper.sleepapp.data.local.database.NightOutcomeEntity
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.data.local.database.PetProgressionEventEntity
import com.zleeper.sleepapp.data.local.database.QuestDao
import com.zleeper.sleepapp.data.local.database.SleepDao
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.data.local.database.WorldDao
import com.zleeper.sleepapp.data.local.database.CollectionEntryEntity
import com.zleeper.sleepapp.domain.expedition.ExpeditionInput
import com.zleeper.sleepapp.domain.expedition.ExpeditionResolver
import com.zleeper.sleepapp.domain.progression.ProgressionCalculator
import com.zleeper.sleepapp.domain.progression.ProgressionRules
import com.zleeper.sleepapp.domain.sleep.ResolvedSleepSession
import com.zleeper.sleepapp.domain.sleep.SleepConfidence
import com.zleeper.sleepapp.domain.sleep.SleepResolutionMethod
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class MorningResult(val expeditionId: String, val reachBand: Int, val xp: Int, val rewards: List<Pair<String, Int>>, val pathNodeIds: List<String>)

@Singleton
class NightResolutionService @Inject constructor(
    private val database: ZleeperDatabase,
    private val sleepDao: SleepDao,
    private val expeditionDao: ExpeditionDao,
    private val petDao: PetDao,
    private val inventoryDao: InventoryDao,
    private val questDao: QuestDao,
    private val worldDao: WorldDao,
    private val content: GameContentRepository,
    private val resolver: ExpeditionResolver,
) {
    suspend fun resolve(sessionId: String, targetDurationMinutes: Int, reflected: Boolean): MorningResult = database.withTransaction {
        val session = requireNotNull(sleepDao.session(sessionId))
        require(session.state == "FINALIZED" || session.state == "EXPEDITION_RESOLVED")
        expeditionDao.forSession(sessionId)?.takeIf { it.status == "RESOLVED" }?.let { existing ->
            return@withTransaction MorningResult(existing.id, requireNotNull(existing.reachBand), sleepDao.outcome(sessionId)?.xpGranted ?: 0, expeditionDao.rewards(existing.id).map { requireNotNull(it.contentId) to it.quantity }, expeditionDao.path(existing.id).map { it.nodeId })
        }
        val pet = requireNotNull(petDao.pet())
        val rulesAsset = content.progressionRules
        val rules = ProgressionRules(rulesAsset.version, rulesAsset.baseParticipationXp, rulesAsset.durationWeight, rulesAsset.timingWeight, rulesAsset.consistencyWeight, rulesAsset.windDownWeight, rulesAsset.reflectionWeight)
        val resolvedSleep = ResolvedSleepSession(session.id, session.sessionStartEpochMs, requireNotNull(session.sessionEndEpochMs), requireNotNull(session.estimatedSleepStartEpochMs), requireNotNull(session.estimatedSleepEndEpochMs), session.targetSleepMinutes, session.targetWakeMinutes, requireNotNull(session.estimatedSleepMinutes), requireNotNull(session.timingOffsetMinutes), session.windDownCompleted, SleepResolutionMethod.valueOf(requireNotNull(session.resolutionMethod)), SleepConfidence.valueOf(requireNotNull(session.confidence)), requireNotNull(session.finalizedAtEpochMs))
        val quality = ProgressionCalculator.calculate(resolvedSleep, targetDurationMinutes, session.timingOffsetMinutes ?: 180, reflected, rules)
        val band = ProgressionCalculator.reachBand(quality.xp, pet.energy, pet.resilience)
        val region = if (band >= 3 && pet.level >= 5) content.regions[1] else content.regions[0]
        val expeditionId = stableId("expedition:$sessionId")
        val result = resolver.resolve(ExpeditionInput(expeditionId, session.expeditionSeed, region.id, band * 32 + pet.energy * 2, pet.focus), content.expeditionNodes, content.lootTables)
        val now = System.currentTimeMillis()
        expeditionDao.insert(ExpeditionEntity(expeditionId, sessionId, region.id, session.expeditionSeed, "RESOLVING", null, content.manifest.contentPackVersion, rules.version, session.sessionStartEpochMs, null))
        expeditionDao.insertPath(result.path.mapIndexed { index, node -> ExpeditionPathNodeEntity(expeditionId, index, node.id, node.type, node.textKey) })
        val rewardEntities = result.rewards.map { reward -> ExpeditionRewardEntity(stableId("$expeditionId:${reward.itemId}"), expeditionId, "ITEM", reward.itemId, reward.quantity, now) }
        expeditionDao.insertRewards(rewardEntities)
        result.rewards.forEach { reward ->
            val definition = requireNotNull(content.items.firstOrNull { it.id == reward.itemId })
            if (definition.stackable) {
                val current = inventoryDao.stack(reward.itemId)?.quantity ?: 0
                inventoryDao.putStack(InventoryStackEntity(reward.itemId, current + reward.quantity, now))
                inventoryDao.insertTransactions(listOf(InventoryTransactionEntity(stableId("ledger:$expeditionId:${reward.itemId}"), reward.itemId, null, reward.quantity, "EXPEDITION_REWARD", expeditionId, now)))
            } else {
                val instances = (0 until reward.quantity).map { index ->
                    InventoryInstanceEntity(stableId("instance:$expeditionId:${reward.itemId}:$index"), reward.itemId, now, null)
                }
                inventoryDao.insertInstances(instances)
                inventoryDao.insertTransactions(instances.map { instance -> InventoryTransactionEntity(stableId("ledger:${instance.instanceId}"), reward.itemId, instance.instanceId, 1, "EXPEDITION_REWARD", expeditionId, now) })
            }
            val collected = worldDao.collectionEntry(reward.itemId)
            worldDao.putCollection(CollectionEntryEntity(reward.itemId, definition.category, (collected?.quantity ?: 0) + reward.quantity, collected?.firstDiscoveredAtEpochMs ?: now, now))
        }
        val newXp = pet.totalXp + quality.xp
        var level = pet.level
        var thresholdTotal = (1 until level).sumOf { ProgressionCalculator.xpToNextLevel(it).toLong() }
        while (newXp >= thresholdTotal + ProgressionCalculator.xpToNextLevel(level)) { thresholdTotal += ProgressionCalculator.xpToNextLevel(level); level++ }
        petDao.update(pet.copy(level = level, totalXp = newXp, energyAffinity = pet.energyAffinity + quality.consistencyFit, focusAffinity = pet.focusAffinity + quality.windDown, resilienceAffinity = pet.resilienceAffinity + quality.timingFit, updatedAtEpochMs = now))
        petDao.insertEvents(listOf(PetProgressionEventEntity(stableId("xp:$expeditionId"), pet.instanceId, "XP_GRANTED", quality.xp, expeditionId, rules.version, now)))
        sleepDao.insertOutcome(NightOutcomeEntity(stableId("outcome:$sessionId"), sessionId, quality.xp, band, quality.xp, quality.consistencyFit, quality.windDown, quality.timingFit, reflected, now))
        content.quests.forEach { quest ->
            val progress = questDao.quest(quest.id) ?: return@forEach
            if (progress.status != "ACTIVE") return@forEach
            val objectives = questDao.objectives(quest.id).map { objective ->
                val definition = quest.objectives.first { it.id == objective.objectiveId }
                val add = when (definition.type) { "FINALIZE_SLEEP_SESSION", "COMPLETE_MORNING_REVIEW", "COMPLETE_EXPEDITION" -> 1; "COLLECT_ITEM" -> result.rewards.filter { it.itemId == definition.parameters["item_id"] }.sumOf { it.quantity }; else -> 0 }
                objective.copy(currentCount = (objective.currentCount + add).coerceAtMost(objective.requiredCount), updatedAtEpochMs = now)
            }
            questDao.putObjectives(objectives)
            if (objectives.all { it.currentCount >= it.requiredCount }) questDao.putQuest(progress.copy(status = "COMPLETED", completedAtEpochMs = now))
        }
        expeditionDao.update(ExpeditionEntity(expeditionId, sessionId, region.id, session.expeditionSeed, "RESOLVED", band, content.manifest.contentPackVersion, rules.version, session.sessionStartEpochMs, now))
        sleepDao.updateSession(session.copy(state = "EXPEDITION_RESOLVED"))
        MorningResult(expeditionId, band, quality.xp, result.rewards.map { it.itemId to it.quantity }, result.path.map { it.id })
    }

    private fun stableId(value: String): String = UUID.nameUUIDFromBytes(value.toByteArray(StandardCharsets.UTF_8)).toString()
}
