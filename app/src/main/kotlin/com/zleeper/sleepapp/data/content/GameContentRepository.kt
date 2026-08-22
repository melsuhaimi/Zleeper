package com.zleeper.sleepapp.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Singleton
class GameContentRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val json = Json { ignoreUnknownKeys = false; isLenient = false }
    private fun read(name: String): String = context.assets.open("game/content/$name").bufferedReader().use { it.readText() }

    val manifest by lazy { json.decodeFromString<ContentManifest>(read("content_manifest.json")) }
    val progressionRules by lazy { json.decodeFromString<ProgressionRulesDefinition>(read("progression_rules_v2.json")) }
    val species by lazy { json.decodeFromString<List<PetSpeciesDefinition>>(read("pet_species.json")) }
    val forms by lazy { json.decodeFromString<List<PetFormDefinition>>(read("pet_forms.json")) }
    val items by lazy { json.decodeFromString<List<ItemDefinition>>(read("items.json")) }
    val equipmentEffects by lazy { json.decodeFromString<List<EquipmentEffectDefinition>>(read("equipment_effects.json")) }
    val quests by lazy { json.decodeFromString<List<QuestDefinition>>(read("quests.json")) }
    val regions by lazy { json.decodeFromString<List<RegionDefinition>>(read("regions.json")) }
    val scenes by lazy { json.decodeFromString<List<SceneDefinition>>(read("scenes.json")) }
    val expeditionNodes by lazy { json.decodeFromString<List<ExpeditionNodeDefinition>>(read("expedition_nodes.json")) }
    val lootTables by lazy { json.decodeFromString<List<LootTableDefinition>>(read("loot_tables.json")) }
    val dialogue by lazy { json.decodeFromString<Map<String, List<DialogueLineDefinition>>>(read("dialogue/npcs.json")) }
    val specializations by lazy { json.decodeFromString<List<SpecializationNodeDefinition>>(read("specializations.json")) }
    val hearthStages by lazy { json.decodeFromString<List<HearthStageDefinition>>(read("hearth_stages.json")) }
    val craftingRecipes by lazy { json.decodeFromString<List<CraftingRecipeDefinition>>(read("crafting_recipes.json")) }
    val equipmentInfusions by lazy { json.decodeFromString<List<EquipmentInfusionDefinition>>(read("equipment_infusions.json")) }
    val equipmentTraits by lazy { json.decodeFromString<List<EquipmentTraitDefinition>>(read("equipment_traits.json")) }
    val titles by lazy { json.decodeFromString<List<TitleDefinition>>(read("titles.json")) }
    val expeditionNarrative by lazy { json.decodeFromString<Map<String, String>>(read("expedition_narrative.json")) }

    val validationErrors: List<String> by lazy { validateNow() }
    fun validate(): List<String> = validationErrors

    private fun validateNow(): List<String> {
        val errors = mutableListOf<String>()
        val stableId = Regex("^[a-z][a-z0-9_]+$")
        val allIds = buildList {
            addAll(species.map { it.id }); addAll(forms.map { it.id }); addAll(items.map { it.id }); addAll(quests.map { it.id })
            addAll(regions.map { it.id }); addAll(scenes.map { it.id }); addAll(expeditionNodes.map { it.id }); addAll(lootTables.map { it.id })
            addAll(specializations.map { it.id }); addAll(hearthStages.map { it.id }); addAll(craftingRecipes.map { it.id })
            addAll(equipmentInfusions.map { it.id }); addAll(equipmentTraits.map { it.id }); addAll(titles.map { it.id })
        }
        allIds.filterNot(stableId::matches).forEach { errors += "Invalid stable ID: $it" }
        allIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { errors += "Duplicate stable ID: $it" }

        if (manifest.contentPackVersion != 2 || manifest.progressionRulesVersion != progressionRules.version) errors += "Content version mismatch"
        if (progressionRules.version != 2) errors += "Progression rules v2 must have version 2"
        if (progressionRules.dreamSparksPerLevel <= 0) errors += "Dream Spark rate must be positive"
        if (progressionRules.levelCurve.base <= 0 || progressionRules.levelCurve.linear < 0 || progressionRules.levelCurve.exponent <= 0.0) errors += "Invalid level curve"
        val growth = progressionRules.statGrowthRates
        if (growth.energy <= 0 || growth.focus <= 0 || growth.resilience <= 0) errors += "Stat affinity growth thresholds must be positive"
        val bands = progressionRules.expeditionDepthRules.let { listOf(it.band1, it.band2, it.band3, it.band4, it.band5) }
        if (bands.any { it <= 0 } || bands.zipWithNext().any { (a, b) -> a >= b }) errors += "Expedition reach thresholds must be strictly increasing"
        val rarity = progressionRules.rarityThresholds.let { listOf(it.common, it.uncommon, it.rare, it.epic, it.mythic) }
        if (rarity.any { it < 0 } || rarity.sum() != 100) errors += "Rarity thresholds must be non-negative weights totaling 100"
        progressionRules.equipmentProgression?.let { rules ->
            if (rules.maxUpgradeLevel != 10 || rules.tierGrowthPercent <= 0 || rules.upgradeGrowthPercent <= 0.0) errors += "Invalid equipment growth rules"
            if (rules.enhanceBaseCost <= 0 || rules.refineBaseCost <= 0) errors += "Equipment material costs must be positive"
            if (rules.qualityMinimum !in 1..100 || rules.qualityMaximum !in rules.qualityMinimum..100) errors += "Invalid equipment quality bounds"
        } ?: run { errors += "Missing equipment progression rules" }
        progressionRules.hearthMemoryRewards ?: run { errors += "Missing Hearth memory reward rules" }

        val itemIds = items.mapTo(mutableSetOf()) { it.id }
        val questIds = quests.mapTo(mutableSetOf()) { it.id }
        val regionIds = regions.mapTo(mutableSetOf()) { it.id }
        val sceneIds = scenes.mapTo(mutableSetOf()) { it.id }
        val nodeIds = expeditionNodes.mapTo(mutableSetOf()) { it.id }
        val lootIds = lootTables.mapTo(mutableSetOf()) { it.id }
        val npcIds = regions.flatMap { it.npcs }.mapTo(mutableSetOf()) { it.id }
        val specIds = specializations.mapTo(mutableSetOf()) { it.id }
        val infusionIds = equipmentInfusions.mapTo(mutableSetOf()) { it.id }
        val supportedEffects = setOf(
            "forage_bonus", "interaction_reveal", "material_capacity", "focus_route_bonus", "discovery_bonus",
            "expedition_depth_bonus", "expedition_budget", "focus_bonus", "resilience_bonus", "coyote_time_ms",
            "equipment_effect_percent", "crafting_discount_percent", "refinement_discount_percent",
        )
        val supportedObjectives = setOf(
            "SLEEP_WITHIN_TARGET_WINDOW", "COMPLETE_WIND_DOWN", "FINALIZE_SLEEP_SESSION", "COMPLETE_MORNING_REVIEW", "ACCUMULATE_CONSISTENCY",
            "REACH_PET_LEVEL", "REACH_PET_STAT", "ENTER_REGION", "COMPLETE_SCENE", "COLLECT_ITEM", "EQUIP_ITEM", "TALK_TO_NPC",
            "INTERACT_WITH_OBJECT", "DISCOVER_NODE", "COMPLETE_EXPEDITION",
        )
        val supportedRewards = setOf("ITEM", "XP", "HEARTH_MEMORY")
        val supportedQuestFamilies = setOf("BEHAVIOR", "WORLD", "HYBRID")

        forms.forEach { form ->
            if (form.speciesId !in species.map { it.id }) errors += "Unknown pet species: ${form.speciesId}"
            if (form.assetKey.isBlank() || !stableId.matches(form.assetKey)) errors += "Invalid pet asset key: ${form.assetKey}"
            if (form.animations.toSet() != REQUIRED_PET_ANIMATIONS) errors += "Pet form ${form.id} does not implement the animation contract"
        }
        species.forEach { if (it.initialFormId !in forms.map { f -> f.id }) errors += "Unknown initial pet form: ${it.initialFormId}" }

        equipmentEffects.forEach {
            if (it.itemId !in itemIds) errors += "Unknown equipment item: ${it.itemId}"
            if (it.effect !in supportedEffects) errors += "Unsupported equipment effect: ${it.effect}"
        }
        lootTables.forEach { table ->
            if (table.rolls <= 0) errors += "Loot table ${table.id} has no rolls"
            table.entries.forEach { entry ->
                if (entry.itemId !in itemIds) errors += "Unknown loot item: ${entry.itemId}"
                if (entry.weight <= 0 || entry.minimum <= 0 || entry.maximum < entry.minimum) errors += "Invalid loot entry ${table.id}:${entry.itemId}"
            }
        }
        expeditionNodes.forEach { node ->
            if (node.regionId !in regionIds) errors += "Unknown expedition region: ${node.regionId}"
            if (node.depthCost < 0 || node.focusRequired < 0) errors += "Invalid expedition node cost: ${node.id}"
            node.next.filterNot { it in nodeIds }.forEach { errors += "Unknown expedition node: $it" }
            node.lootTableId?.takeIf { it !in lootIds }?.let { errors += "Unknown loot table: $it" }
            if (node.textKey !in expeditionNarrative) errors += "Missing expedition narrative: ${node.textKey}"
        }
        regions.forEach { region ->
            region.scenes.filterNot { it in sceneIds }.forEach { errors += "Unknown region scene: $it" }
            if (region.expeditionStart !in nodeIds) errors += "Unknown expedition start: ${region.expeditionStart}"
        }
        scenes.forEach { scene ->
            if (scene.regionId !in regionIds) errors += "Unknown scene region: ${scene.regionId}"
            scene.interactables.forEach { interactable ->
                when (interactable.type) {
                    "NPC" -> if (interactable.contentId !in npcIds) errors += "Unknown NPC interactable: ${interactable.contentId}"
                    "ITEM", "COLLECTIBLE" -> if (interactable.contentId !in itemIds) errors += "Unknown item interactable: ${interactable.contentId}"
                    else -> errors += "Unsupported interactable type: ${interactable.type}"
                }
            }
        }
        dialogue.keys.filterNot { it in npcIds }.forEach { errors += "Dialogue references unknown NPC: $it" }

        quests.forEach { quest ->
            if (quest.family !in supportedQuestFamilies) errors += "Unsupported quest family: ${quest.family}"
            quest.prerequisiteQuests.filterNot { it in questIds }.forEach { errors += "Unknown quest prerequisite: $it" }
            quest.objectives.forEach { objective ->
                if (objective.requiredCount <= 0) errors += "Quest objective ${objective.id} requires a positive count"
                if (objective.type !in supportedObjectives) errors += "Unsupported quest objective type: ${objective.type}"
                objective.parameters["window_nights"]?.toIntOrNull()?.let { if (it <= 0) errors += "Invalid quest window: ${objective.id}" }
                objective.parameters["item_id"]?.takeIf { it !in itemIds }?.let { errors += "Unknown quest item: $it" }
                objective.parameters["npc_id"]?.takeIf { it !in npcIds }?.let { errors += "Unknown quest NPC: $it" }
                objective.parameters["region_id"]?.takeIf { it !in regionIds }?.let { errors += "Unknown quest region: $it" }
                objective.parameters["scene_id"]?.takeIf { it !in sceneIds }?.let { errors += "Unknown quest scene: $it" }
            }
            quest.rewards.forEach { reward ->
                if (reward.quantity <= 0) errors += "Quest reward quantity must be positive: ${quest.id}"
                if (reward.type !in supportedRewards) errors += "Unsupported quest reward type: ${reward.type}"
                if (reward.type == "ITEM" && reward.id !in itemIds) errors += "Unknown quest reward item: ${reward.id}"
            }
        }
        detectCycles(quests.associate { it.id to it.prerequisiteQuests }, "quest", errors)

        specializations.forEach { node ->
            if (node.maxRank <= 0 || node.costPerRank <= 0) errors += "Invalid specialization rank/cost: ${node.id}"
            if (node.effect !in supportedEffects) errors += "Unsupported specialization effect: ${node.effect}"
            node.prerequisites.filterNot { it in specIds }.forEach { errors += "Unknown specialization prerequisite: $it" }
        }
        detectCycles(specializations.associate { it.id to it.prerequisites }, "specialization", errors)

        if (hearthStages.isEmpty() || hearthStages.sortedBy { it.order }.map { it.order } != (1..hearthStages.size).toList()) errors += "Hearth stage order must be contiguous"
        if (hearthStages.sortedBy { it.order }.zipWithNext().any { (a, b) -> a.requiredMemories >= b.requiredMemories }) errors += "Hearth memory thresholds must strictly increase"

        craftingRecipes.forEach { recipe ->
            if (recipe.outputItemId !in itemIds) errors += "Unknown recipe output: ${recipe.outputItemId}"
            if (recipe.minimumLevel <= 0 || recipe.costs.isEmpty()) errors += "Invalid recipe requirements: ${recipe.id}"
            recipe.costs.forEach { cost ->
                if (cost.itemId !in itemIds) errors += "Unknown recipe ingredient: ${cost.itemId}"
                if (cost.quantity <= 0) errors += "Invalid recipe ingredient quantity: ${recipe.id}:${cost.itemId}"
            }
            recipe.allowedInfusions.filterNot { it in infusionIds }.forEach { errors += "Unknown recipe infusion: $it" }
        }
        equipmentInfusions.forEach { infusion ->
            if (infusion.catalystItemId !in itemIds) errors += "Unknown infusion catalyst: ${infusion.catalystItemId}"
            if (infusion.catalystQuantity <= 0 || infusion.amount <= 0) errors += "Invalid infusion: ${infusion.id}"
            if (infusion.effect !in supportedEffects) errors += "Unsupported infusion effect: ${infusion.effect}"
        }
        equipmentTraits.forEach { trait ->
            if (trait.baseAmount <= 0) errors += "Invalid equipment trait: ${trait.id}"
            if (trait.effect !in supportedEffects) errors += "Unsupported trait effect: ${trait.effect}"
        }
        val supportedTitleConditions = setOf("MORNING_REVEALS", "DISCOVERIES", "CRAFTS", "HEARTH_STAGE_ORDER")
        titles.forEach { title ->
            if (title.conditionType !in supportedTitleConditions || title.conditionValue <= 0) errors += "Invalid title condition: ${title.id}"
        }

        manifest.files.forEach { name -> runCatching { context.assets.open("game/content/$name").close() }.onFailure { errors += "Missing content file: $name" } }
        items.forEach { item -> runCatching { context.assets.open("game/${item.iconAsset}").close() }.onFailure { errors += "Missing item icon: ${item.iconAsset}" } }
        regions.forEach { region ->
            listOf(region.musicAsset, region.ambienceAsset).forEach { asset -> runCatching { context.assets.open("game/$asset").close() }.onFailure { errors += "Missing region audio: $asset" } }
            val key = region.id.removePrefix("region_")
            listOf("bg_far", "bg_mid", "bg_near", "tiles", "props_static", "props_animated", "foreground", "interactables", "collectibles", "vfx").forEach { role ->
                val path = "game/atlas/region/region_${key}_${role}.webp"
                runCatching { context.assets.open(path).close() }.onFailure { errors += "Missing region asset: $path" }
            }
        }
        npcIds.forEach { npcId -> listOf("webp", "json").forEach { ext -> val path = "game/atlas/npc/${npcId}_atlas.$ext"; runCatching { context.assets.open(path).close() }.onFailure { errors += "Missing NPC atlas: $path" } } }
        forms.forEach { form -> listOf("webp", "json").forEach { ext -> val path = "game/atlas/pet/pet_moonmoth_${form.assetKey}_atlas.$ext"; runCatching { context.assets.open(path).close() }.onFailure { errors += "Missing pet atlas: $path" } } }
        return errors.distinct()
    }

    private fun detectCycles(graph: Map<String, List<String>>, label: String, errors: MutableList<String>) {
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String): Boolean {
            if (id in visiting) return true
            if (id in visited) return false
            visiting += id
            if (graph[id].orEmpty().any(::visit)) return true
            visiting -= id
            visited += id
            return false
        }
        graph.keys.forEach { if (visit(it)) errors += "Cycle detected in $label prerequisites" }
    }

    companion object {
        val REQUIRED_PET_ANIMATIONS = setOf("idle", "blink", "walk", "run", "jump_start", "jump_loop", "fall", "land", "interact", "sleep", "wake", "celebrate")
    }
}
