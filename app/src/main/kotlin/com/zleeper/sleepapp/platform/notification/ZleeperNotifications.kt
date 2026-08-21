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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZleeperNotifications @Inject constructor(@ApplicationContext private val context: Context) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(NotificationChannel(WAKE_CHANNEL, "Wake alarms", NotificationManager.IMPORTANCE_HIGH), NotificationChannel(REMINDER_CHANNEL, "Wind-down reminders", NotificationManager.IMPORTANCE_DEFAULT), NotificationChannel(REWARD_CHANNEL, "Morning rewards", NotificationManager.IMPORTANCE_DEFAULT)))
    }

    fun showWakeAlarm() = notify(WAKE_ID, WAKE_CHANNEL, "Good morning", "Confirm your estimated sleep and discover where your Moonmoth travelled.", NotificationCompat.PRIORITY_HIGH)
    fun showWindDownReminder() = notify(REMINDER_ID, REMINDER_CHANNEL, "The grove is growing quiet", "Your wind-down window is ready when you are.", NotificationCompat.PRIORITY_DEFAULT)
    fun showRewardsReady() = notify(REWARD_ID, REWARD_CHANNEL, "Your journey is ready", "Open Zleeper to reveal the night's expedition.", NotificationCompat.PRIORITY_DEFAULT)

    private fun notify(id: Int, channel: String, title: String, body: String, priority: Int) {
        val launch = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText(body).setPriority(priority).setContentIntent(launch).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(context).notify(id, notification) } catch (_: SecurityException) { /* Permission denial is a supported state. */ }
    }
    private companion object { const val WAKE_CHANNEL = "wake_alarm"; const val REMINDER_CHANNEL = "wind_down"; const val REWARD_CHANNEL = "morning_reward"; const val WAKE_ID = 1001; const val REMINDER_ID = 1002; const val REWARD_ID = 1003 }
}
