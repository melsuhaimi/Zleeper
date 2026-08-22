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
class BedtimeReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var notifications: ZleeperNotifications
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var scheduler: BedtimeReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val value = settings.settings.first()
                if (!value.bedtimeReminderEnabled) return@launch
                notifications.showBedtimeReminder()
                scheduler.schedule(ScheduleTimes.nextOccurrence(System.currentTimeMillis(), value.targetSleepMinutes))
            } finally { pending.finish() }
        }
    }
}
