package com.zleeper.sleepapp.platform.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zleeper.sleepapp.data.local.database.MorningDao
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import com.zleeper.sleepapp.data.repository.NightResolutionService
import com.zleeper.sleepapp.platform.notification.ZleeperNotifications
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class RewardResolutionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val nightResolutionService: NightResolutionService,
    private val morningDao: MorningDao,
    private val settingsRepository: SettingsRepository,
    private val notifications: ZleeperNotifications,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(RewardResolutionScheduler.KEY_SESSION_ID) ?: return Result.failure()
        return runCatching {
            val settings = settingsRepository.settings.first()
            val reflected = morningDao.note(sessionId) != null
            nightResolutionService.resolve(sessionId, settings.targetDurationMinutes, reflected)
            if (settings.morningResultNotificationsEnabled) notifications.showRewardsReady()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure() },
        )
    }

    private companion object { const val MAX_RETRIES = 4 }
}
