package com.zleeper.sleepapp.domain.equipment

data class GameEffects(
    val forageBonus: Int = 0,
    val interactionReveal: Int = 0,
    val materialCapacity: Int = 0,
    val focusRouteBonus: Int = 0,
    val discoveryBonus: Int = 0,
    val expeditionDepthBonus: Int = 0,
    val expeditionBudget: Int = 0,
    val focusBonus: Int = 0,
    val resilienceBonus: Int = 0,
    val coyoteTimeMillis: Int = 0,
    val equipmentEffectPercent: Int = 0,
    val craftingDiscountPercent: Int = 0,
    val refinementDiscountPercent: Int = 0,
) {
    operator fun plus(other: GameEffects) = GameEffects(
        forageBonus + other.forageBonus,
        interactionReveal + other.interactionReveal,
        materialCapacity + other.materialCapacity,
        focusRouteBonus + other.focusRouteBonus,
        discoveryBonus + other.discoveryBonus,
        expeditionDepthBonus + other.expeditionDepthBonus,
        expeditionBudget + other.expeditionBudget,
        focusBonus + other.focusBonus,
        resilienceBonus + other.resilienceBonus,
        coyoteTimeMillis + other.coyoteTimeMillis,
        equipmentEffectPercent + other.equipmentEffectPercent,
        craftingDiscountPercent + other.craftingDiscountPercent,
        refinementDiscountPercent + other.refinementDiscountPercent,
    )

    companion object {
        val SUPPORTED_EFFECTS = setOf(
            "forage_bonus", "interaction_reveal", "material_capacity", "focus_route_bonus", "discovery_bonus",
            "expedition_depth_bonus", "expedition_budget", "focus_bonus", "resilience_bonus", "coyote_time_ms",
            "equipment_effect_percent", "crafting_discount_percent", "refinement_discount_percent",
        )
    }
}

object GameEffectMapper {
    fun one(effect: String, amount: Int): GameEffects = when (effect) {
        "forage_bonus" -> GameEffects(forageBonus = amount)
        "interaction_reveal" -> GameEffects(interactionReveal = amount)
        "material_capacity" -> GameEffects(materialCapacity = amount)
        "focus_route_bonus" -> GameEffects(focusRouteBonus = amount)
        "discovery_bonus" -> GameEffects(discoveryBonus = amount)
        "expedition_depth_bonus" -> GameEffects(expeditionDepthBonus = amount)
        "expedition_budget" -> GameEffects(expeditionBudget = amount)
        "focus_bonus" -> GameEffects(focusBonus = amount)
        "resilience_bonus" -> GameEffects(resilienceBonus = amount)
        "coyote_time_ms" -> GameEffects(coyoteTimeMillis = amount)
        "equipment_effect_percent" -> GameEffects(equipmentEffectPercent = amount)
        "crafting_discount_percent" -> GameEffects(craftingDiscountPercent = amount)
        "refinement_discount_percent" -> GameEffects(refinementDiscountPercent = amount)
        else -> error("Unsupported game effect: $effect")
    }
}
