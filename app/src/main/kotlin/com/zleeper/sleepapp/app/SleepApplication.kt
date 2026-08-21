package com.zleeper.sleepapp.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zleeper.sleepapp.platform.notification.ZleeperNotifications
import com.zleeper.sleepapp.platform.work.SignalRetentionWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class SleepApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notifications: ZleeperNotifications
    override val workManagerConfiguration: Configuration get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "sleep-signal-retention",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SignalRetentionWorker>(12, TimeUnit.HOURS).build(),
        )
    }
}
