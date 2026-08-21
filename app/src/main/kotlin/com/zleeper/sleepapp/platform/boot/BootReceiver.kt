package com.zleeper.sleepapp.platform.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import com.zleeper.sleepapp.platform.alarm.WakeAlarmScheduler
import com.zleeper.sleepapp.platform.alarm.WindDownReminderScheduler
import com.zleeper.sleepapp.platform.work.SignalRetentionWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var wakeAlarmScheduler: WakeAlarmScheduler
    @Inject lateinit var windDownReminderScheduler: WindDownReminderScheduler
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("sleep-signal-retention", ExistingPeriodicWorkPolicy.UPDATE, PeriodicWorkRequestBuilder<SignalRetentionWorker>(12, TimeUnit.HOURS).build())
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val value = settings.settings.first()
                    if (value.alarmEnabled && wakeAlarmScheduler.canSchedule()) {
                        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, value.targetWakeMinutes / 60); set(Calendar.MINUTE, value.targetWakeMinutes % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        if (calendar.timeInMillis <= System.currentTimeMillis()) calendar.add(Calendar.DAY_OF_YEAR, 1)
                        wakeAlarmScheduler.schedule(calendar.timeInMillis)
                    }
                    if (value.windDownReminderEnabled) {
                        val reminderMinutes = (value.targetSleepMinutes - 60 + 1440) % 1440
                        val reminder = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, reminderMinutes / 60); set(Calendar.MINUTE, reminderMinutes % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                        if (reminder.timeInMillis <= System.currentTimeMillis()) reminder.add(Calendar.DAY_OF_YEAR, 1)
                        windDownReminderScheduler.schedule(reminder.timeInMillis)
                    }
                } finally { pending.finish() }
            }
        }
    }
}
