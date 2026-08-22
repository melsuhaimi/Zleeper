package com.zleeper.sleepapp.platform.work

object RewardRetryPolicy {
    const val MAX_RETRIES = 4

    fun shouldRetry(runAttemptCount: Int): Boolean {
        require(runAttemptCount >= 0)
        return runAttemptCount < MAX_RETRIES
    }
}
