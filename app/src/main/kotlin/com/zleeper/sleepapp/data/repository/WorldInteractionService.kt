package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.core.id.stableId
import com.zleeper.sleepapp.core.id.stableLong
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.InventoryInstanceEntity
import com.zleeper.sleepapp.data.local.database.InventoryStackEntity
import com.zleeper.sleepapp.data.local.database.InventoryTransactionEntity
import com.zleeper.sleepapp.data.local.database.WorldDao
import com.zleeper.sleepapp.data.local.database.WorldDiscoveryEntity
import com.zleeper.sleepapp.data.local.database.WorldSceneCompletionEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.domain.quest.DomainEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorldInteractionService @Inject constructor(
    private val database: ZleeperDatabase,
    private val inventoryDao: InventoryDao,
    private val worldDao: WorldDao,
    private val content: GameContentRepository,
    private val questProgressService: QuestProgressService,
    private val worldProgressService: WorldProgressService,
    private val memoryService: PetMemoryService,
    private val titleService: TitleService,
) {
    suspend fun collect(interactableId: String, itemId: String, sceneId: String, regionId: String): Boolean {
        val event = database.withTransaction {
            val item = requireNotNull(content.items.firstOrNull { it.id == itemId })
            val now = System.currentTimeMillis()
            val discoveryId = stableId("world-discovery", sceneId, interactableId)
            if (worldDao.discover(WorldDiscoveryEntity(discoveryId, regionId, sceneId, now)) == -1L) return@withTransaction null
            if (item.stackable) {
                val current = inventoryDao.stack(itemId)?.quantity ?: 0
                inventoryDao.putStack(InventoryStackEntity(itemId, current + 1, now))
                inventoryDao.insertTransactions(listOf(InventoryTransactionEntity(stableId("inventory-ledger", discoveryId, itemId), itemId, null, 1, "WORLD_DISCOVERY", discoveryId, now)))
            } else {
                val instanceId = stableId("world-item", discoveryId, itemId)
                inventoryDao.insertInstances(listOf(InventoryInstanceEntity(instanceId, itemId, now, null, progressionSeed = stableLong(instanceId))))
                inventoryDao.insertTransactions(listOf(InventoryTransactionEntity(stableId("inventory-ledger", instanceId), itemId, instanceId, 1, "WORLD_DISCOVERY", discoveryId, now)))
            }
            worldProgressService.recordCollection(itemId, item.category, 1, discoveryId, now)
            worldProgressService.addHearthMemories(content.progressionRules.hearthMemoryRewards?.newDiscovery ?: 0, "NEW_DISCOVERY", discoveryId, now)
            memoryService.remember("WORLD_DISCOVERY", discoveryId, "thought.recent_discovery", now, itemId)
            DomainEvent.ItemGranted(discoveryId, itemId, 1)
        } ?: return false
        questProgressService.record(event)
        titleService.evaluateAll()
        return true
    }

    suspend fun talkToNpc(npcId: String, sceneId: String) {
        val now = System.currentTimeMillis()
        questProgressService.record(DomainEvent.TalkToNpc(sceneId, npcId), now)
        memoryService.remember("NPC_MEETING", "$sceneId:$npcId", "thought.npc_meeting", now, npcId)
    }

    suspend fun interactWithObject(objectId: String, sceneId: String) = questProgressService.record(DomainEvent.InteractWithObject(sceneId, objectId))

    suspend fun enterRegion(regionId: String) = questProgressService.record(DomainEvent.EnterRegion("region:$regionId", regionId))

    suspend fun completeScene(sceneId: String, regionId: String) {
        val event = database.withTransaction {
            val now = System.currentTimeMillis()
            val existing = worldDao.sceneCompletion(sceneId)
            worldDao.putSceneCompletion(WorldSceneCompletionEntity(sceneId, regionId, (existing?.completionCount ?: 0) + 1, existing?.firstCompletedAtEpochMs ?: now, now))
            val discoveryId = stableId("scene-completion", sceneId)
            if (worldDao.discover(WorldDiscoveryEntity(discoveryId, regionId, sceneId, now)) != -1L) {
                worldProgressService.recordCollection(sceneId, "PLACE", 1, discoveryId, now)
                worldProgressService.addHearthMemories(content.progressionRules.hearthMemoryRewards?.newDiscovery ?: 0, "SCENE_COMPLETION", sceneId, now)
            }
            DomainEvent.SceneCompleted(sceneId, sceneId)
        }
        questProgressService.record(event)
        titleService.evaluateAll()
    }
}
