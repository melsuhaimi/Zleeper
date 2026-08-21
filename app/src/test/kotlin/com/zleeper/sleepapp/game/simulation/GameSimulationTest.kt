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

    @Test fun heldDirectionAcceleratesAndFacesPlayer() {
        val simulation = GameSimulation(listOf(PlatformDefinition(0f, 200f, 800f, 30f)), 800f, 500f)
        var player = PlayerState(300f, 200f - GameSimulation.HEIGHT, grounded = true)
        repeat(20) { player = simulation.step(player, GameInput(horizontal = -1f), 1f / 60f) }
        assertTrue(player.x < 300f)
        assertTrue(player.velocityX < 0f)
        assertTrue(!player.facingRight)
        assertEquals(MotionState.WALK, player.motion)
    }

    @Test fun releasingDirectionUsesFrictionInsteadOfStoppingInstantly() {
        val simulation = GameSimulation(listOf(PlatformDefinition(0f, 200f, 800f, 30f)), 800f, 500f)
        var player = PlayerState(100f, 200f - GameSimulation.HEIGHT, grounded = true)
        repeat(8) { player = simulation.step(player, GameInput(horizontal = 1f), 1f / 60f) }
        val movingVelocity = player.velocityX
        player = simulation.step(player, GameInput(), 1f / 60f)
        assertTrue(player.velocityX > 0f && player.velocityX < movingVelocity)
    }

    @Test fun sustainedMovementNaturallyTransitionsToRun() {
        val simulation = GameSimulation(listOf(PlatformDefinition(0f, 200f, 1_200f, 30f)), 1_200f, 500f)
        var player = PlayerState(100f, 200f - GameSimulation.HEIGHT, grounded = true)
        repeat(75) { player = simulation.step(player, GameInput(horizontal = 1f), 1f / 60f) }
        assertEquals(MotionState.RUN, player.motion)
        assertTrue(player.velocityX > GameSimulation.WALK_SPEED)
    }

    @Test fun bufferedJumpFiresOnLanding() {
        val simulation = GameSimulation(listOf(PlatformDefinition(0f, 200f, 800f, 30f)), 800f, 500f)
        var player = PlayerState(100f, 110f, velocityY = 160f)
        player = simulation.step(player, GameInput(jump = true), 1f / 60f)
        repeat(8) { player = simulation.step(player, GameInput(), 1f / 60f) }
        assertTrue(player.velocityY < 0f)
        assertTrue(!player.grounded)
    }
}
