package com.zleeper.sleepapp.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeAlarmScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    private val operation get() = PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context, WakeAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun schedule(atEpochMs: Long) {
        require(atEpochMs > System.currentTimeMillis())
        check(Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) { "Allow exact alarms in Android settings to enable the wake alarm" }
        val showIntent = PendingIntent.getActivity(context, REQUEST_CODE, context.packageManager.getLaunchIntentForPackage(context.packageName), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(atEpochMs, showIntent), operation)
    }
    fun cancel() = manager.cancel(operation)
    fun canSchedule(): Boolean = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()
    private companion object { const val REQUEST_CODE = 8123 }
}
