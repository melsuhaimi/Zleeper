package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.core.id.stableId
import com.zleeper.sleepapp.core.id.stableLong
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.EquipmentProgressionEventEntity
import com.zleeper.sleepapp.data.local.database.EquipmentSlotEntity
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.InventoryInstanceEntity
import com.zleeper.sleepapp.data.local.database.InventoryStackEntity
import com.zleeper.sleepapp.data.local.database.InventoryTransactionEntity
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.domain.equipment.EquipmentProgression
import com.zleeper.sleepapp.domain.equipment.EquipmentRules
import com.zleeper.sleepapp.domain.equipment.EquipmentState
import com.zleeper.sleepapp.domain.equipment.MaterialCost
import com.zleeper.sleepapp.domain.quest.DomainEvent
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class EquipmentService @Inject constructor(
    private val database: ZleeperDatabase,
    private val inventoryDao: InventoryDao,
    private val content: GameContentRepository,
    private val effectsService: GameEffectsService,
    private val questProgressService: QuestProgressService,
    private val worldProgressService: WorldProgressService,
    private val memoryService: PetMemoryService,
) {
    suspend fun synthesize(recipeId: String, infusionId: String? = null): String = database.withTransaction {
        val definition = requireNotNull(content.craftingRecipes.firstOrNull { it.id == recipeId }) { "Unknown crafting recipe" }
        val petLevel = requireNotNull(database.petDao().pet()).level
        require(petLevel >= definition.minimumLevel) { "Companion level ${definition.minimumLevel} is required" }
        require(infusionId == null || infusionId in definition.allowedInfusions) { "Infusion is not compatible with this recipe" }
        val infusion = infusionId?.let { id -> requireNotNull(content.equipmentInfusions.firstOrNull { it.id == id }) }
        val rawCosts = definition.costs.map { MaterialCost(it.itemId, it.quantity) } + listOfNotNull(infusion?.let { MaterialCost(it.catalystItemId, it.catalystQuantity) })
        val effects = effectsService.current()
        val costs = rawCosts.map { it.copy(quantity = EquipmentProgression.discounted(it.quantity, effects.craftingDiscountPercent)) }
        requireMaterials(costs)
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val seed = SecureRandom().nextLong()
        val rules = requireNotNull(content.progressionRules.equipmentProgression)
        val qualitySpan = rules.qualityMaximum - rules.qualityMinimum + 1
        val quality = rules.qualityMinimum + (abs(seed % qualitySpan).toInt())
        val trait = EquipmentProgression.deterministicTrait(definition.outputItemId, infusionId, content.equipmentTraits.map { it.id })
        consumeMaterials(costs, "CRAFT", id, now)
        inventoryDao.insertInstances(listOf(InventoryInstanceEntity(id, definition.outputItemId, now, null, 1, 0, quality, trait, infusionId, seed)))
        inventoryDao.insertTransactions(listOf(InventoryTransactionEntity(stableId("inventory-ledger", id, "craft"), definition.outputItemId, id, 1, "CRAFT", recipeId, now)))
        inventoryDao.insertEquipmentProgressionEvent(EquipmentProgressionEventEntity(stableId("equipment-event", id, "craft"), id, "CRAFT", 0, 0, 1, 0, recipeId, now))
        val item = requireNotNull(content.items.firstOrNull { it.id == definition.outputItemId })
        worldProgressService.recordCollection(item.id, item.category, 1, "craft:$id", now)
        memoryService.remember("CRAFT", id, "thought.crafted_equipment", now)
        id
    }

    suspend fun enhance(instanceId: String) = database.withTransaction {
        val entity = requireNotNull(inventoryDao.instance(instanceId)) { "Equipment instance not found" }
        require(content.equipmentEffects.any { it.itemId == entity.itemId }) { "Only equipment can be enhanced" }
        val definition = requireNotNull(content.progressionRules.equipmentProgression)
        val rules = EquipmentRules(definition.maxUpgradeLevel, definition.upgradeGrowthPercent, definition.tierGrowthPercent.toDouble())
        val current = EquipmentState(entity.tier, entity.upgradeLevel, entity.infusionId, entity.traitId)
        val effects = effectsService.current()
        val base = definition.enhanceBaseCost + entity.tier + entity.upgradeLevel / 3
        val cost = MaterialCost(definition.enhanceMaterialId, EquipmentProgression.discounted(base, effects.craftingDiscountPercent))
        requireMaterials(listOf(cost))
        val now = System.currentTimeMillis()
        consumeMaterials(listOf(cost), "ENHANCE", instanceId, now)
        val next = EquipmentProgression.enhance(current, rules)
        inventoryDao.updateInstance(entity.copy(upgradeLevel = next.enhancement))
        inventoryDao.insertEquipmentProgressionEvent(EquipmentProgressionEventEntity(stableId("equipment-event", instanceId, "enhance", next.enhancement.toString()), instanceId, "ENHANCE", entity.tier, entity.upgradeLevel, next.tier, next.enhancement, instanceId, now))
    }

    suspend fun refine(instanceId: String) = database.withTransaction {
        val entity = requireNotNull(inventoryDao.instance(instanceId)) { "Equipment instance not found" }
        val definition = requireNotNull(content.progressionRules.equipmentProgression)
        val rules = EquipmentRules(definition.maxUpgradeLevel, definition.upgradeGrowthPercent, definition.tierGrowthPercent.toDouble())
        val current = EquipmentState(entity.tier, entity.upgradeLevel, entity.infusionId, entity.traitId)
        val effects = effectsService.current()
        val base = definition.refineBaseCost + entity.tier * 2
        val cost = MaterialCost(definition.refineMaterialId, EquipmentProgression.discounted(base, effects.refinementDiscountPercent))
        requireMaterials(listOf(cost))
        val now = System.currentTimeMillis()
        consumeMaterials(listOf(cost), "REFINE", instanceId, now)
        val next = EquipmentProgression.refine(current, rules)
        inventoryDao.updateInstance(entity.copy(tier = next.tier, upgradeLevel = next.enhancement))
        inventoryDao.insertEquipmentProgressionEvent(EquipmentProgressionEventEntity(stableId("equipment-event", instanceId, "refine", next.tier.toString()), instanceId, "REFINE", entity.tier, entity.upgradeLevel, next.tier, next.enhancement, instanceId, now))
        memoryService.remember("REFINEMENT", "$instanceId:${next.tier}", "thought.refined_equipment", now)
    }

    suspend fun equip(instanceId: String) {
        val event = database.withTransaction {
            val instance = requireNotNull(inventoryDao.instance(instanceId)) { "Equipment instance not found" }
            val effect = requireNotNull(content.equipmentEffects.firstOrNull { it.itemId == instance.itemId }) { "This item cannot be equipped" }
            val now = System.currentTimeMillis()
            inventoryDao.equipmentNow().firstOrNull { it.slot == effect.slot }?.inventoryInstanceId?.let { currentId -> inventoryDao.instance(currentId)?.let { inventoryDao.updateInstance(it.copy(equippedSlot = null)) } }
            inventoryDao.updateInstance(instance.copy(equippedSlot = effect.slot))
            inventoryDao.putEquipment(EquipmentSlotEntity(effect.slot, instanceId, now))
            DomainEvent.EquipmentChanged(instanceId, effect.slot, instance.itemId)
        }
        questProgressService.record(event)
    }

    suspend fun unequip(slot: String) = database.withTransaction {
        val current = inventoryDao.equipmentNow().firstOrNull { it.slot == slot } ?: return@withTransaction
        current.inventoryInstanceId?.let { id -> inventoryDao.instance(id)?.let { inventoryDao.updateInstance(it.copy(equippedSlot = null)) } }
        inventoryDao.putEquipment(current.copy(inventoryInstanceId = null, updatedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun salvage(instanceId: String) = database.withTransaction {
        val entity = requireNotNull(inventoryDao.instance(instanceId)) { "Equipment instance not found" }
        require(entity.equippedSlot == null) { "Unequip equipment before salvaging it" }
        require(content.equipmentEffects.any { it.itemId == entity.itemId }) { "Only equipment can be salvaged" }
        val materialId = requireNotNull(content.progressionRules.equipmentProgression).enhanceMaterialId
        val returned = maxOf(2, 1 + entity.tier + entity.upgradeLevel / 5)
        val now = System.currentTimeMillis()
        val current = inventoryDao.stack(materialId)?.quantity ?: 0
        inventoryDao.putStack(InventoryStackEntity(materialId, current + returned, now))
        inventoryDao.insertTransactions(listOf(
            InventoryTransactionEntity(stableId("inventory-ledger", instanceId, "salvage-remove"), entity.itemId, instanceId, -1, "SALVAGE", instanceId, now),
            InventoryTransactionEntity(stableId("inventory-ledger", instanceId, "salvage-return"), materialId, null, returned, "SALVAGE_RETURN", instanceId, now),
        ))
        inventoryDao.insertEquipmentProgressionEvent(EquipmentProgressionEventEntity(stableId("equipment-event", instanceId, "salvage"), instanceId, "SALVAGE", entity.tier, entity.upgradeLevel, entity.tier, entity.upgradeLevel, instanceId, now))
        inventoryDao.deleteInstance(instanceId)
    }

    private suspend fun requireMaterials(costs: List<MaterialCost>) {
        costs.groupBy { it.itemId }.mapValues { (_, v) -> v.sumOf { it.quantity } }.forEach { (itemId, quantity) ->
            require((inventoryDao.stack(itemId)?.quantity ?: 0) >= quantity) { "Not enough ${content.items.firstOrNull { it.id == itemId }?.name ?: itemId}" }
        }
    }
    private suspend fun consumeMaterials(costs: List<MaterialCost>, reason: String, sourceId: String, now: Long) {
        costs.groupBy { it.itemId }.mapValues { (_, v) -> v.sumOf { it.quantity } }.forEach { (itemId, quantity) ->
            val stack = requireNotNull(inventoryDao.stack(itemId))
            inventoryDao.putStack(stack.copy(quantity = stack.quantity - quantity, updatedAtEpochMs = now))
            inventoryDao.insertTransactions(listOf(InventoryTransactionEntity(stableId("inventory-ledger", sourceId, reason, itemId), itemId, null, -quantity, reason, sourceId, now)))
        }
    }
}
