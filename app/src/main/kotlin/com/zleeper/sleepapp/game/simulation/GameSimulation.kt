package com.zleeper.sleepapp.game.simulation

import kotlin.math.abs

data class CollisionPlatform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

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

data class GameInput(
    val horizontal: Float = 0f,
    val jump: Boolean = false,
    val run: Boolean = false,
    val interact: Boolean = false,
)

class GameSimulation(
    private val platforms: List<CollisionPlatform>,
    private val worldWidth: Float,
    private val worldHeight: Float,
    coyoteWindowSeconds: Float = BASE_COYOTE_WINDOW,
) {
    private val coyoteWindowSeconds = coyoteWindowSeconds.coerceIn(BASE_COYOTE_WINDOW, MAX_COYOTE_WINDOW)

    fun step(player: PlayerState, input: GameInput, deltaSeconds: Float): PlayerState {
        val dt = deltaSeconds.coerceIn(0f, MAX_STEP_SECONDS)
        val horizontal = input.horizontal.coerceIn(-1f, 1f)
        val running = input.run || (
            abs(horizontal) > .01f &&
                (player.motion == MotionState.RUN || player.motion == MotionState.WALK && player.motionTimeSeconds >= RUN_RAMP_SECONDS)
            )
        val targetSpeed = if (running) RUN_SPEED else WALK_SPEED
        val targetVelocity = horizontal * targetSpeed
        val acceleration = if (abs(horizontal) > .01f) GROUND_ACCELERATION else GROUND_FRICTION
        val vx = approach(player.velocityX, targetVelocity, acceleration * dt)

        var coyote = if (player.grounded) coyoteWindowSeconds else (player.coyoteSeconds - dt).coerceAtLeast(0f)
        var jumpBuffer = if (input.jump) JUMP_BUFFER_WINDOW else (player.jumpBufferSeconds - dt).coerceAtLeast(0f)
        val shouldJump = jumpBuffer > 0f && (player.grounded || coyote > 0f)
        var vy = if (shouldJump) JUMP_VELOCITY else player.velocityY + GRAVITY * dt
        if (shouldJump) {
            coyote = 0f
            jumpBuffer = 0f
        }

        var nextX = (player.x + vx * dt).coerceIn(0f, (worldWidth - WIDTH).coerceAtLeast(0f))
        var nextY = player.y + vy * dt
        var grounded = false
        var landedThisFrame = false
        if (vy >= 0f) {
            val oldBottom = player.y + HEIGHT
            val newBottom = nextY + HEIGHT
            val landing = platforms
                .asSequence()
                .filter { nextX + WIDTH > it.x && nextX < it.x + it.width }
                .filter { oldBottom <= it.y + LANDING_TOLERANCE && newBottom >= it.y }
                .minByOrNull { it.y }
            if (landing != null) {
                nextY = landing.y - HEIGHT
                vy = 0f
                grounded = true
                landedThisFrame = !player.grounded
            }
        }
        if (nextY > worldHeight) {
            nextX = RESPAWN_X
            nextY = RESPAWN_Y
            vy = 0f
            coyote = 0f
            jumpBuffer = 0f
        }

        val requestedMotion = when {
            input.interact -> MotionState.INTERACT
            landedThisFrame -> MotionState.LAND
            player.motion == MotionState.LAND && player.motionTimeSeconds < LAND_ANIMATION_SECONDS && grounded -> MotionState.LAND
            player.motion == MotionState.INTERACT && player.motionTimeSeconds < INTERACT_ANIMATION_SECONDS && grounded -> MotionState.INTERACT
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
        const val BASE_COYOTE_WINDOW = .11f
        const val MAX_COYOTE_WINDOW = .35f
        const val JUMP_BUFFER_WINDOW = .12f
        const val RUN_RAMP_SECONDS = .65f
        private const val MAX_STEP_SECONDS = 1f / 20f
        private const val LANDING_TOLERANCE = 4f
        private const val LAND_ANIMATION_SECONDS = .45f
        private const val INTERACT_ANIMATION_SECONDS = .65f
        private const val RESPAWN_X = 40f
        private const val RESPAWN_Y = 40f
    }
}
