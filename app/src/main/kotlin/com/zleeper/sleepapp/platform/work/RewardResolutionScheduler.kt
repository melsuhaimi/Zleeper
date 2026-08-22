package com.zleeper.sleepapp.platform.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardResolutionScheduler @Inject constructor(@ApplicationContext context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueue(sleepSessionId: String) {
        val request = OneTimeWorkRequestBuilder<RewardResolutionWorker>()
            .setInputData(Data.Builder().putString(KEY_SESSION_ID, sleepSessionId).build())
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork("$UNIQUE_PREFIX:$sleepSessionId", ExistingWorkPolicy.KEEP, request)
    }

    fun cancelAll() = workManager.cancelAllWorkByTag(TAG)

    companion object {
        const val KEY_SESSION_ID = "sleep_session_id"
        const val TAG = "reward-resolution"
        private const val UNIQUE_PREFIX = "reward-resolution"
    }
}
