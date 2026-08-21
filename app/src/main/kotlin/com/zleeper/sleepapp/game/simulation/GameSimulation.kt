package com.zleeper.sleepapp.game.simulation

import com.zleeper.sleepapp.data.content.PlatformDefinition
import kotlin.math.abs

enum class MotionState { IDLE, WALK, RUN, JUMP_START, JUMP_LOOP, FALL, LAND, INTERACT }
data class PlayerState(val x: Float, val y: Float, val velocityX: Float = 0f, val velocityY: Float = 0f, val grounded: Boolean = false, val facingRight: Boolean = true, val motion: MotionState = MotionState.IDLE, val motionTimeSeconds: Float = 0f)
data class GameInput(val horizontal: Float = 0f, val jump: Boolean = false, val run: Boolean = false, val interact: Boolean = false)

class GameSimulation(private val platforms: List<PlatformDefinition>, private val worldWidth: Float, private val worldHeight: Float) {
    fun step(player: PlayerState, input: GameInput, deltaSeconds: Float): PlayerState {
        val dt = deltaSeconds.coerceIn(0f, 1f / 20f)
        val targetSpeed = if (input.run) 320f else 190f
        val vx = input.horizontal.coerceIn(-1f, 1f) * targetSpeed
        var vy = if (input.jump && player.grounded) -540f else player.velocityY + 1450f * dt
        var nextX = (player.x + vx * dt).coerceIn(0f, worldWidth - WIDTH)
        var nextY = player.y + vy * dt
        var grounded = false
        var landedThisFrame = false
        if (vy >= 0f) {
            val oldBottom = player.y + HEIGHT
            val newBottom = nextY + HEIGHT
            val landing = platforms.filter { nextX + WIDTH > it.x && nextX < it.x + it.width && oldBottom <= it.y + 4f && newBottom >= it.y }.minByOrNull { it.y }
            if (landing != null) { nextY = landing.y - HEIGHT; vy = 0f; grounded = true; landedThisFrame = !player.grounded }
        }
        if (nextY > worldHeight) { nextX = 40f; nextY = 40f; vy = 0f }
        val requestedMotion = when {
            input.interact -> MotionState.INTERACT
            landedThisFrame -> MotionState.LAND
            player.motion == MotionState.LAND && player.motionTimeSeconds < 0.45f && grounded -> MotionState.LAND
            player.motion == MotionState.INTERACT && player.motionTimeSeconds < 0.65f && grounded -> MotionState.INTERACT
            !grounded && vy < -250f -> MotionState.JUMP_START
            !grounded && vy < 0f -> MotionState.JUMP_LOOP
            !grounded -> MotionState.FALL
            abs(vx) > 250f -> MotionState.RUN
            abs(vx) > 1f -> MotionState.WALK
            else -> MotionState.IDLE
        }
        val motionTime = if (requestedMotion == player.motion) player.motionTimeSeconds + dt else 0f
        return PlayerState(nextX, nextY, vx, vy, grounded, if (abs(vx) > 1f) vx > 0 else player.facingRight, requestedMotion, motionTime)
    }
    companion object { const val WIDTH = 64f; const val HEIGHT = 72f }
}
