package com.zleeper.sleepapp.platform.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zleeper.sleepapp.data.local.database.SleepDao
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import com.zleeper.sleepapp.domain.sleep.SleepSessionState
import com.zleeper.sleepapp.platform.alarm.BedtimeReminderScheduler
import com.zleeper.sleepapp.platform.alarm.ScheduleTimes
import com.zleeper.sleepapp.platform.alarm.WakeAlarmScheduler
import com.zleeper.sleepapp.platform.alarm.WindDownReminderScheduler
import com.zleeper.sleepapp.platform.sleep.SleepSignalSource
import com.zleeper.sleepapp.platform.work.RewardResolutionScheduler
import com.zleeper.sleepapp.platform.work.SignalRetentionWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var sleepDao: SleepDao
    @Inject lateinit var sleepSignalSource: SleepSignalSource
    @Inject lateinit var wakeAlarmScheduler: WakeAlarmScheduler
    @Inject lateinit var windDownReminderScheduler: WindDownReminderScheduler
    @Inject lateinit var bedtimeReminderScheduler: BedtimeReminderScheduler
    @Inject lateinit var rewardResolutionScheduler: RewardResolutionScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sleep-signal-retention",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SignalRetentionWorker>(12, TimeUnit.HOURS).build(),
        )
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val value = settings.settings.first()
                if (value.alarmEnabled && wakeAlarmScheduler.canSchedule()) {
                    wakeAlarmScheduler.schedule(ScheduleTimes.nextOccurrence(now, value.targetWakeMinutes))
                }
                if (value.windDownReminderEnabled) {
                    val windDown = (value.targetSleepMinutes - 60 + 1440) % 1440
                    windDownReminderScheduler.schedule(ScheduleTimes.nextOccurrence(now, windDown))
                }
                if (value.bedtimeReminderEnabled) {
                    bedtimeReminderScheduler.schedule(ScheduleTimes.nextOccurrence(now, value.targetSleepMinutes))
                }

                val signalSession = sleepDao.trackingSession()
                if (signalSession != null && sleepSignalSource.isAvailable()) {
                    val subscription = sleepSignalSource.subscribe()
                    if (subscription.isSuccess && signalSession.state == SleepSessionState.ARMED.name) {
                        val current = sleepDao.session(signalSession.id)
                        if (current?.state == SleepSessionState.ARMED.name) {
                            sleepDao.updateSession(current.copy(state = SleepSessionState.TRACKING.name))
                        }
                    }
                }

                sleepDao.pendingRewardResolution().forEach { session -> rewardResolutionScheduler.enqueue(session.id) }
            } finally {
                pending.finish()
            }
        }
    }
}
