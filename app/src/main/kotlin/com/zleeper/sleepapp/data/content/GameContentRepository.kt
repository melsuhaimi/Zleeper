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
    val progressionRules by lazy { json.decodeFromString<ProgressionRulesDefinition>(read("progression_rules_v1.json")) }
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

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val stableId = Regex("^[a-z][a-z0-9_]+$")
        val ids = buildList { addAll(species.map { it.id }); addAll(forms.map { it.id }); addAll(items.map { it.id }); addAll(quests.map { it.id }); addAll(regions.map { it.id }); addAll(scenes.map { it.id }); addAll(expeditionNodes.map { it.id }); addAll(lootTables.map { it.id }) }
        ids.filterNot(stableId::matches).forEach { errors += "Invalid stable ID: $it" }
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach { errors += "Duplicate stable ID: $it" }
        val itemIds = items.mapTo(mutableSetOf()) { it.id }
        val npcIds = regions.flatMap { it.npcs }.mapTo(mutableSetOf()) { it.id }
        lootTables.flatMap { it.entries }.filterNot { it.itemId in itemIds }.forEach { errors += "Unknown loot item: ${it.itemId}" }
        equipmentEffects.filterNot { it.itemId in itemIds }.forEach { errors += "Unknown equipment item: ${it.itemId}" }
        dialogue.keys.filterNot { it in npcIds }.forEach { errors += "Dialogue references unknown NPC: $it" }
        val nodeIds = expeditionNodes.mapTo(mutableSetOf()) { it.id }
        expeditionNodes.flatMap { it.next }.filterNot { it in nodeIds }.forEach { errors += "Unknown expedition node: $it" }
        if (forms.any { it.animations.toSet() != REQUIRED_PET_ANIMATIONS }) errors += "Every form must implement the standard animation contract"
        if (manifest.contentPackVersion < 1 || progressionRules.version != manifest.progressionRulesVersion) errors += "Content version mismatch"
        manifest.files.forEach { name -> runCatching { context.assets.open("game/content/$name").close() }.onFailure { errors += "Missing content file: $name" } }
        items.forEach { item -> runCatching { context.assets.open("game/${item.iconAsset}").close() }.onFailure { errors += "Missing item icon: ${item.iconAsset}" } }
        regions.forEach { region -> runCatching { context.assets.open("game/${region.ambienceAsset}").close() }.onFailure { errors += "Missing ambience: ${region.ambienceAsset}" } }
        regions.forEach { region ->
            val key = region.id.removePrefix("region_")
            listOf("bg_far", "bg_mid", "bg_near", "tiles", "props_static", "props_animated", "foreground", "interactables", "collectibles", "vfx").forEach { role ->
                val path = "game/atlas/region/region_${key}_${role}.webp"
                runCatching { context.assets.open(path).close() }.onFailure { errors += "Missing region asset: $path" }
            }
        }
        npcIds.forEach { npcId -> listOf("webp", "json").forEach { extension -> val path = "game/atlas/npc/${npcId}_atlas.$extension"; runCatching { context.assets.open(path).close() }.onFailure { errors += "Missing NPC atlas: $path" } } }
        forms.forEach { form ->
            val speciesKey = form.speciesId.removePrefix("species_")
            val formKey = form.name.lowercase()
            listOf("webp", "json").forEach { extension ->
                val path = "game/atlas/pet/pet_${speciesKey}_${formKey}_atlas.$extension"
                runCatching { context.assets.open(path).close() }.onFailure { errors += "Missing pet atlas: $path" }
            }
        }
        return errors
    }

    companion object {
        val REQUIRED_PET_ANIMATIONS = setOf("idle", "blink", "walk", "run", "jump_start", "jump_loop", "fall", "land", "interact", "sleep", "wake", "celebrate")
    }
}
