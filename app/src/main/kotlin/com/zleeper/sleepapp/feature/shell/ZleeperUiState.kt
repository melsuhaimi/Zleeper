package com.zleeper.sleepapp.feature.shell

import com.zleeper.sleepapp.data.content.*
import com.zleeper.sleepapp.data.local.database.*
import com.zleeper.sleepapp.data.local.preferences.AppSettings
import com.zleeper.sleepapp.data.repository.MorningResult
import com.zleeper.sleepapp.domain.equipment.GameEffects

enum class WorldInteractionKind { DIALOGUE, COLLECTION }

data class WorldInteraction(
    val title: String,
    val body: String,
    val kind: WorldInteractionKind,
)

data class ZleeperUiState(
    val settings: AppSettings = AppSettings(),
    val pet: PetEntity? = null,
    val sessions: List<SleepSessionEntity> = emptyList(),
    val expeditions: List<ExpeditionEntity> = emptyList(),
    val regions: List<RegionDefinition> = emptyList(),
    val scenes: List<SceneDefinition> = emptyList(),
    val forms: List<PetFormDefinition> = emptyList(),
    val items: List<ItemDefinition> = emptyList(),
    val quests: List<QuestDefinition> = emptyList(),
    val dialogue: Map<String, List<DialogueLineDefinition>> = emptyMap(),
    val equipmentEffects: List<EquipmentEffectDefinition> = emptyList(),
    val specializations: List<SpecializationNodeDefinition> = emptyList(),
    val hearthStages: List<HearthStageDefinition> = emptyList(),
    val craftingRecipes: List<CraftingRecipeDefinition> = emptyList(),
    val equipmentInfusions: List<EquipmentInfusionDefinition> = emptyList(),
    val equipmentTraits: List<EquipmentTraitDefinition> = emptyList(),
    val titleDefinitions: List<TitleDefinition> = emptyList(),
    val progressionRules: ProgressionRulesDefinition? = null,
    val inventoryStacks: List<InventoryStackEntity> = emptyList(),
    val inventoryInstances: List<InventoryInstanceEntity> = emptyList(),
    val equipmentSlots: List<EquipmentSlotEntity> = emptyList(),
    val collectionEntries: List<CollectionEntryEntity> = emptyList(),
    val questProgress: List<QuestProgressEntity> = emptyList(),
    val questObjectives: List<QuestObjectiveProgressEntity> = emptyList(),
    val morningNotes: List<MorningNoteEntity> = emptyList(),
    val petSpecializations: List<PetSpecializationEntity> = emptyList(),
    val petMemories: List<PetMemoryEntity> = emptyList(),
    val hearth: HearthProgressEntity? = null,
    val sceneCompletions: List<WorldSceneCompletionEntity> = emptyList(),
    val titles: List<PlayerTitleEntity> = emptyList(),
    val worldUnlocks: List<WorldUnlockEntity> = emptyList(),
    val worldDiscoveries: List<WorldDiscoveryEntity> = emptyList(),
    val gameEffects: GameEffects = GameEffects(),
    val equipmentMaxUpgradeLevel: Int = 10,
    val contentErrors: List<String> = emptyList(),
    val operationError: String? = null,
    val morningResult: MorningResult? = null,
    val worldInteraction: WorldInteraction? = null,
) {
    val activeSession: SleepSessionEntity?
        get() = sessions.firstOrNull { it.state !in setOf("FINALIZED", "EXPEDITION_RESOLVED", "ABORTED") }
    val trackingSession: SleepSessionEntity?
        get() = sessions.firstOrNull { it.state == "TRACKING" || it.state == "ARMED" }
    val pendingReview: SleepSessionEntity?
        get() = sessions.firstOrNull { it.state == "REVIEW_PENDING" }
    val pendingResolution: SleepSessionEntity?
        get() = sessions.firstOrNull { it.state == "FINALIZED" }
    val pendingReveal: ExpeditionEntity?
        get() = expeditions.firstOrNull { it.status == "RESOLVED" && it.id != settings.lastRevealedExpeditionId }
    val trackedQuest: QuestProgressEntity?
        get() = questProgress.firstOrNull { it.tracked }
}
