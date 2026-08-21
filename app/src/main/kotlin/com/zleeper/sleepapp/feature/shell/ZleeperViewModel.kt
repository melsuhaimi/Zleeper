package com.zleeper.sleepapp.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zleeper.sleepapp.data.content.GameContentRepository
import com.zleeper.sleepapp.data.local.database.EquipmentSlotEntity
import com.zleeper.sleepapp.data.local.database.PetDao
import com.zleeper.sleepapp.data.local.database.PetEntity
import com.zleeper.sleepapp.data.local.database.QuestDao
import com.zleeper.sleepapp.data.local.database.QuestObjectiveProgressEntity
import com.zleeper.sleepapp.data.local.database.QuestProgressEntity
import com.zleeper.sleepapp.data.local.database.SleepDao
import com.zleeper.sleepapp.data.local.database.SleepSessionEntity
import com.zleeper.sleepapp.data.local.database.WorldDao
import com.zleeper.sleepapp.data.local.database.WorldUnlockEntity
import com.zleeper.sleepapp.data.local.database.InventoryDao
import com.zleeper.sleepapp.data.local.database.InventoryStackEntity
import com.zleeper.sleepapp.data.local.database.InventoryTransactionEntity
import com.zleeper.sleepapp.data.local.database.InventoryInstanceEntity
import com.zleeper.sleepapp.data.local.database.CollectionEntryEntity
import com.zleeper.sleepapp.data.local.database.WorldDiscoveryEntity
import com.zleeper.sleepapp.data.local.preferences.AppSettings
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import com.zleeper.sleepapp.data.local.preferences.ThemePreference
import com.zleeper.sleepapp.data.local.preferences.MotionPreference
import com.zleeper.sleepapp.data.repository.MorningResult
import com.zleeper.sleepapp.data.repository.NightResolutionService
import com.zleeper.sleepapp.domain.sleep.SleepSessionRepository
import com.zleeper.sleepapp.platform.sleep.SleepSignalSource
import com.zleeper.sleepapp.platform.data.DataControlRepository
import com.zleeper.sleepapp.platform.alarm.WakeAlarmScheduler
import com.zleeper.sleepapp.platform.alarm.WindDownReminderScheduler
import com.zleeper.sleepapp.platform.audio.GameAudioController
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WorldInteractionKind { DIALOGUE, COLLECTION }
data class WorldInteraction(val title: String, val body: String, val kind: WorldInteractionKind)

data class ZleeperUiState(val settings: AppSettings = AppSettings(), val pet: PetEntity? = null, val sessions: List<SleepSessionEntity> = emptyList(), val regions: List<com.zleeper.sleepapp.data.content.RegionDefinition> = emptyList(), val scenes: List<com.zleeper.sleepapp.data.content.SceneDefinition> = emptyList(), val items: List<com.zleeper.sleepapp.data.content.ItemDefinition> = emptyList(), val quests: List<com.zleeper.sleepapp.data.content.QuestDefinition> = emptyList(), val dialogue: Map<String, List<com.zleeper.sleepapp.data.content.DialogueLineDefinition>> = emptyMap(), val inventoryStacks: List<InventoryStackEntity> = emptyList(), val inventoryInstances: List<InventoryInstanceEntity> = emptyList(), val equipmentSlots: List<EquipmentSlotEntity> = emptyList(), val collectionEntries: List<CollectionEntryEntity> = emptyList(), val questProgress: List<QuestProgressEntity> = emptyList(), val contentErrors: List<String> = emptyList(), val operationError: String? = null, val morningResult: MorningResult? = null, val worldInteraction: WorldInteraction? = null) {
    val activeSession get() = sessions.firstOrNull { it.state !in setOf("FINALIZED", "EXPEDITION_RESOLVED", "ABORTED") }
    val pendingReview get() = sessions.firstOrNull { it.state == "REVIEW_PENDING" }
}

@HiltViewModel
class ZleeperViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sleepRepository: SleepSessionRepository,
    private val sleepDao: SleepDao,
    private val petDao: PetDao,
    private val questDao: QuestDao,
    private val inventoryDao: InventoryDao,
    private val worldDao: WorldDao,
    private val content: GameContentRepository,
    private val sleepSignalSource: SleepSignalSource,
    private val nightResolutionService: NightResolutionService,
    private val dataControlRepository: DataControlRepository,
    private val wakeAlarmScheduler: WakeAlarmScheduler,
    private val windDownReminderScheduler: WindDownReminderScheduler,
    private val gameAudioController: GameAudioController,
) : ViewModel() {
    private val operationError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val morningResult = kotlinx.coroutines.flow.MutableStateFlow<MorningResult?>(null)
    private val worldInteraction = kotlinx.coroutines.flow.MutableStateFlow<WorldInteraction?>(null)
    val state: StateFlow<ZleeperUiState> = combine(settingsRepository.settings, petDao.observePet(), sleepDao.sessions(), operationError, morningResult) { settings, pet, sessions, error, result ->
        ZleeperUiState(settings, pet, sessions, content.regions, content.scenes, content.items, content.quests, content.dialogue, contentErrors = content.validate(), operationError = error, morningResult = result)
    }.combine(inventoryDao.stacks()) { base, stacks -> base.copy(inventoryStacks = stacks) }
        .combine(combine(inventoryDao.instances(), inventoryDao.equipment()) { instances, equipment -> instances to equipment }) { base, inventory ->
            base.copy(inventoryInstances = inventory.first, equipmentSlots = inventory.second)
        }
        .combine(combine(worldDao.collection(), questDao.quests()) { collection, quests -> collection to quests }) { base, progress ->
            base.copy(collectionEntries = progress.first, questProgress = progress.second)
        }
        .combine(worldInteraction) { base, interaction -> base.copy(worldInteraction = interaction) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ZleeperUiState())

    fun createPet(name: String) = launchOperation {
        val clean = name.trim().take(24)
        require(clean.length >= 2) { "Pet name must contain at least two characters" }
        if (petDao.pet() == null) {
            val now = System.currentTimeMillis()
            petDao.insert(PetEntity(UUID.randomUUID().toString(), content.species.first().id, clean, content.species.first().initialFormId, 1, 0, 3, 3, 3, 0, 0, 0, now, now))
            content.quests.forEach { quest ->
                questDao.putQuest(QuestProgressEntity(quest.id, if (quest.id == "quest_gentle_beginnings") "ACTIVE" else "LOCKED", now, null, null))
                questDao.putObjectives(quest.objectives.map { QuestObjectiveProgressEntity(quest.id, it.id, 0, it.requiredCount, null, now) })
            }
            com.zleeper.sleepapp.domain.inventory.EquipmentSlot.entries.forEach { inventoryDao.putEquipment(EquipmentSlotEntity(it.name, null, now)) }
            worldDao.unlock(WorldUnlockEntity("region_whispering_grove", "REGION", "onboarding", now))
        }
        settingsRepository.completeOnboarding()
    }

    fun saveSleepPlan(sleep: Int, wake: Int, duration: Int) = launchOperation { settingsRepository.setSleepPlan(sleep, wake, duration) }
    fun beginSleep(windDownComplete: Boolean) = launchOperation {
        sleepRepository.begin(state.value.settings.targetSleepMinutes, state.value.settings.targetWakeMinutes, windDownComplete)
        if (sleepSignalSource.isAvailable()) sleepSignalSource.subscribe()
    }
    fun wake() = launchOperation { state.value.activeSession?.let { sleepRepository.requestWake(it.id, System.currentTimeMillis()) }; sleepSignalSource.unsubscribe() }
    fun finalizeMorning() = launchOperation { state.value.pendingReview?.let { val finalized = sleepRepository.finalize(it.id, null); morningResult.value = nightResolutionService.resolve(finalized.id, state.value.settings.targetDurationMinutes) } }
    fun dismissMorningReveal() { morningResult.value = null }
    fun dismissWorldInteraction() { worldInteraction.value = null }
    fun interactWith(contentId: String, sourceId: String, regionId: String) = launchOperation {
        content.dialogue[contentId]?.let { lines ->
            val line = lines.first()
            worldInteraction.value = WorldInteraction(line.speaker, line.text, WorldInteractionKind.DIALOGUE)
            return@launchOperation
        }
        val item = content.items.firstOrNull { it.id == contentId } ?: return@launchOperation
        val now = System.currentTimeMillis()
        val discoveryId = "$sourceId:$contentId"
        val inserted = worldDao.discover(WorldDiscoveryEntity(discoveryId, regionId, sourceId, now))
        if (inserted == -1L) {
            worldInteraction.value = WorldInteraction(item.name, "You already gathered this discovery. It will return on another trail.", WorldInteractionKind.COLLECTION)
            return@launchOperation
        }
        val previous = inventoryDao.stack(contentId)?.quantity ?: 0
        if (item.stackable) {
            inventoryDao.putStack(InventoryStackEntity(contentId, previous + 1, now))
            inventoryDao.insertTransactions(listOf(InventoryTransactionEntity(UUID.randomUUID().toString(), contentId, null, 1, "WORLD_DISCOVERY", sourceId, now)))
        } else {
            val instanceId = UUID.randomUUID().toString()
            inventoryDao.insertInstances(listOf(InventoryInstanceEntity(instanceId, contentId, now, null)))
            inventoryDao.insertTransactions(listOf(InventoryTransactionEntity(UUID.randomUUID().toString(), contentId, instanceId, 1, "WORLD_DISCOVERY", sourceId, now)))
        }
        val collected = worldDao.collectionEntry(contentId)
        worldDao.putCollection(CollectionEntryEntity(contentId, item.category, (collected?.quantity ?: 0) + 1, collected?.firstDiscoveredAtEpochMs ?: now, now))
        worldInteraction.value = WorldInteraction("Found ${item.name}", item.description, WorldInteractionKind.COLLECTION)
    }
    fun equip(itemId: String) = launchOperation {
        val effect = content.equipmentEffects.firstOrNull { it.itemId == itemId } ?: error("This item cannot be equipped")
        val instance = state.value.inventoryInstances.firstOrNull { it.itemId == itemId }
            ?: run {
                val legacyStack = inventoryDao.stack(itemId)
                require((legacyStack?.quantity ?: 0) > 0) { "This item is not in your inventory" }
                val migratedStack = requireNotNull(legacyStack)
                val now = System.currentTimeMillis()
                InventoryInstanceEntity(UUID.randomUUID().toString(), itemId, now, effect.slot).also {
                    inventoryDao.insertInstances(listOf(it))
                    inventoryDao.putStack(migratedStack.copy(quantity = migratedStack.quantity - 1, updatedAtEpochMs = now))
                }
            }
        state.value.equipmentSlots.firstOrNull { it.slot == effect.slot }?.inventoryInstanceId
            ?.let { previousId -> state.value.inventoryInstances.firstOrNull { it.instanceId == previousId } }
            ?.let { previous -> inventoryDao.updateInstance(previous.copy(equippedSlot = null)) }
        inventoryDao.updateInstance(instance.copy(equippedSlot = effect.slot))
        inventoryDao.putEquipment(EquipmentSlotEntity(effect.slot, instance.instanceId, System.currentTimeMillis()))
    }
    fun renamePet(name: String) = launchOperation { petDao.pet()?.let { petDao.update(it.copy(displayName = name.trim().take(24), updatedAtEpochMs = System.currentTimeMillis())) } }
    fun setAlarmEnabled(value: Boolean) = launchOperation {
        settingsRepository.setAlarmEnabled(value)
        if (value) wakeAlarmScheduler.schedule(nextOccurrence(state.value.settings.targetWakeMinutes)) else wakeAlarmScheduler.cancel()
    }
    fun setWindDownReminderEnabled(value: Boolean) = launchOperation { settingsRepository.setWindDownReminderEnabled(value); if (value) windDownReminderScheduler.schedule(nextOccurrence((state.value.settings.targetSleepMinutes - 60 + 1440) % 1440)) else windDownReminderScheduler.cancel() }
    fun setLargeControls(value: Boolean) = launchOperation { settingsRepository.setLargeControls(value) }
    fun setControlOpacity(value: Float) = launchOperation { settingsRepository.setControlOpacity(value) }
    fun setLeftHandedControls(value: Boolean) = launchOperation { settingsRepository.setLeftHandedControls(value) }
    fun setReducedMotion(value: Boolean) = launchOperation { settingsRepository.setMotion(if (value) MotionPreference.REDUCED else MotionPreference.FULL) }
    fun setTheme(value: ThemePreference) = launchOperation { settingsRepository.setTheme(value) }
    fun setVolume(value: Float) = launchOperation { settingsRepository.setVolume(value); gameAudioController.setVolume(value) }
    fun playSceneAudio(regionId: String) {
        content.regions.firstOrNull { it.id == regionId }?.let { gameAudioController.playLoop(it.ambienceAsset, state.value.settings.soundVolume) }
    }
    fun stopSceneAudio() = gameAudioController.stop()

    override fun onCleared() {
        gameAudioController.stop()
        super.onCleared()
    }
    fun clearError() { operationError.value = null }
    fun exportData(onReady: (Uri) -> Unit) = launchOperation { onReady(dataControlRepository.exportJson()) }
    fun deleteSleepHistory() = launchOperation { sleepSignalSource.unsubscribe(); dataControlRepository.deleteSleepHistory(); morningResult.value = null }
    fun resetGameProgress() = launchOperation { dataControlRepository.resetGameProgress(); worldInteraction.value = null }
    fun deleteAllData() = launchOperation { sleepSignalSource.unsubscribe(); wakeAlarmScheduler.cancel(); windDownReminderScheduler.cancel(); gameAudioController.stop(); dataControlRepository.deleteAllLocalData(); morningResult.value = null; worldInteraction.value = null }

    private fun launchOperation(block: suspend () -> Unit) = viewModelScope.launch { operationError.value = null; runCatching { block() }.onFailure { operationError.value = it.message ?: "The operation could not be completed" } }

    private fun nextOccurrence(minutes: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, minutes / 60); set(java.util.Calendar.MINUTE, minutes % 60); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
        if (calendar.timeInMillis <= System.currentTimeMillis()) calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }
}
