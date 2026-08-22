package com.zleeper.sleepapp.data.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class ContentManifest(
    @SerialName("content_pack_version") val contentPackVersion: Int,
    @SerialName("progression_rules_version") val progressionRulesVersion: Int,
    val files: List<String>,
)
@Serializable data class PetSpeciesDefinition(val id: String, val name: String, val description: String, @SerialName("initial_form_id") val initialFormId: String)
@Serializable data class PetFormDefinition(
    val id: String,
    @SerialName("species_id") val speciesId: String,
    val name: String,
    @SerialName("asset_key") val assetKey: String,
    @SerialName("level_gate") val levelGate: Int,
    val animations: List<String>,
)
@Serializable data class ItemDefinition(val id: String, val name: String, val description: String, val category: String, val rarity: String, val stackable: Boolean, @SerialName("icon_asset") val iconAsset: String)
@Serializable data class ObjectiveDefinition(val id: String, val type: String, @SerialName("required_count") val requiredCount: Int, val parameters: Map<String, String> = emptyMap())
@Serializable data class QuestDefinition(
    val id: String,
    val name: String,
    val family: String,
    val description: String,
    @SerialName("prerequisites") val prerequisiteQuests: List<String> = emptyList(),
    @SerialName("abandon_resets_progress") val abandonResetsProgress: Boolean = false,
    val objectives: List<ObjectiveDefinition>,
    val rewards: List<RewardDefinition>,
)
@Serializable data class RewardDefinition(val type: String, val id: String? = null, val quantity: Int)
@Serializable data class EquipmentEffectDefinition(@SerialName("item_id") val itemId: String, val slot: String, val effect: String, val amount: Int)
@Serializable data class DialogueLineDefinition(val id: String, val speaker: String, val text: String)
@Serializable data class NpcDefinition(val id: String, val name: String, val role: String)
@Serializable data class RegionDefinition(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("unlock_level") val unlockLevel: Int,
    val scenes: List<String>,
    val npcs: List<NpcDefinition>,
    @SerialName("expedition_start") val expeditionStart: String,
    val palette: List<String>,
    @SerialName("music_asset") val musicAsset: String,
    @SerialName("ambience_asset") val ambienceAsset: String,
)
@Serializable data class SceneDefinition(val id: String, @SerialName("region_id") val regionId: String, val name: String, @SerialName("world_width") val worldWidth: Float, @SerialName("world_height") val worldHeight: Float, val spawn: PointDefinition, val platforms: List<PlatformDefinition>, val interactables: List<InteractableDefinition>)
@Serializable data class PointDefinition(val x: Float, val y: Float)
@Serializable data class PlatformDefinition(val x: Float, val y: Float, val width: Float, val height: Float)
@Serializable data class InteractableDefinition(val id: String, val type: String, val x: Float, val y: Float, @SerialName("content_id") val contentId: String)
@Serializable data class ExpeditionNodeDefinition(val id: String, @SerialName("region_id") val regionId: String, val type: String, @SerialName("depth_cost") val depthCost: Int, @SerialName("focus_required") val focusRequired: Int = 0, @SerialName("loot_table_id") val lootTableId: String? = null, val next: List<String> = emptyList(), @SerialName("text_key") val textKey: String)
@Serializable data class LootEntryDefinition(@SerialName("item_id") val itemId: String, val weight: Int, val minimum: Int, val maximum: Int)
@Serializable data class LootTableDefinition(val id: String, val rolls: Int, val entries: List<LootEntryDefinition>)

@Serializable data class LevelCurveDefinition(val base: Int, val linear: Int, val exponent: Double)
@Serializable data class StatGrowthRatesDefinition(val energy: Int, val focus: Int, val resilience: Int)
@Serializable data class RarityThresholdsDefinition(val common: Int, val uncommon: Int, val rare: Int, val epic: Int, val mythic: Int)
@Serializable data class ExpeditionDepthRulesDefinition(@SerialName("band_1") val band1: Int, @SerialName("band_2") val band2: Int, @SerialName("band_3") val band3: Int, @SerialName("band_4") val band4: Int, @SerialName("band_5") val band5: Int)
@Serializable data class HearthMemoryRewardsDefinition(@SerialName("finalized_night") val finalizedNight: Int, @SerialName("new_discovery") val newDiscovery: Int, @SerialName("quest_claim") val questClaim: Int, @SerialName("new_collection_entry") val newCollectionEntry: Int)
@Serializable data class EquipmentProgressionRulesDefinition(
    @SerialName("max_upgrade_level") val maxUpgradeLevel: Int,
    @SerialName("tier_growth_percent") val tierGrowthPercent: Int,
    @SerialName("upgrade_growth_percent") val upgradeGrowthPercent: Double,
    @SerialName("enhance_base_cost") val enhanceBaseCost: Int,
    @SerialName("refine_base_cost") val refineBaseCost: Int,
    @SerialName("quality_minimum") val qualityMinimum: Int,
    @SerialName("quality_maximum") val qualityMaximum: Int,
    @SerialName("enhance_material_id") val enhanceMaterialId: String,
    @SerialName("refine_material_id") val refineMaterialId: String,
)
@Serializable data class ProgressionRulesDefinition(
    val version: Int,
    @SerialName("base_participation_xp") val baseParticipationXp: Int,
    @SerialName("duration_weight") val durationWeight: Int,
    @SerialName("timing_weight") val timingWeight: Int,
    @SerialName("consistency_weight") val consistencyWeight: Int,
    @SerialName("wind_down_weight") val windDownWeight: Int,
    @SerialName("reflection_weight") val reflectionWeight: Int,
    @SerialName("dream_sparks_per_level") val dreamSparksPerLevel: Int = 0,
    @SerialName("level_curve") val levelCurve: LevelCurveDefinition,
    @SerialName("stat_growth_rates") val statGrowthRates: StatGrowthRatesDefinition,
    @SerialName("rarity_thresholds") val rarityThresholds: RarityThresholdsDefinition,
    @SerialName("expedition_depth_rules") val expeditionDepthRules: ExpeditionDepthRulesDefinition,
    @SerialName("hearth_memory_rewards") val hearthMemoryRewards: HearthMemoryRewardsDefinition? = null,
    @SerialName("equipment_progression") val equipmentProgression: EquipmentProgressionRulesDefinition? = null,
)

@Serializable data class SpecializationNodeDefinition(
    val id: String,
    val path: String,
    val name: String,
    val description: String,
    @SerialName("max_rank") val maxRank: Int,
    @SerialName("cost_per_rank") val costPerRank: Int,
    @SerialName("prerequisites") val prerequisites: List<String> = emptyList(),
    val effect: String,
    @SerialName("amount_per_rank") val amountPerRank: Int,
)
@Serializable data class HearthStageDefinition(val id: String, val order: Int, val name: String, @SerialName("required_memories") val requiredMemories: Int, val features: List<String>)
@Serializable data class CraftingIngredientDefinition(@SerialName("item_id") val itemId: String, val quantity: Int)
@Serializable data class CraftingRecipeDefinition(
    val id: String,
    @SerialName("output_item_id") val outputItemId: String,
    @SerialName("required_level") val minimumLevel: Int,
    @SerialName("ingredients") val costs: List<CraftingIngredientDefinition>,
    @SerialName("compatible_infusions") val allowedInfusions: List<String> = emptyList(),
)
@Serializable data class EquipmentInfusionDefinition(val id: String, val name: String, @SerialName("catalyst_item_id") val catalystItemId: String, @SerialName("catalyst_quantity") val catalystQuantity: Int, val effect: String, val amount: Int)
@Serializable data class EquipmentTraitDefinition(val id: String, val name: String, val effect: String, @SerialName("amount") val baseAmount: Int)
@Serializable data class TitleDefinition(val id: String, val name: String, @SerialName("condition_type") val conditionType: String, @SerialName("condition_value") val conditionValue: Int)
