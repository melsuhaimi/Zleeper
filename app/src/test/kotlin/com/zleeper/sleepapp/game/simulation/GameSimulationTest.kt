package com.zleeper.sleepapp.game.simulation

import com.zleeper.sleepapp.data.content.PlatformDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSimulationTest {
    @Test fun playerLandsOnPlatform() {
        val simulation = GameSimulation(listOf(PlatformDefinition(0f, 200f, 500f, 30f)), 500f, 500f)
        var player = PlayerState(50f, 0f)
        repeat(100) { player = simulation.step(player, GameInput(), 1f / 60f) }
        assertTrue(player.grounded)
        assertEquals(200f - GameSimulation.HEIGHT, player.y, 0.01f)
    }
}
