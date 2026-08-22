package com.zleeper.sleepapp.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import com.zleeper.sleepapp.data.repository.SleepStartService
import com.zleeper.sleepapp.platform.notification.ZleeperNotifications
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {
    @Inject lateinit var sleepStartService: SleepStartService
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var scheduler: BedtimeReminderScheduler
    @Inject lateinit var notifications: ZleeperNotifications

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SNOOZE_15 -> {
                notifications.cancelBedtimeReminder()
                scheduler.schedule(System.currentTimeMillis() + SNOOZE_MILLIS)
            }
            ACTION_SKIP_TONIGHT -> notifications.cancelBedtimeReminder()
            ACTION_BEGIN_SLEEP -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val value = settings.settings.first()
                        sleepStartService.begin(value.targetSleepMinutes, value.targetWakeMinutes, windDownCompleted = false)
                        notifications.cancelBedtimeReminder()
                    } finally { pending.finish() }
                }
            }
        }
    }

    companion object {
        const val ACTION_BEGIN_SLEEP = "com.zleeper.sleepapp.action.BEGIN_SLEEP"
        const val ACTION_SNOOZE_15 = "com.zleeper.sleepapp.action.SNOOZE_BEDTIME_15"
        const val ACTION_SKIP_TONIGHT = "com.zleeper.sleepapp.action.SKIP_BEDTIME_TONIGHT"
        private const val SNOOZE_MILLIS = 15 * 60_000L
    }
}
