package com.zleeper.sleepapp.domain.progression

data class LevelAdvance(val newLevel: Int, val levelsGained: Int, val dreamSparksGranted: Int)

object LevelProgress {
    fun advance(currentLevel: Int, currentTotalXp: Long, xpGranted: Int, rules: ProgressionRules): LevelAdvance {
        require(currentLevel >= 1 && currentTotalXp >= 0 && xpGranted >= 0)
        val newTotal = currentTotalXp + xpGranted
        var level = 1
        var remaining = newTotal
        while (true) {
            val needed = ProgressionCalculator.xpToNextLevel(level, rules.levelCurve)
            if (remaining < needed) break
            remaining -= needed
            level++
        }
        val newLevel = maxOf(currentLevel, level)
        val gained = newLevel - currentLevel
        return LevelAdvance(newLevel, gained, gained * rules.dreamSparksPerLevel)
    }
}
