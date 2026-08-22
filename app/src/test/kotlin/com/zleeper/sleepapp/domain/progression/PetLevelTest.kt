package com.zleeper.sleepapp.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Test

class PetLevelTest {
    private val rules = ProgressionRules(
        version = 2,
        dreamSparksPerLevel = 2,
        levelCurve = LevelCurve(base = 100, linear = 0, exponent = 1.0),
    )

    @Test
    fun xpBelowThresholdDoesNotAdvanceLevel() {
        assertEquals(LevelAdvance(newLevel = 1, levelsGained = 0, dreamSparksGranted = 0), LevelProgress.advance(1, 0, 99, rules))
    }

    @Test
    fun crossingOneThresholdAdvancesAndGrantsConfiguredDreamSparks() {
        assertEquals(LevelAdvance(newLevel = 2, levelsGained = 1, dreamSparksGranted = 2), LevelProgress.advance(1, 0, 100, rules))
    }

    @Test
    fun oneGrantCanAdvanceMultipleLevels() {
        assertEquals(LevelAdvance(newLevel = 3, levelsGained = 2, dreamSparksGranted = 4), LevelProgress.advance(1, 0, 250, rules))
    }

    @Test
    fun advancementHasNoArtificialMaximumLevel() {
        val highLevelRules = rules.copy(levelCurve = LevelCurve(base = 1, linear = 0, exponent = 1.0))

        val result = LevelProgress.advance(currentLevel = 1, currentTotalXp = 0, xpGranted = 150, rules = highLevelRules)

        assertEquals(151, result.newLevel)
        assertEquals(150, result.levelsGained)
        assertEquals(300, result.dreamSparksGranted)
    }

    @Test
    fun currentPersistedLevelNeverMovesBackward() {
        assertEquals(LevelAdvance(newLevel = 8, levelsGained = 0, dreamSparksGranted = 0), LevelProgress.advance(8, 0, 0, rules))
    }
}
