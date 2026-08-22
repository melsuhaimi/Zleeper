package com.zleeper.sleepapp.platform.work

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardRetryPolicyTest {
    @Test
    fun retriesUntilConfiguredAttemptLimit() {
        assertTrue(RewardRetryPolicy.shouldRetry(0))
        assertTrue(RewardRetryPolicy.shouldRetry(1))
        assertTrue(RewardRetryPolicy.shouldRetry(2))
        assertTrue(RewardRetryPolicy.shouldRetry(3))
        assertFalse(RewardRetryPolicy.shouldRetry(4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeAttemptCountIsRejected() {
        RewardRetryPolicy.shouldRetry(-1)
    }
}
