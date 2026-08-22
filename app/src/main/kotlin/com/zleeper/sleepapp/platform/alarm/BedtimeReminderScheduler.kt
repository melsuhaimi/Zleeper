package com.zleeper.sleepapp.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BedtimeReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    private val operation get() = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, BedtimeReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(atEpochMs: Long) {
        require(atEpochMs > System.currentTimeMillis())
        manager.setWindow(AlarmManager.RTC_WAKEUP, atEpochMs, WINDOW_MILLIS, operation)
    }

    fun cancel() = manager.cancel(operation)

    private companion object {
        const val REQUEST_CODE = 8125
        const val WINDOW_MILLIS = 15 * 60_000L
    }
}
