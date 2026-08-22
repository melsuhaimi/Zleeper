package com.zleeper.sleepapp.data.repository

import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.InventoryInstanceEntity
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.domain.equipment.EquipmentRules
import com.zleeper.sleepapp.domain.equipment.EquipmentState
import com.zleeper.sleepapp.domain.equipment.EquipmentProgression
import com.zleeper.sleepapp.domain.equipment.GameEffectMapper
import com.zleeper.sleepapp.domain.equipment.GameEffects
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class GameEffectsService @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val petDao: PetDao,
    private val content: GameContentRepository,
) {
    fun observe(): Flow<GameEffects> = combine(inventoryDao.equipment(), inventoryDao.instances(), petDao.specializations()) { slots, instances, specs ->
        aggregate(slots.mapNotNull { slot -> slot.inventoryInstanceId?.let { id -> instances.firstOrNull { it.instanceId == id } } }, specs.associate { it.nodeId to it.rank })
    }

    suspend fun current(): GameEffects = aggregate(
        inventoryDao.equipmentNow().mapNotNull { slot -> slot.inventoryInstanceId?.let { inventoryDao.instance(it) } },
        petDao.specializationsNow().associate { it.nodeId to it.rank },
    )

    private fun aggregate(equipped: List<InventoryInstanceEntity>, ranks: Map<String, Int>): GameEffects {
        var specialization = GameEffects()
        content.specializations.forEach { node ->
            val rank = ranks[node.id] ?: 0
            if (rank > 0) specialization += GameEffectMapper.one(node.effect, node.amountPerRank * rank)
        }
        val rulesDef = requireNotNull(content.progressionRules.equipmentProgression)
        val rules = EquipmentRules(rulesDef.maxUpgradeLevel, rulesDef.upgradeGrowthPercent, rulesDef.tierGrowthPercent.toDouble())
        var equipment = GameEffects()
        equipped.forEach { instance ->
            val mult = EquipmentProgression.multiplier(EquipmentState(instance.tier, instance.upgradeLevel, instance.infusionId, instance.traitId), rules, specialization.equipmentEffectPercent)
            fun add(effect: String, amount: Int) { equipment += GameEffectMapper.one(effect, (amount * mult).roundToInt().coerceAtLeast(1)) }
            content.equipmentEffects.firstOrNull { it.itemId == instance.itemId }?.let { add(it.effect, it.amount) }
            instance.traitId?.let { id -> content.equipmentTraits.firstOrNull { it.id == id } }?.let { add(it.effect, it.baseAmount) }
            instance.infusionId?.let { id -> content.equipmentInfusions.firstOrNull { it.id == id } }?.let { add(it.effect, it.amount) }
        }
        return equipment + specialization
    }
}
