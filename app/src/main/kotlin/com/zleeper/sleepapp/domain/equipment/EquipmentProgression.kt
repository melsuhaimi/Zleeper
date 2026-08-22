package com.zleeper.sleepapp.domain.equipment

import kotlin.math.ceil

data class EquipmentState(val tier: Int = 1, val enhancement: Int = 0, val infusionId: String? = null, val traitId: String? = null)
data class EquipmentRules(val maxEnhancement: Int = 10, val enhancementScalePercent: Double = 4.5, val tierScalePercent: Double = 35.0)
data class MaterialCost(val itemId: String, val quantity: Int)

object EquipmentProgression {
    fun multiplier(state: EquipmentState, rules: EquipmentRules = EquipmentRules(), specializationPercent: Int = 0): Double {
        require(state.tier >= 1 && state.enhancement in 0..rules.maxEnhancement)
        val tier = 1.0 + (state.tier - 1) * (rules.tierScalePercent / 100.0)
        val enhancement = 1.0 + state.enhancement * (rules.enhancementScalePercent / 100.0)
        val specialization = 1.0 + specializationPercent.coerceAtLeast(0) / 100.0
        return tier * enhancement * specialization
    }

    fun discounted(quantity: Int, discountPercent: Int): Int = ceil(quantity * (1.0 - discountPercent.coerceIn(0, 80) / 100.0)).toInt().coerceAtLeast(1)

    fun enhancementCost(state: EquipmentState, materialId: String, discountPercent: Int = 0, rules: EquipmentRules = EquipmentRules()): MaterialCost {
        require(state.enhancement < rules.maxEnhancement)
        val base = 1 + state.tier + (state.enhancement / 3)
        return MaterialCost(materialId, discounted(base, discountPercent))
    }

    fun refinementCost(state: EquipmentState, materialId: String, discountPercent: Int = 0, rules: EquipmentRules = EquipmentRules()): MaterialCost {
        require(state.enhancement == rules.maxEnhancement)
        return MaterialCost(materialId, discounted(4 + state.tier * 2, discountPercent))
    }

    fun enhance(state: EquipmentState, rules: EquipmentRules = EquipmentRules()): EquipmentState {
        require(state.enhancement < rules.maxEnhancement)
        return state.copy(enhancement = state.enhancement + 1)
    }

    fun refine(state: EquipmentState, rules: EquipmentRules = EquipmentRules()): EquipmentState {
        require(state.enhancement == rules.maxEnhancement)
        return state.copy(tier = state.tier + 1, enhancement = 0)
    }

    fun deterministicTrait(outputItemId: String, infusionId: String?, traitIds: List<String>): String? {
        if (traitIds.isEmpty()) return null
        val hash = (outputItemId + ":" + infusionId.orEmpty()).hashCode().toLong().let { if (it < 0) -it else it }
        return traitIds[(hash % traitIds.size).toInt()]
    }
}
