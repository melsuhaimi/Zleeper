package com.zleeper.sleepapp.domain.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetStatGrowthTest {
    @Test
    fun affinityBelowThresholdIsRetainedWithoutStatGain() {
        val result = PetStatGrowth.apply(stat = 3, affinity = 20, affinityGranted = 30, growthRate = 20)

        assertEquals(3, result.stat)
        assertEquals(50, result.affinityRemainder)
        assertEquals(0, result.statGains)
    }

    @Test
    fun exactThresholdRaisesStatAndConsumesAffinity() {
        val result = PetStatGrowth.apply(stat = 3, affinity = 0, affinityGranted = 80, growthRate = 20)

        assertEquals(4, result.stat)
        assertEquals(0, result.affinityRemainder)
        assertEquals(1, result.statGains)
    }

    @Test
    fun largeGrantCanCrossMultipleIncreasingThresholds() {
        val result = PetStatGrowth.apply(stat = 3, affinity = 0, affinityGranted = 180, growthRate = 20)

        assertEquals(5, result.stat)
        assertEquals(0, result.affinityRemainder)
        assertEquals(2, result.statGains)
    }

    @Test
    fun existingAffinityCarriesIntoNextGrant() {
        val result = PetStatGrowth.apply(stat = 3, affinity = 70, affinityGranted = 15, growthRate = 20)

        assertEquals(4, result.stat)
        assertEquals(5, result.affinityRemainder)
        assertEquals(1, result.statGains)
    }

    @Test
    fun invalidNegativeAffinityIsRejected() {
        val failure = runCatching { PetStatGrowth.apply(stat = 3, affinity = -1, affinityGranted = 0, growthRate = 20) }

        assertTrue(failure.isFailure)
    }
}
