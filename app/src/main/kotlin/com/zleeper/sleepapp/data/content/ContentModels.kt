package com.zleeper.sleepapp.data.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class ContentManifest(@SerialName("content_pack_version") val contentPackVersion: Int, @SerialName("progression_rules_version") val progressionRulesVersion: Int, val files: List<String>)
@Serializable data class PetSpeciesDefinition(val id: String, val name: String, val description: String, @SerialName("initial_form_id") val initialFormId: String)
@Serializable data class PetFormDefinition(val id: String, @SerialName("species_id") val speciesId: String, val name: String, @SerialName("level_gate") val levelGate: Int, val animations: List<String>)
@Serializable data class ItemDefinition(val id: String, val name: String, val description: String, val category: String, val rarity: String, val stackable: Boolean, @SerialName("icon_asset") val iconAsset: String)
@Serializable data class ObjectiveDefinition(val id: String, val type: String, @SerialName("required_count") val requiredCount: Int, val parameters: Map<String, String> = emptyMap())
@Serializable data class QuestDefinition(val id: String, val name: String, val family: String, val description: String, val objectives: List<ObjectiveDefinition>, val rewards: List<RewardDefinition>)
@Serializable data class RewardDefinition(val type: String, val id: String? = null, val quantity: Int)
@Serializable data class EquipmentEffectDefinition(@SerialName("item_id") val itemId: String, val slot: String, val effect: String, val amount: Int)
@Serializable data class DialogueLineDefinition(val id: String, val speaker: String, val text: String)
@Serializable data class NpcDefinition(val id: String, val name: String, val role: String)
@Serializable data class RegionDefinition(val id: String, val name: String, val description: String, @SerialName("unlock_level") val unlockLevel: Int, val scenes: List<String>, val npcs: List<NpcDefinition>, @SerialName("expedition_start") val expeditionStart: String, val palette: List<String>, @SerialName("ambience_asset") val ambienceAsset: String)
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
@Serializable data class ProgressionRulesDefinition(val version: Int, @SerialName("base_participation_xp") val baseParticipationXp: Int, @SerialName("duration_weight") val durationWeight: Int, @SerialName("timing_weight") val timingWeight: Int, @SerialName("consistency_weight") val consistencyWeight: Int, @SerialName("wind_down_weight") val windDownWeight: Int, @SerialName("reflection_weight") val reflectionWeight: Int, @SerialName("level_curve") val levelCurve: LevelCurveDefinition, @SerialName("stat_growth_rates") val statGrowthRates: StatGrowthRatesDefinition, @SerialName("rarity_thresholds") val rarityThresholds: RarityThresholdsDefinition, @SerialName("expedition_depth_rules") val expeditionDepthRules: ExpeditionDepthRulesDefinition)
