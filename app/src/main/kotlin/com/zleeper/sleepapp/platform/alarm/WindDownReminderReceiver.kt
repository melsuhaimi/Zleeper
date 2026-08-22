package com.zleeper.sleepapp.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import com.zleeper.sleepapp.platform.notification.ZleeperNotifications
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WindDownReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var notifications: ZleeperNotifications
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var scheduler: WindDownReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val value = settings.settings.first()
                if (!value.windDownReminderEnabled) return@launch
                notifications.showWindDownReminder()
                val reminderMinutes = (value.targetSleepMinutes - 60 + 1440) % 1440
                scheduler.schedule(ScheduleTimes.nextOccurrence(System.currentTimeMillis(), reminderMinutes))
            } finally { pending.finish() }
        }
    }
}
