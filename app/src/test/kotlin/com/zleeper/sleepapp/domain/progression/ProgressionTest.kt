package com.zleeper.sleepapp.domain.progression

import com.zleeper.sleepapp.domain.sleep.ResolvedSleepSession
import com.zleeper.sleepapp.domain.sleep.SleepConfidence
import com.zleeper.sleepapp.domain.sleep.SleepResolutionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {
    @Test fun normalMaximumIsOneHundredTen() {
        val session = ResolvedSleepSession("id", 0, 480 * 60_000L, 0, 480 * 60_000L, 0, 480, 480, 0, true, SleepResolutionMethod.MANUAL, SleepConfidence.LOW, 1)
        assertEquals(110, ProgressionCalculator.calculate(session, 480, 0, true, ProgressionRules(1)).xp)
    }
    @Test fun levelCurveAlwaysIncreases() { assertTrue(ProgressionCalculator.xpToNextLevel(10) > ProgressionCalculator.xpToNextLevel(9)) }
}
