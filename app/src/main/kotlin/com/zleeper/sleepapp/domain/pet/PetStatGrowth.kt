package com.zleeper.sleepapp.domain.pet

data class StatGrowthResult(val stat: Int, val affinityRemainder: Int, val statGains: Int)

object PetStatGrowth {
    fun apply(stat: Int, affinity: Int, affinityGranted: Int, growthRate: Int): StatGrowthResult {
        require(stat >= 0 && affinity >= 0 && affinityGranted >= 0 && growthRate > 0)
        var currentStat = stat
        var remainder = affinity + affinityGranted
        var gains = 0
        while (true) {
            val threshold = growthRate * (currentStat + 1)
            if (remainder < threshold) break
            remainder -= threshold
            currentStat++
            gains++
        }
        return StatGrowthResult(currentStat, remainder, gains)
    }
}
