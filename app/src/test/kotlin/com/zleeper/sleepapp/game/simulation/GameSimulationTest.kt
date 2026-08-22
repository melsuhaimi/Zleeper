package com.zleeper.sleepapp.game.simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSimulationTest {
    private fun floor(width: Float = 800f) = CollisionPlatform(0f, 200f, width, 30f)

    @Test fun playerLandsOnPlatform() {
        val simulation = GameSimulation(listOf(floor(500f)), 500f, 500f)
        var player = PlayerState(50f, 0f)
        repeat(100) { player = simulation.step(player, GameInput(), 1f / 60f) }
        assertTrue(player.grounded)
        assertEquals(200f - GameSimulation.HEIGHT, player.y, 0.01f)
    }

    @Test fun heldDirectionAcceleratesAndFacesPlayer() {
        val simulation = GameSimulation(listOf(floor()), 800f, 500f)
        var player = PlayerState(300f, 200f - GameSimulation.HEIGHT, grounded = true)
        repeat(20) { player = simulation.step(player, GameInput(horizontal = -1f), 1f / 60f) }
        assertTrue(player.x < 300f)
        assertTrue(player.velocityX < 0f)
        assertTrue(!player.facingRight)
        assertEquals(MotionState.WALK, player.motion)
    }

    @Test fun releasingDirectionUsesFrictionInsteadOfStoppingInstantly() {
        val simulation = GameSimulation(listOf(floor()), 800f, 500f)
        var player = PlayerState(100f, 200f - GameSimulation.HEIGHT, grounded = true)
        repeat(8) { player = simulation.step(player, GameInput(horizontal = 1f), 1f / 60f) }
        val movingVelocity = player.velocityX
        player = simulation.step(player, GameInput(), 1f / 60f)
        assertTrue(player.velocityX > 0f && player.velocityX < movingVelocity)
    }

    @Test fun sustainedMovementNaturallyTransitionsToRun() {
        val simulation = GameSimulation(listOf(floor(1_200f)), 1_200f, 500f)
        var player = PlayerState(100f, 200f - GameSimulation.HEIGHT, grounded = true)
        repeat(75) { player = simulation.step(player, GameInput(horizontal = 1f), 1f / 60f) }
        assertEquals(MotionState.RUN, player.motion)
        assertTrue(player.velocityX > GameSimulation.WALK_SPEED)
    }

    @Test fun bufferedJumpFiresOnLanding() {
        val simulation = GameSimulation(listOf(floor()), 800f, 500f)
        var player = PlayerState(100f, 110f, velocityY = 160f)
        player = simulation.step(player, GameInput(jump = true), 1f / 60f)
        repeat(8) { player = simulation.step(player, GameInput(), 1f / 60f) }
        assertTrue(player.velocityY < 0f)
        assertTrue(!player.grounded)
    }

    @Test fun specializationCoyoteBonusChangesActualJumpForgiveness() {
        val standard = GameSimulation(emptyList(), 1_000f, 1_000f)
        val specialized = GameSimulation(emptyList(), 1_000f, 1_000f, coyoteWindowSeconds = .21f)
        val initial = PlayerState(x = 100f, y = 100f, grounded = true)

        fun afterLeaving(simulation: GameSimulation): PlayerState {
            var player = simulation.step(initial, GameInput(), .05f)
            repeat(3) { player = simulation.step(player, GameInput(), .05f) }
            return simulation.step(player, GameInput(jump = true), .01f)
        }

        val standardJump = afterLeaving(standard)
        val specializedJump = afterLeaving(specialized)
        assertTrue("base coyote window should have expired", standardJump.velocityY > 0f)
        assertEquals(GameSimulation.JUMP_VELOCITY, specializedJump.velocityY, .001f)
    }
}
