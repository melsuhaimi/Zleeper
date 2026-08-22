package com.zleeper.sleepapp.platform.sleep

import com.zleeper.sleepapp.domain.sleep.SleepSignalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SleepSignalIdentityTest {
    @Test
    fun duplicateClassifyEventUsesSameIdentity() {
        val first = SleepSignalIdentity.id("night", SleepSignalType.CLASSIFY, 1_000L, null, 87)
        val duplicate = SleepSignalIdentity.id("night", SleepSignalType.CLASSIFY, 1_000L, null, 87)

        assertEquals(first, duplicate)
    }

    @Test
    fun materiallyDifferentPlatformEventsDoNotCollapseTogether() {
        val classify = SleepSignalIdentity.id("night", SleepSignalType.CLASSIFY, 1_000L, null, 87)
        val changedConfidence = SleepSignalIdentity.id("night", SleepSignalType.CLASSIFY, 1_000L, null, 88)
        val segment = SleepSignalIdentity.id("night", SleepSignalType.SEGMENT, 1_000L, 500L, null)
        val otherSession = SleepSignalIdentity.id("other-night", SleepSignalType.CLASSIFY, 1_000L, null, 87)

        assertNotEquals(classify, changedConfidence)
        assertNotEquals(classify, segment)
        assertNotEquals(classify, otherSession)
    }
}
