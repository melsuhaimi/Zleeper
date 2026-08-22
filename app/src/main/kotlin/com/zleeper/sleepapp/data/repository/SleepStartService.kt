package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.data.local.database.SleepDao
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.domain.quest.DomainEvent
import com.zleeper.sleepapp.domain.sleep.SleepSession
import com.zleeper.sleepapp.domain.sleep.SleepSessionRepository
import com.zleeper.sleepapp.domain.sleep.SleepSessionState
import com.zleeper.sleepapp.platform.sleep.SleepSignalSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepStartService @Inject constructor(
    private val database: ZleeperDatabase,
    private val sleepDao: SleepDao,
    private val sleepRepository: SleepSessionRepository,
    private val questProgressService: QuestProgressService,
    private val sleepSignalSource: SleepSignalSource,
) {
    suspend fun begin(targetSleepMinutes: Int, targetWakeMinutes: Int, windDownCompleted: Boolean): SleepSession {
        val armed = database.withTransaction {
            val created = sleepRepository.begin(targetSleepMinutes, targetWakeMinutes, windDownCompleted)
            require(created.state == SleepSessionState.ARMED) { "New sleep session must be armed before tracking" }
            questProgressService.record(DomainEvent.SleepSessionStarted(created.id))
            if (windDownCompleted) questProgressService.record(DomainEvent.WindDownCompleted(created.id))
            created
        }

        if (sleepSignalSource.isAvailable()) sleepSignalSource.subscribe()

        database.withTransaction {
            val persisted = requireNotNull(sleepDao.session(armed.id)) { "Armed sleep session was not persisted" }
            require(persisted.state == SleepSessionState.ARMED.name) { "Armed sleep session changed before tracking started" }
            sleepDao.updateSession(persisted.copy(state = SleepSessionState.TRACKING.name))
        }
        return armed.copy(state = SleepSessionState.TRACKING)
    }
}
