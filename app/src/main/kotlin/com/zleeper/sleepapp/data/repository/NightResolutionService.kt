package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.core.id.stableId
import com.zleeper.sleepapp.core.id.stableLong
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.ExpeditionDao
import com.zleeper.sleepapp.data.local.database.ExpeditionEntity
import com.zleeper.sleepapp.data.local.database.ExpeditionPathNodeEntity
import com.zleeper.sleepapp.data.local.database.ExpeditionRewardEntity
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.InventoryInstanceEntity
import com.zleeper.sleepapp.data.local.database.InventoryStackEntity
import com.zleeper.sleepapp.data.local.database.InventoryTransactionEntity
import com.zleeper.sleepapp.data.local.database.NightOutcomeEntity
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.data.local.database.PetProgressionEventEntity
import com.zleeper.sleepapp.data.local.database.SleepDao
import com.zleeper.sleepapp.data.local.database.WorldDao
import com.zleeper.sleepapp.data.local.database.WorldDiscoveryEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.domain.expedition.ExpeditionInput
import com.zleeper.sleepapp.domain.expedition.ExpeditionNode
import com.zleeper.sleepapp.domain.expedition.ExpeditionResolver
import com.zleeper.sleepapp.domain.expedition.LootEntry
import com.zleeper.sleepapp.domain.expedition.LootTable
import com.zleeper.sleepapp.domain.pet.PetStatGrowth
import com.zleeper.sleepapp.domain.progression.LevelCurve
import com.zleeper.sleepapp.domain.progression.LevelProgress
import com.zleeper.sleepapp.domain.progression.ProgressionCalculator
import com.zleeper.sleepapp.domain.progression.ProgressionRules
import com.zleeper.sleepapp.domain.progression.ReachThresholds
import com.zleeper.sleepapp.domain.quest.DomainEvent
import com.zleeper.sleepapp.domain.sleep.ResolvedSleepSession
import com.zleeper.sleepapp.domain.sleep.ScheduleConsistencyCalculator
import com.zleeper.sleepapp.domain.sleep.SleepConfidence
import com.zleeper.sleepapp.domain.sleep.SleepResolutionMethod
import com.zleeper.sleepapp.domain.sleep.SleepSchedule
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class MorningStatChange(val stat: String, val previous: Int, val current: Int) {
    val gained: Int get() = current - previous
}

data class MorningJourneyStep(val nodeId: String, val nodeType: String, val narrative: String)

data class MorningResult(
    val expeditionId: String,
    val regionId: String,
    val reachBand: Int,
    val xp: Int,
    val rewards: List<Pair<String, Int>>,
    val pathNodeIds: List<String>,
    val journey: List<MorningJourneyStep>,
    val previousLevel: Int,
    val newLevel: Int,
    val dreamSparksGained: Int,
    val statChanges: List<MorningStatChange>,
    val newDiscoveries: List<String>,
    val hearthMemoriesGained: Int,
)

/**
 * Resolves one finalized sleep session into one immutable expedition/reward transaction.
 * Repeated calls never reroll or regrant: they reconstruct MorningResult from persisted ledgers.
 */
@Singleton
class NightResolutionService @Inject constructor(
    private val database: ZleeperDatabase,
    private val sleepDao: SleepDao,
    private val expeditionDao: ExpeditionDao,
    private val petDao: PetDao,
    private val inventoryDao: InventoryDao,
    private val worldDao: WorldDao,
    private val content: GameContentRepository,
    private val effectsService: GameEffectsService,
    private val questProgressService: QuestProgressService,
    private val worldProgressService: WorldProgressService,
    private val memoryService: PetMemoryService,
    private val titleService: TitleService,
) {
    suspend fun resolve(sessionId: String, reflected: Boolean): MorningResult {
        val result = database.withTransaction {
            val session = requireNotNull(sleepDao.session(sessionId)) { "Sleep session not found" }
            require(session.state == "FINALIZED" || session.state == "EXPEDITION_RESOLVED") {
                "Sleep session must be finalized before expedition resolution"
            }

            expeditionDao.forSession(sessionId)?.takeIf { it.status == "RESOLVED" }?.let { existing ->
                return@withTransaction reconstruct(existing)
            }
            require(session.state == "FINALIZED") { "Resolved session is missing its expedition ledger" }

            val pet = requireNotNull(petDao.pet()) { "Create a companion before resolving a night" }
            val now = System.currentTimeMillis()
            val rules = content.progressionRules.toDomain()
            val resolved = session.toDomainResolved()
            val targetDuration = SleepSchedule.durationBetween(session.targetSleepMinutes, session.targetWakeMinutes)
            val currentStart = minutesOfDay(resolved.estimatedSleepStartEpochMs)
            val recentStarts = sleepDao.recentResolved(8)
                .asSequence()
                .filter { it.id != session.id }
                .mapNotNull { it.estimatedSleepStartEpochMs?.let(::minutesOfDay) }
                .take(7)
                .toList()
            val consistencyOffset = ScheduleConsistencyCalculator.averageOffsetMinutes(listOf(currentStart) + recentStarts)
            val quality = ProgressionCalculator.calculate(resolved, targetDuration, consistencyOffset, reflected, rules)
            val effects = effectsService.current()
            val effectiveResilience = (pet.resilience + effects.resilienceBonus).coerceAtLeast(0)
            val reachScore = ProgressionCalculator.expeditionReachScore(quality.xp, pet.energy, effectiveResilience) +
                effects.expeditionDepthBonus + effects.expeditionBudget
            val reachBand = ProgressionCalculator.reachBand(reachScore, rules.reachThresholds)
            val region = chooseRegion(pet.level, reachBand)
            val expeditionId = stableId("expedition", session.id)
            val expedition = ExpeditionResolver.resolve(
                ExpeditionInput(
                    expeditionId = expeditionId,
                    seed = session.expeditionSeed,
                    regionId = region.id,
                    budget = reachScore.coerceAtLeast(0),
                    focus = (pet.focus + effects.focusBonus + effects.focusRouteBonus).coerceAtLeast(0),
                    bonusMaterialRolls = (effects.forageBonus + effects.discoveryBonus).coerceAtLeast(0),
                    materialCapacityBonus = effects.materialCapacity.coerceAtLeast(0),
                ),
                nodes = content.expeditionNodes.map { ExpeditionNode(it.id, it.regionId, it.type, it.depthCost, it.focusRequired, it.lootTableId, it.next, it.textKey) },
                lootTables = content.lootTables.map { table ->
                    LootTable(
                        table.id,
                        table.rolls,
                        table.entries.map { entry ->
                            LootEntry(
                                itemId = entry.itemId,
                                weight = entry.weight,
                                minimum = entry.minimum,
                                maximum = entry.maximum,
                                material = content.items.firstOrNull { it.id == entry.itemId }?.category == "MATERIAL",
                            )
                        },
                    )
                },
            )

            val pendingExpedition = ExpeditionEntity(
                id = expeditionId,
                sleepSessionId = session.id,
                regionId = region.id,
                seed = session.expeditionSeed,
                status = "RESOLVING",
                reachBand = null,
                contentPackVersion = content.manifest.contentPackVersion,
                progressionRulesVersion = rules.version,
                startedAtEpochMs = session.sessionStartEpochMs,
                resolvedAtEpochMs = null,
            )
            expeditionDao.insert(pendingExpedition)
            expeditionDao.insertPath(expedition.path.mapIndexed { index, node ->
                ExpeditionPathNodeEntity(expeditionId, index, node.id, node.type, node.textKey)
            })
            val rewardRows = expedition.rewards.map { reward ->
                ExpeditionRewardEntity(
                    stableId("expedition-reward", expeditionId, reward.itemId),
                    expeditionId,
                    "ITEM",
                    reward.itemId,
                    reward.quantity,
                    now,
                )
            }
            if (rewardRows.isNotEmpty()) expeditionDao.insertRewards(rewardRows)

            val questEvents = mutableListOf<DomainEvent>()
            expedition.rewards.forEach { reward ->
                grantReward(expeditionId, reward.itemId, reward.quantity, now)
                questEvents += DomainEvent.ItemGranted(expeditionId, reward.itemId, reward.quantity)
            }

            val levelAdvance = LevelProgress.advance(pet.level, pet.totalXp, quality.xp, rules)
            val growth = content.progressionRules.statGrowthRates
            val energy = PetStatGrowth.apply(pet.energy, pet.energyAffinity, quality.consistencyFit, growth.energy)
            val focus = PetStatGrowth.apply(pet.focus, pet.focusAffinity, quality.windDown, growth.focus)
            val resilience = PetStatGrowth.apply(pet.resilience, pet.resilienceAffinity, quality.timingFit, growth.resilience)
            val updatedPet = pet.copy(
                level = levelAdvance.newLevel,
                totalXp = pet.totalXp + quality.xp,
                energy = energy.stat,
                focus = focus.stat,
                resilience = resilience.stat,
                energyAffinity = energy.affinityRemainder,
                focusAffinity = focus.affinityRemainder,
                resilienceAffinity = resilience.affinityRemainder,
                dreamSparksAvailable = pet.dreamSparksAvailable + levelAdvance.dreamSparksGranted,
                dreamSparksEarned = pet.dreamSparksEarned + levelAdvance.dreamSparksGranted,
                updatedAtEpochMs = now,
            )
            petDao.update(updatedPet)
            petDao.insertEvents(buildList {
                add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "xp"), pet.instanceId, "XP_GRANTED", quality.xp, expeditionId, rules.version, now))
                add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "level-before"), pet.instanceId, "LEVEL_BEFORE", pet.level, expeditionId, rules.version, now))
                add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "level-after"), pet.instanceId, "LEVEL_AFTER", updatedPet.level, expeditionId, rules.version, now))
                add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "energy-before"), pet.instanceId, "ENERGY_BEFORE", pet.energy, expeditionId, rules.version, now))
                add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "energy-after"), pet.instanceId, "ENERGY_AFTER", updatedPet.energy, expeditionId, rules.version, now))
                add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "focus-before"), pet.instanceId, "FOCUS_BEFORE", pet.focus, expeditionId, rules.version, now))
                add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "focus-after"), pet.instanceId, "FOCUS_AFTER", updatedPet.focus, expeditionId, rules.version, now))
                add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "resilience-before"), pet.instanceId, "RESILIENCE_BEFORE", pet.resilience, expeditionId, rules.version, now))
                add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "resilience-after"), pet.instanceId, "RESILIENCE_AFTER", updatedPet.resilience, expeditionId, rules.version, now))
                if (levelAdvance.levelsGained > 0) add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "levels"), pet.instanceId, "LEVEL_GAINED", levelAdvance.levelsGained, expeditionId, rules.version, now))
                if (levelAdvance.dreamSparksGranted > 0) add(PetProgressionEventEntity(stableId("pet-event", expeditionId, "sparks"), pet.instanceId, "DREAM_SPARKS_GRANTED", levelAdvance.dreamSparksGranted, expeditionId, rules.version, now))
            })
            if (levelAdvance.levelsGained > 0) memoryService.remember("LEVEL_UP", expeditionId, "thought.recent_level_up", now)

            expedition.path.filter { it.type == "EVENT" || it.type == "DESTINATION" }.forEach { node ->
                val discoveryId = stableId("expedition-discovery", node.id)
                if (worldDao.discover(WorldDiscoveryEntity(discoveryId, region.id, expeditionId, now)) != -1L) {
                    worldProgressService.recordCollection(node.id, "EXPEDITION_ENCOUNTER", 1, "$expeditionId:${node.id}", now)
                    worldProgressService.addHearthMemories(content.progressionRules.hearthMemoryRewards?.newDiscovery ?: 0, "NEW_DISCOVERY", "$expeditionId:discovery:${node.id}", now)
                    memoryService.remember("EXPEDITION_DISCOVERY", "$expeditionId:${node.id}", "thought.expedition_discovery", now, node.id)
                    questEvents += DomainEvent.WorldNodeDiscovered(expeditionId, node.id, region.id)
                }
            }
            worldProgressService.addHearthMemories(content.progressionRules.hearthMemoryRewards?.finalizedNight ?: 0, "FINALIZED_NIGHT", expeditionId, now)
            memoryService.remember("MORNING_RETURN", expeditionId, "thought.morning_return", now, region.id)

            sleepDao.insertOutcome(
                NightOutcomeEntity(
                    stableId("night-outcome", session.id), session.id, quality.xp, reachBand, quality.xp,
                    quality.consistencyFit, quality.windDown, quality.timingFit, reflected, now,
                ),
            )

            questEvents += DomainEvent.SleepTimingObserved(expeditionId, resolved.timingOffsetMinutes)
            questEvents += DomainEvent.ConsistencyAccumulated(expeditionId, quality.consistencyFit)
            questEvents += DomainEvent.SleepSessionFinalized(expeditionId)
            if (reflected) questEvents += DomainEvent.MorningReviewCompleted(expeditionId)
            questEvents += DomainEvent.ExpeditionResolved(expeditionId, reachBand)
            questEvents += DomainEvent.PetXpGranted(expeditionId, quality.xp)
            if (levelAdvance.levelsGained > 0) questEvents += DomainEvent.PetLevelReached(expeditionId, updatedPet.level)
            if (energy.statGains > 0) questEvents += DomainEvent.PetStatReached(expeditionId, "ENERGY", updatedPet.energy)
            if (focus.statGains > 0) questEvents += DomainEvent.PetStatReached(expeditionId, "FOCUS", updatedPet.focus)
            if (resilience.statGains > 0) questEvents += DomainEvent.PetStatReached(expeditionId, "RESILIENCE", updatedPet.resilience)
            questEvents.forEach { questProgressService.record(it, now) }

            val resolvedExpedition = pendingExpedition.copy(status = "RESOLVED", reachBand = reachBand, resolvedAtEpochMs = now)
            expeditionDao.update(resolvedExpedition)
            sleepDao.updateSession(session.copy(state = "EXPEDITION_RESOLVED"))
            reconstruct(resolvedExpedition)
        }
        titleService.evaluateAll()
        return result
    }

    private suspend fun grantReward(expeditionId: String, itemId: String, quantity: Int, now: Long) {
        val item = requireNotNull(content.items.firstOrNull { it.id == itemId }) { "Unknown expedition reward item: $itemId" }
        if (item.stackable) {
            val current = inventoryDao.stack(itemId)?.quantity ?: 0
            inventoryDao.putStack(InventoryStackEntity(itemId, current + quantity, now))
            inventoryDao.insertTransactions(listOf(InventoryTransactionEntity(stableId("inventory-ledger", expeditionId, itemId), itemId, null, quantity, "EXPEDITION_REWARD", expeditionId, now)))
        } else {
            val instances = (0 until quantity).map { index ->
                val instanceId = stableId("expedition-item", expeditionId, itemId, index.toString())
                InventoryInstanceEntity(instanceId, itemId, now, null, progressionSeed = stableLong(instanceId))
            }
            inventoryDao.insertInstances(instances)
            inventoryDao.insertTransactions(instances.map { instance ->
                InventoryTransactionEntity(stableId("inventory-ledger", instance.instanceId), itemId, instance.instanceId, 1, "EXPEDITION_REWARD", expeditionId, now)
            })
        }
        worldProgressService.recordCollection(itemId, item.category, quantity, expeditionId, now)
    }

    private suspend fun reconstruct(expedition: ExpeditionEntity): MorningResult {
        val outcome = requireNotNull(sleepDao.outcome(expedition.sleepSessionId)) { "Resolved expedition has no night outcome" }
        val path = expeditionDao.path(expedition.id)
        val rewards = expeditionDao.rewards(expedition.id)
        val events = petDao.eventsForSource(expedition.id).associateBy { it.type }
        val currentPet = petDao.pet()
        fun eventAmount(type: String, fallback: Int): Int = events[type]?.amount ?: fallback
        val previousLevel = eventAmount("LEVEL_BEFORE", currentPet?.level ?: 1)
        val newLevel = eventAmount("LEVEL_AFTER", previousLevel)
        val statChanges = listOf(
            MorningStatChange("ENERGY", eventAmount("ENERGY_BEFORE", currentPet?.energy ?: 0), eventAmount("ENERGY_AFTER", currentPet?.energy ?: 0)),
            MorningStatChange("FOCUS", eventAmount("FOCUS_BEFORE", currentPet?.focus ?: 0), eventAmount("FOCUS_AFTER", currentPet?.focus ?: 0)),
            MorningStatChange("RESILIENCE", eventAmount("RESILIENCE_BEFORE", currentPet?.resilience ?: 0), eventAmount("RESILIENCE_AFTER", currentPet?.resilience ?: 0)),
        )
        val discoveredIds = worldDao.discoveriesForSource(expedition.id).mapTo(mutableSetOf()) { it.discoveryId }
        val pathDiscoveries = path.filter { stableId("expedition-discovery", it.nodeId) in discoveredIds }.map { it.nodeId }
        val hearthGain = worldDao.hearthEventsForSourceTree(expedition.id).sumOf { it.amount }
        return MorningResult(
            expedition.id,
            expedition.regionId,
            expedition.reachBand ?: outcome.reachBand,
            outcome.xpGranted,
            rewards.mapNotNull { reward -> reward.contentId?.let { it to reward.quantity } },
            path.map { it.nodeId },
            path.map { MorningJourneyStep(it.nodeId, it.nodeType, content.expeditionNarrative[it.outcomeTextKey] ?: it.outcomeTextKey) },
            previousLevel,
            newLevel,
            events["DREAM_SPARKS_GRANTED"]?.amount ?: 0,
            statChanges,
            pathDiscoveries,
            hearthGain,
        )
    }

    private fun chooseRegion(petLevel: Int, reachBand: Int) = content.regions
        .filter { petLevel >= it.unlockLevel }
        .sortedBy { it.unlockLevel }
        .let { unlocked -> if (reachBand >= 3) unlocked.lastOrNull() else unlocked.firstOrNull() }
        ?: error("No expedition region is unlocked")

    private fun com.zleeper.sleepapp.data.content.ProgressionRulesDefinition.toDomain() = ProgressionRules(
        version = version,
        baseParticipationXp = baseParticipationXp,
        durationWeight = durationWeight,
        timingWeight = timingWeight,
        consistencyWeight = consistencyWeight,
        windDownWeight = windDownWeight,
        reflectionWeight = reflectionWeight,
        dreamSparksPerLevel = dreamSparksPerLevel,
        levelCurve = LevelCurve(levelCurve.base, levelCurve.linear, levelCurve.exponent),
        reachThresholds = ReachThresholds(expeditionDepthRules.band1, expeditionDepthRules.band2, expeditionDepthRules.band3, expeditionDepthRules.band4, expeditionDepthRules.band5),
    )

    private fun com.zleeper.sleepapp.data.local.database.SleepSessionEntity.toDomainResolved() = ResolvedSleepSession(
        id, sessionStartEpochMs, requireNotNull(sessionEndEpochMs), requireNotNull(estimatedSleepStartEpochMs), requireNotNull(estimatedSleepEndEpochMs),
        targetSleepMinutes, targetWakeMinutes, requireNotNull(estimatedSleepMinutes), requireNotNull(timingOffsetMinutes), windDownCompleted,
        SleepResolutionMethod.valueOf(requireNotNull(resolutionMethod)), SleepConfidence.valueOf(requireNotNull(confidence)), requireNotNull(finalizedAtEpochMs),
    )

    private fun minutesOfDay(epochMs: Long): Int = Calendar.getInstance().apply { timeInMillis = epochMs }.let {
        it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
    }
}
