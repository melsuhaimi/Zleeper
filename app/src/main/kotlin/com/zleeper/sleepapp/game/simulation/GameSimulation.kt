package com.zleeper.sleepapp.game.simulation

import com.zleeper.sleepapp.data.content.PlatformDefinition
import kotlin.math.abs

enum class MotionState { IDLE, WALK, RUN, JUMP_START, JUMP_LOOP, FALL, LAND, INTERACT }
data class PlayerState(
    val x: Float,
    val y: Float,
    val velocityX: Float = 0f,
    val velocityY: Float = 0f,
    val grounded: Boolean = false,
    val facingRight: Boolean = true,
    val motion: MotionState = MotionState.IDLE,
    val motionTimeSeconds: Float = 0f,
    val coyoteSeconds: Float = 0f,
    val jumpBufferSeconds: Float = 0f,
)
data class GameInput(val horizontal: Float = 0f, val jump: Boolean = false, val run: Boolean = false, val interact: Boolean = false)

class GameSimulation(private val platforms: List<PlatformDefinition>, private val worldWidth: Float, private val worldHeight: Float) {
    fun step(player: PlayerState, input: GameInput, deltaSeconds: Float): PlayerState {
        val dt = deltaSeconds.coerceIn(0f, 1f / 20f)
        val horizontal = input.horizontal.coerceIn(-1f, 1f)
        val running = input.run || (abs(horizontal) > .01f && (player.motion == MotionState.RUN || player.motion == MotionState.WALK && player.motionTimeSeconds >= RUN_RAMP_SECONDS))
        val targetSpeed = if (running) RUN_SPEED else WALK_SPEED
        val targetVelocity = horizontal * targetSpeed
        val acceleration = if (abs(horizontal) > .01f) GROUND_ACCELERATION else GROUND_FRICTION
        val vx = approach(player.velocityX, targetVelocity, acceleration * dt)
        var coyote = if (player.grounded) COYOTE_WINDOW else (player.coyoteSeconds - dt).coerceAtLeast(0f)
        var jumpBuffer = if (input.jump) JUMP_BUFFER_WINDOW else (player.jumpBufferSeconds - dt).coerceAtLeast(0f)
        val shouldJump = jumpBuffer > 0f && (player.grounded || coyote > 0f)
        var vy = if (shouldJump) JUMP_VELOCITY else player.velocityY + GRAVITY * dt
        if (shouldJump) { coyote = 0f; jumpBuffer = 0f }
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
        return PlayerState(
            x = nextX,
            y = nextY,
            velocityX = vx,
            velocityY = vy,
            grounded = grounded,
            facingRight = if (abs(horizontal) > .01f) horizontal > 0 else player.facingRight,
            motion = requestedMotion,
            motionTimeSeconds = motionTime,
            coyoteSeconds = coyote,
            jumpBufferSeconds = jumpBuffer,
        )
    }

    private fun approach(current: Float, target: Float, delta: Float): Float = when {
        current < target -> (current + delta).coerceAtMost(target)
        current > target -> (current - delta).coerceAtLeast(target)
        else -> current
    }

    companion object {
        const val WIDTH = 64f
        const val HEIGHT = 72f
        const val WALK_SPEED = 205f
        const val RUN_SPEED = 325f
        const val GROUND_ACCELERATION = 1_450f
        const val GROUND_FRICTION = 1_900f
        const val GRAVITY = 1_420f
        const val JUMP_VELOCITY = -550f
        const val COYOTE_WINDOW = .11f
        const val JUMP_BUFFER_WINDOW = .12f
        const val RUN_RAMP_SECONDS = .65f
    }
}
