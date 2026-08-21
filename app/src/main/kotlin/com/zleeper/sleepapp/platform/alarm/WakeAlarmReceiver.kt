package com.zleeper.sleepapp.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zleeper.sleepapp.platform.notification.ZleeperNotifications
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WakeAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var notifications: ZleeperNotifications
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var scheduler: WakeAlarmScheduler
    override fun onReceive(context: Context, intent: Intent) {
        notifications.showWakeAlarm()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val value = settings.settings.first()
                if (value.alarmEnabled && scheduler.canSchedule()) {
                    val next = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, value.targetWakeMinutes / 60); set(Calendar.MINUTE, value.targetWakeMinutes % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                    scheduler.schedule(next.timeInMillis)
                }
            } finally { pending.finish() }
        }
    }
}
