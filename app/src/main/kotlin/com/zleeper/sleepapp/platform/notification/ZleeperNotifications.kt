package com.zleeper.sleepapp.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zleeper.sleepapp.app.MainActivity
import com.zleeper.sleepapp.platform.alarm.ReminderActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZleeperNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(SLEEP_REMINDERS_CHANNEL, "Sleep reminders", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(WAKE_CHANNEL, "Wake alarm", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(MORNING_RESULTS_CHANNEL, "Morning results", NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
        manager.deleteNotificationChannel("wind_down")
        manager.deleteNotificationChannel("morning_reward")
    }

    fun showWakeAlarm() = notify(WAKE_ID, WAKE_CHANNEL, "Good morning", "Confirm your phone-based sleep estimate and reveal the saved night journey.", NotificationCompat.PRIORITY_HIGH)
    fun showWindDownReminder() = notify(WIND_DOWN_ID, SLEEP_REMINDERS_CHANNEL, "The grove is growing quiet", "Your wind-down window is ready when you are.", NotificationCompat.PRIORITY_DEFAULT)

    fun showBedtimeReminder() {
        val launch = launchPendingIntent(BEDTIME_ID)
        val begin = actionPendingIntent(ReminderActionReceiver.ACTION_BEGIN_SLEEP, 2101)
        val snooze = actionPendingIntent(ReminderActionReceiver.ACTION_SNOOZE_15, 2102)
        val skip = actionPendingIntent(ReminderActionReceiver.ACTION_SKIP_TONIGHT, 2103)
        val notification = NotificationCompat.Builder(context, SLEEP_REMINDERS_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Ready for tonight?")
            .setContentText("Sleep tracking starts only when you choose Begin Sleep.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(launch)
            .addAction(0, "Begin Sleep", begin)
            .addAction(0, "15m Later", snooze)
            .addAction(0, "Skip Tonight", skip)
            .setAutoCancel(false)
            .build()
        safeNotify(BEDTIME_ID, notification)
    }

    fun showRewardsReady() = notify(MORNING_RESULTS_ID, MORNING_RESULTS_CHANNEL, "Your journey is ready", "Open Zleeper to reveal the saved expedition and rewards.", NotificationCompat.PRIORITY_DEFAULT)
    fun cancelBedtimeReminder() = NotificationManagerCompat.from(context).cancel(BEDTIME_ID)

    private fun notify(id: Int, channel: String, title: String, body: String, priority: Int) {
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(priority)
            .setContentIntent(launchPendingIntent(id))
            .setAutoCancel(true)
            .build()
        safeNotify(id, notification)
    }

    private fun launchPendingIntent(requestCode: Int): PendingIntent = PendingIntent.getActivity(context, requestCode, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun actionPendingIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(context, requestCode, Intent(context, ReminderActionReceiver::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun safeNotify(id: Int, notification: android.app.Notification) {
        try { NotificationManagerCompat.from(context).notify(id, notification) } catch (_: SecurityException) { }
    }

    private companion object {
        const val SLEEP_REMINDERS_CHANNEL = "sleep_reminders"
        const val WAKE_CHANNEL = "wake_alarm"
        const val MORNING_RESULTS_CHANNEL = "morning_results"
        const val WAKE_ID = 1001
        const val WIND_DOWN_ID = 1002
        const val BEDTIME_ID = 1003
        const val MORNING_RESULTS_ID = 1004
    }
}
