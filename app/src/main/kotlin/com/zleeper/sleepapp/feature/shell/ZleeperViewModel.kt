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
import com.zleeper.sleepapp.data.local.preferences.AppSettings
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import com.zleeper.sleepapp.data.repository.MorningResult
import com.zleeper.sleepapp.data.repository.NightResolutionService
import com.zleeper.sleepapp.domain.sleep.ResolvedSleepSession
import com.zleeper.sleepapp.domain.sleep.SleepSessionRepository
import com.zleeper.sleepapp.platform.sleep.SleepSignalSource
import com.zleeper.sleepapp.platform.data.DataControlRepository
import com.zleeper.sleepapp.platform.alarm.WakeAlarmScheduler
import com.zleeper.sleepapp.platform.alarm.WindDownReminderScheduler
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ZleeperUiState(val settings: AppSettings = AppSettings(), val pet: PetEntity? = null, val sessions: List<SleepSessionEntity> = emptyList(), val regions: List<com.zleeper.sleepapp.data.content.RegionDefinition> = emptyList(), val scenes: List<com.zleeper.sleepapp.data.content.SceneDefinition> = emptyList(), val items: List<com.zleeper.sleepapp.data.content.ItemDefinition> = emptyList(), val quests: List<com.zleeper.sleepapp.data.content.QuestDefinition> = emptyList(), val contentErrors: List<String> = emptyList(), val operationError: String? = null, val morningResult: MorningResult? = null) {
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
) : ViewModel() {
    private val operationError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val morningResult = kotlinx.coroutines.flow.MutableStateFlow<MorningResult?>(null)
    val state: StateFlow<ZleeperUiState> = combine(settingsRepository.settings, petDao.observePet(), sleepDao.sessions(), operationError, morningResult) { settings, pet, sessions, error, result ->
        ZleeperUiState(settings, pet, sessions, content.regions, content.scenes, content.items, content.quests, content.validate(), error, result)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ZleeperUiState())

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
    fun renamePet(name: String) = launchOperation { petDao.pet()?.let { petDao.update(it.copy(displayName = name.trim().take(24), updatedAtEpochMs = System.currentTimeMillis())) } }
    fun setAlarmEnabled(value: Boolean) = launchOperation {
        settingsRepository.setAlarmEnabled(value)
        if (value) wakeAlarmScheduler.schedule(nextOccurrence(state.value.settings.targetWakeMinutes)) else wakeAlarmScheduler.cancel()
    }
    fun setWindDownReminderEnabled(value: Boolean) = launchOperation { settingsRepository.setWindDownReminderEnabled(value); if (value) windDownReminderScheduler.schedule(nextOccurrence((state.value.settings.targetSleepMinutes - 60 + 1440) % 1440)) else windDownReminderScheduler.cancel() }
    fun setLargeControls(value: Boolean) = launchOperation { settingsRepository.setLargeControls(value) }
    fun clearError() { operationError.value = null }
    fun exportData(onReady: (Uri) -> Unit) = launchOperation { onReady(dataControlRepository.exportJson()) }
    fun deleteAllData() = launchOperation { dataControlRepository.deleteAllLocalData(); morningResult.value = null }

    private fun launchOperation(block: suspend () -> Unit) = viewModelScope.launch { operationError.value = null; runCatching { block() }.onFailure { operationError.value = it.message ?: "The operation could not be completed" } }

    private fun nextOccurrence(minutes: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, minutes / 60); set(java.util.Calendar.MINUTE, minutes % 60); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
        if (calendar.timeInMillis <= System.currentTimeMillis()) calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }
}
