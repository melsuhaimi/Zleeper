package com.zleeper.sleepapp.data.repository

import androidx.room.withTransaction
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.domain.quest.DomainEvent
import com.zleeper.sleepapp.domain.sleep.SleepSession
import com.zleeper.sleepapp.domain.sleep.SleepSessionRepository
import com.zleeper.sleepapp.platform.sleep.SleepSignalSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepStartService @Inject constructor(
    private val database: ZleeperDatabase,
    private val sleepRepository: SleepSessionRepository,
    private val questProgressService: QuestProgressService,
    private val sleepSignalSource: SleepSignalSource,
) {
    suspend fun begin(targetSleepMinutes: Int, targetWakeMinutes: Int, windDownCompleted: Boolean): SleepSession {
        val session = database.withTransaction {
            val created = sleepRepository.begin(targetSleepMinutes, targetWakeMinutes, windDownCompleted)
            questProgressService.record(DomainEvent.SleepSessionStarted(created.id))
            if (windDownCompleted) questProgressService.record(DomainEvent.WindDownCompleted(created.id))
            created
        }
        if (sleepSignalSource.isAvailable()) sleepSignalSource.subscribe()
        return session
    }
}
