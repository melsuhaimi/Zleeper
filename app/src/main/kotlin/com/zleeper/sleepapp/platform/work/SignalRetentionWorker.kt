package com.zleeper.sleepapp.platform.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zleeper.sleepapp.data.local.database.SleepDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SignalRetentionWorker @AssistedInject constructor(@Assisted context: Context, @Assisted parameters: WorkerParameters, private val sleepDao: SleepDao) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        sleepDao.deleteSignalsOlderThan(System.currentTimeMillis() - 48L * 60L * 60L * 1000L)
        return Result.success()
    }
}
