package com.zleeper.sleepapp.game.simulation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionSystemTest {
    @Test
    fun horizontalMovementIsClampedToWorldBounds() {
        val simulation = GameSimulation(emptyList(), worldWidth = 300f, worldHeight = 500f)
        var player = PlayerState(x = 0f, y = 100f, velocityX = -500f)

        player = simulation.step(player, GameInput(horizontal = -1f), 1f / 20f)
        assertEquals(0f, player.x, 0.001f)

        player = player.copy(x = 300f - GameSimulation.WIDTH, velocityX = 500f)
        player = simulation.step(player, GameInput(horizontal = 1f), 1f / 20f)
        assertEquals(300f - GameSimulation.WIDTH, player.x, 0.001f)
    }

    @Test
    fun fallingPlayerLandsOnHighestIntersectedPlatform() {
        val simulation = GameSimulation(
            platforms = listOf(
                CollisionPlatform(0f, 220f, 400f, 20f),
                CollisionPlatform(0f, 180f, 400f, 20f),
            ),
            worldWidth = 400f,
            worldHeight = 500f,
        )
        var player = PlayerState(x = 50f, y = 80f, velocityY = 300f)

        repeat(20) {
            player = simulation.step(player, GameInput(), 1f / 60f)
            if (player.grounded) return@repeat
        }

        assertTrue(player.grounded)
        assertEquals(180f - GameSimulation.HEIGHT, player.y, 0.01f)
    }

    @Test
    fun fallingPastWorldBottomRespawnsPlayer() {
        val simulation = GameSimulation(emptyList(), worldWidth = 800f, worldHeight = 400f)
        val player = PlayerState(x = 500f, y = 410f, velocityX = 80f, velocityY = 200f)

        val result = simulation.step(player, GameInput(), 1f / 60f)

        assertEquals(40f, result.x, 0.001f)
        assertEquals(40f, result.y, 0.001f)
        assertEquals(0f, result.velocityY, 0.001f)
        assertTrue(!result.grounded)
    }
}
