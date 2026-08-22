package com.zleeper.sleepapp.feature.shell

import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.*
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import com.zleeper.sleepapp.data.repository.GameEffectsService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*

@Singleton
class ZleeperStateStore @Inject constructor(
    settingsRepository: SettingsRepository,
    petDao: PetDao,
    sleepDao: SleepDao,
    expeditionDao: ExpeditionDao,
    inventoryDao: InventoryDao,
    worldDao: WorldDao,
    questDao: QuestDao,
    morningDao: MorningDao,
    content: GameContentRepository,
    gameEffectsService: GameEffectsService,
) {
    val state: Flow<ZleeperUiState> = combine(
        settingsRepository.settings,
        petDao.observePet(),
        sleepDao.sessions(),
        expeditionDao.expeditions(),
        gameEffectsService.observe(),
    ) { settings, pet, sessions, expeditions, effects ->
        ZleeperUiState(
            settings = settings,
            pet = pet,
            sessions = sessions,
            expeditions = expeditions,
            regions = content.regions,
            scenes = content.scenes,
            forms = content.forms,
            items = content.items,
            quests = content.quests,
            dialogue = content.dialogue,
            equipmentEffects = content.equipmentEffects,
            specializations = content.specializations,
            hearthStages = content.hearthStages,
            craftingRecipes = content.craftingRecipes,
            equipmentInfusions = content.equipmentInfusions,
            equipmentTraits = content.equipmentTraits,
            titleDefinitions = content.titles,
            progressionRules = content.progressionRules,
            gameEffects = effects,
            equipmentMaxUpgradeLevel = requireNotNull(content.progressionRules.equipmentProgression).maxUpgradeLevel,
            contentErrors = content.validate(),
        )
    }
        .combine(inventoryDao.stacks()) { base, values -> base.copy(inventoryStacks = values) }
        .combine(inventoryDao.instances()) { base, values -> base.copy(inventoryInstances = values) }
        .combine(inventoryDao.equipment()) { base, values -> base.copy(equipmentSlots = values) }
        .combine(worldDao.collection()) { base, values -> base.copy(collectionEntries = values) }
        .combine(questDao.quests()) { base, values -> base.copy(questProgress = values) }
        .combine(questDao.allObjectives()) { base, values -> base.copy(questObjectives = values) }
        .combine(morningDao.notes()) { base, values -> base.copy(morningNotes = values) }
        .combine(petDao.specializations()) { base, values -> base.copy(petSpecializations = values) }
        .combine(petDao.memories()) { base, values -> base.copy(petMemories = values) }
        .combine(worldDao.hearth()) { base, value -> base.copy(hearth = value) }
        .combine(worldDao.sceneCompletions()) { base, values -> base.copy(sceneCompletions = values) }
        .combine(worldDao.titles()) { base, values -> base.copy(titles = values) }
        .combine(worldDao.unlocks()) { base, values -> base.copy(worldUnlocks = values) }
        .combine(worldDao.discoveries()) { base, values -> base.copy(worldDiscoveries = values) }
}
