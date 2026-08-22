package com.zleeper.sleepapp.domain.sleep

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepResolverTest {
    private val resolver = SleepResolver()
    @Test fun manualFallbackRemainsPlayable() {
        val session = SleepSession("night", SleepSessionState.TRACKING, 1_000, null, 22 * 60, 7 * 60, false, 7)
        val result = resolver.resolve(session, emptyList(), 8 * 60 * 60 * 1000L + 1_000, 9_000)
        assertEquals(SleepResolutionMethod.MANUAL, result.resolutionMethod)
        assertEquals(SleepConfidence.LOW, result.confidence)
        assertEquals(480, result.estimatedSleepMinutes)
    }

    @Test fun segmentAndClassificationProduceHighConfidence() {
        val session = SleepSession("night", SleepSessionState.TRACKING, 1_000, null, 0, 480, true, 9)
        val signals = listOf(SleepSignal("a", "night", SleepSignalType.CLASSIFY, 5_000, confidencePercent = 80), SleepSignal("b", "night", SleepSignalType.SEGMENT, 3_605_000, 3_600_000, 0))
        assertEquals(SleepConfidence.HIGH, resolver.resolve(session, signals, 3_700_000, 4_000_000).confidence)
    }
}
