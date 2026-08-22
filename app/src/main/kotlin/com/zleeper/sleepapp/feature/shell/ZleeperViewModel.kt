package com.zleeper.sleepapp.feature.shell

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.zleeper.sleepapp.data.content.*
import com.zleeper.sleepapp.data.local.database.*
import com.zleeper.sleepapp.data.local.preferences.*
import com.zleeper.sleepapp.data.repository.*
import com.zleeper.sleepapp.domain.equipment.GameEffects
import com.zleeper.sleepapp.domain.inventory.EquipmentSlot
import com.zleeper.sleepapp.domain.sleep.*
import com.zleeper.sleepapp.platform.alarm.*
import com.zleeper.sleepapp.platform.audio.GameAudioController
import com.zleeper.sleepapp.platform.data.DataControlRepository
import com.zleeper.sleepapp.platform.sleep.SleepSignalSource
import com.zleeper.sleepapp.platform.work.RewardResolutionScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ZleeperViewModel @Inject constructor(
    private val database: ZleeperDatabase,
    private val settingsRepository: SettingsRepository,
    private val sleepRepository: SleepSessionRepository,
    private val sleepStartService: SleepStartService,
    private val petDao: PetDao,
    private val morningDao: MorningDao,
    private val inventoryDao: InventoryDao,
    private val worldDao: WorldDao,
    private val content: GameContentRepository,
    private val sleepSignalSource: SleepSignalSource,
    private val nightResolutionService: NightResolutionService,
    private val dataControlRepository: DataControlRepository,
    private val wakeAlarmScheduler: WakeAlarmScheduler,
    private val windDownReminderScheduler: WindDownReminderScheduler,
    private val bedtimeReminderScheduler: BedtimeReminderScheduler,
    private val rewardResolutionScheduler: RewardResolutionScheduler,
    private val gameAudioController: GameAudioController,
    private val equipmentService: EquipmentService,
    private val questProgressService: QuestProgressService,
    private val petSpecializationService: PetSpecializationService,
    private val evolutionService: EvolutionService,
    private val titleService: TitleService,
    private val worldInteractionService: WorldInteractionService,
    private val worldProgressService: WorldProgressService,
    private val stateStore: ZleeperStateStore,
) : ViewModel() {
    private val operationError = MutableStateFlow<String?>(null)
    private val morningResult = MutableStateFlow<MorningResult?>(null)
    private val worldInteraction = MutableStateFlow<WorldInteraction?>(null)
    private val dialogueCursors = mutableMapOf<String, Int>()

    val state: StateFlow<ZleeperUiState> = stateStore.state
        .combine(operationError) { base, value -> base.copy(operationError = value) }
        .combine(morningResult) { base, value -> base.copy(morningResult = value) }
        .combine(worldInteraction) { base, value -> base.copy(worldInteraction = value) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ZleeperUiState())

    fun createPet(name: String) = launchOperation {
        val clean = name.trim().take(24)
        require(clean.length >= 2) { "Pet name must contain at least two characters" }
        val species = requireNotNull(content.species.firstOrNull()) { "Pet species content is unavailable" }
        val form = requireNotNull(content.forms.firstOrNull { it.id == species.initialFormId }) { "Initial pet form is unavailable" }
        val now = System.currentTimeMillis()
        val created = database.withTransaction {
            if (petDao.pet() != null) return@withTransaction false
            petDao.insert(
                PetEntity(
                    instanceId = UUID.randomUUID().toString(),
                    speciesId = species.id,
                    displayName = clean,
                    formId = form.id,
                    level = 1,
                    totalXp = 0,
                    energy = 3,
                    focus = 3,
                    resilience = 3,
                    energyAffinity = 0,
                    focusAffinity = 0,
                    resilienceAffinity = 0,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                ),
            )
            EquipmentSlot.entries.forEach { inventoryDao.putEquipment(EquipmentSlotEntity(it.name, null, now)) }
            content.regions.firstOrNull()?.let { worldDao.unlock(WorldUnlockEntity(it.id, "REGION", "onboarding", now)) }
            true
        }
        if (created) {
            worldProgressService.ensureHearth(now)
            worldProgressService.recordCollection(form.id, "PET_FORM", 1, "onboarding:${form.id}", now)
            questProgressService.initialize(now)
            titleService.evaluateAll(now)
        }
        settingsRepository.completeOnboarding()
    }

    fun saveSleepPlan(sleepMinutes: Int, wakeMinutes: Int) = launchOperation {
        saveSleepPlanInternal(sleepMinutes, wakeMinutes)
    }

    fun saveSleepPlan(sleepMinutes: Int, wakeMinutes: Int, displayedDurationMinutes: Int) = launchOperation {
        val derived = SleepSchedule.plannedDurationMinutes(sleepMinutes, wakeMinutes)
        require(displayedDurationMinutes == derived) { "Sleep duration is derived from bedtime and wake time" }
        saveSleepPlanInternal(sleepMinutes, wakeMinutes)
    }

    fun beginSleep(windDownCompleted: Boolean) = launchOperation {
        val settings = state.value.settings
        sleepStartService.begin(settings.targetSleepMinutes, settings.targetWakeMinutes, windDownCompleted)
    }

    fun wake() = launchOperation {
        val session = requireNotNull(state.value.trackingSession) { "No active sleep session" }
        sleepRepository.requestWake(session.id, System.currentTimeMillis())
        sleepSignalSource.unsubscribe()
    }

    fun abortSleep() = launchOperation {
        val session = requireNotNull(state.value.trackingSession) { "No active sleep session" }
        sleepRepository.abort(session.id)
        sleepSignalSource.unsubscribe()
    }

    fun finalizeMorning(
        correctedStartEpochMs: Long? = null,
        correctedEndEpochMs: Long? = null,
        mood: Int? = null,
        note: String = "",
    ) = launchOperation {
        val session = requireNotNull(state.value.pendingReview) { "No morning review is pending" }
        if (mood != null) {
            require(mood in 1..5)
            val now = System.currentTimeMillis()
            val existing = morningDao.note(session.id)
            morningDao.put(
                MorningNoteEntity(
                    id = existing?.id ?: "morning-note:${session.id}",
                    sleepSessionId = session.id,
                    mood = mood,
                    note = note.trim().take(500),
                    createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                    updatedAtEpochMs = now,
                ),
            )
        }
        val correction = if (correctedStartEpochMs != null && correctedEndEpochMs != null) {
            SleepReviewCorrection(correctedStartEpochMs, correctedEndEpochMs)
        } else null
        sleepRepository.finalize(session.id, correction)
        rewardResolutionScheduler.enqueue(session.id)
    }

    fun resumePendingResolution() = launchOperation {
        val session = state.value.pendingResolution
            ?: state.value.pendingReveal?.let { expedition -> state.value.sessions.firstOrNull { it.id == expedition.sleepSessionId } }
            ?: return@launchOperation
        val reflected = morningDao.note(session.id) != null
        morningResult.value = nightResolutionService.resolve(session.id, reflected)
    }

    fun dismissMorningReveal() = launchOperation {
        morningResult.value?.let { settingsRepository.markExpeditionRevealed(it.expeditionId) }
        morningResult.value = null
    }

    fun dismissWorldInteraction() { worldInteraction.value = null }

    fun enterRegion(regionId: String) = launchOperation { worldInteractionService.enterRegion(regionId) }

    fun interactWith(contentId: String, sceneId: String, regionId: String) = launchOperation {
        val scene = requireNotNull(content.scenes.firstOrNull { it.id == sceneId }) { "Scene content is unavailable" }
        val interactable = requireNotNull(scene.interactables.firstOrNull { it.contentId == contentId }) { "Interaction is unavailable" }
        if (interactable.type == "NPC") {
            worldInteractionService.talkToNpc(contentId, sceneId)
            val lines = content.dialogue[contentId].orEmpty()
            if (lines.isNotEmpty()) {
                val cursor = dialogueCursors[contentId] ?: 0
                val line = lines[cursor % lines.size]
                dialogueCursors[contentId] = (cursor + 1) % lines.size
                worldInteraction.value = WorldInteraction(line.speaker, line.text, WorldInteractionKind.DIALOGUE)
            }
            return@launchOperation
        }

        val item = requireNotNull(content.items.firstOrNull { it.id == contentId }) { "Item content is unavailable" }
        val collected = worldInteractionService.collect(interactable.id, contentId, sceneId, regionId)
        worldInteraction.value = if (collected) {
            WorldInteraction("Found ${item.name}", item.description, WorldInteractionKind.COLLECTION)
        } else {
            WorldInteraction(item.name, "You already gathered this discovery.", WorldInteractionKind.COLLECTION)
        }
    }

    fun completeScene(sceneId: String) = launchOperation {
        val scene = requireNotNull(content.scenes.firstOrNull { it.id == sceneId }) { "Scene content is unavailable" }
        worldInteractionService.completeScene(scene.id, scene.regionId)
    }

    fun equip(instanceId: String) = launchOperation { equipmentService.equip(instanceId) }
    fun unequip(slot: String) = launchOperation { equipmentService.unequip(slot) }
    fun synthesize(recipeId: String, infusionId: String? = null) = launchOperation { equipmentService.synthesize(recipeId, infusionId) }
    fun enhance(instanceId: String) = launchOperation { equipmentService.enhance(instanceId) }
    fun refine(instanceId: String) = launchOperation { equipmentService.refine(instanceId) }
    fun salvage(instanceId: String) = launchOperation { equipmentService.salvage(instanceId) }

    fun spendDreamSpark(nodeId: String) = launchOperation { petSpecializationService.spend(nodeId) }
    fun acceptQuest(questId: String) = launchOperation { questProgressService.accept(questId) }
    fun abandonQuest(questId: String) = launchOperation { questProgressService.abandon(questId) }
    fun trackQuest(questId: String, tracked: Boolean) = launchOperation {
        questProgressService.track(questId, tracked)
        settingsRepository.setTrackedQuestId(if (tracked) questId else null)
    }
    fun claimQuest(questId: String) = launchOperation { questProgressService.claim(questId) }
    fun chooseEvolution(formId: String) = launchOperation { evolutionService.choose(formId) }
    fun equipTitle(titleId: String) = launchOperation { titleService.equip(titleId) }

    fun renamePet(name: String) = launchOperation {
        val clean = name.trim().take(24)
        require(clean.length >= 2) { "Pet name must contain at least two characters" }
        petDao.pet()?.let { petDao.update(it.copy(displayName = clean, updatedAtEpochMs = System.currentTimeMillis())) }
    }

    fun markPermissionExplanations() = launchOperation {
        settingsRepository.markActivityPermissionExplained()
        settingsRepository.markNotificationPermissionExplained()
    }
    fun markActivityPermissionExplained() = launchOperation { settingsRepository.markActivityPermissionExplained() }
    fun markNotificationPermissionExplained() = launchOperation { settingsRepository.markNotificationPermissionExplained() }

    fun setAlarmEnabled(value: Boolean) = launchOperation {
        if (value) {
            require(wakeAlarmScheduler.canSchedule()) { "Allow exact alarms in Android settings to enable the wake alarm" }
            wakeAlarmScheduler.schedule(ScheduleTimes.nextOccurrence(System.currentTimeMillis(), state.value.settings.targetWakeMinutes))
            settingsRepository.setAlarmEnabled(true)
        } else {
            wakeAlarmScheduler.cancel()
            settingsRepository.setAlarmEnabled(false)
        }
    }

    fun setBedtimeReminderEnabled(value: Boolean) = launchOperation {
        settingsRepository.setBedtimeReminderEnabled(value)
        if (value) bedtimeReminderScheduler.schedule(ScheduleTimes.nextOccurrence(System.currentTimeMillis(), state.value.settings.targetSleepMinutes))
        else bedtimeReminderScheduler.cancel()
    }

    fun setWindDownReminderEnabled(value: Boolean) = launchOperation {
        settingsRepository.setWindDownReminderEnabled(value)
        if (value) {
            val target = (state.value.settings.targetSleepMinutes - 60 + 1440) % 1440
            windDownReminderScheduler.schedule(ScheduleTimes.nextOccurrence(System.currentTimeMillis(), target))
        } else windDownReminderScheduler.cancel()
    }

    fun setMorningResultNotificationsEnabled(value: Boolean) = launchOperation { settingsRepository.setMorningResultNotificationsEnabled(value) }
    fun setLargeControls(value: Boolean) = launchOperation { settingsRepository.setLargeControls(value) }
    fun setControlOpacity(value: Float) = launchOperation { settingsRepository.setControlOpacity(value) }
    fun setLeftHandedControls(value: Boolean) = launchOperation { settingsRepository.setLeftHandedControls(value) }
    fun setHapticsEnabled(value: Boolean) = launchOperation { settingsRepository.setHapticsEnabled(value) }
    fun setScreenShakeEnabled(value: Boolean) = launchOperation { settingsRepository.setScreenShakeEnabled(value) }
    fun setReducedMotion(value: Boolean) = launchOperation { settingsRepository.setMotion(if (value) MotionPreference.REDUCED else MotionPreference.FULL) }
    fun setTheme(value: ThemePreference) = launchOperation { settingsRepository.setTheme(value) }

    fun setMusicVolume(value: Float) = launchOperation {
        settingsRepository.setMusicVolume(value)
        gameAudioController.setMusicVolume(value)
    }
    fun setAmbienceVolume(value: Float) = launchOperation {
        settingsRepository.setAmbienceVolume(value)
        gameAudioController.setAmbienceVolume(value)
    }
    fun setSfxVolume(value: Float) = launchOperation { settingsRepository.setSfxVolume(value) }

    fun setVolume(value: Float) = launchOperation {
        settingsRepository.setMusicVolume(value)
        settingsRepository.setAmbienceVolume(value)
        settingsRepository.setSfxVolume(value)
        gameAudioController.setMusicVolume(value)
        gameAudioController.setAmbienceVolume(value)
    }

    fun playSceneAudio(regionId: String) {
        val region = content.regions.firstOrNull { it.id == regionId } ?: return
        val settings = state.value.settings
        gameAudioController.playRegion(region.musicAsset, region.ambienceAsset, settings.musicVolume, settings.ambienceVolume)
    }

    fun stopSceneAudio() = gameAudioController.stopLoops()
    fun playWakeSfx() = gameAudioController.playSfx("audio/sfx/sfx_pet_wake_01.ogg", state.value.settings.sfxVolume)
    fun clearError() { operationError.value = null }
    fun exportData(onReady: (Uri) -> Unit) = launchOperation { onReady(dataControlRepository.exportJson()) }

    fun deleteSleepHistory() = launchOperation {
        sleepSignalSource.unsubscribe()
        dataControlRepository.deleteSleepHistory()
        morningResult.value = null
    }

    fun resetGameProgress() = launchOperation {
        require(state.value.sessions.none { it.state in setOf("ARMED", "TRACKING", "WAKE_PENDING", "REVIEW_PENDING", "FINALIZED") }) {
            "Finish or cancel the current night before resetting game progress"
        }
        dataControlRepository.resetGameProgress()
        worldInteraction.value = null
    }

    fun deleteAllData() = launchOperation {
        sleepSignalSource.unsubscribe()
        wakeAlarmScheduler.cancel()
        windDownReminderScheduler.cancel()
        bedtimeReminderScheduler.cancel()
        gameAudioController.stopAll()
        dataControlRepository.deleteAllLocalData()
        morningResult.value = null
        worldInteraction.value = null
    }

    override fun onCleared() {
        gameAudioController.stopAll()
        super.onCleared()
    }

    private suspend fun saveSleepPlanInternal(sleepMinutes: Int, wakeMinutes: Int) {
        settingsRepository.setSleepPlan(sleepMinutes, wakeMinutes)
        val settings = state.value.settings
        val now = System.currentTimeMillis()
        if (settings.alarmEnabled) wakeAlarmScheduler.schedule(ScheduleTimes.nextOccurrence(now, wakeMinutes))
        if (settings.windDownReminderEnabled) {
            val windDown = (sleepMinutes - 60 + 1440) % 1440
            windDownReminderScheduler.schedule(ScheduleTimes.nextOccurrence(now, windDown))
        }
        if (settings.bedtimeReminderEnabled) bedtimeReminderScheduler.schedule(ScheduleTimes.nextOccurrence(now, sleepMinutes))
    }

    private fun launchOperation(block: suspend () -> Unit) = viewModelScope.launch {
        operationError.value = null
        runCatching { block() }.onFailure { throwable ->
            operationError.value = throwable.message ?: "The operation could not be completed"
        }
    }
}
