package com.zleeper.sleepapp.domain.progression

import com.zleeper.sleepapp.domain.sleep.ResolvedSleepSession
import kotlin.math.pow
import kotlin.math.roundToInt

data class ProgressionRules(
    val version: Int,
    val baseParticipationXp: Int = 30,
    val durationWeight: Int = 20,
    val timingWeight: Int = 20,
    val consistencyWeight: Int = 20,
    val windDownWeight: Int = 15,
    val reflectionWeight: Int = 5,
)

data class ProgressionQuality(val xp: Int, val durationFit: Int, val timingFit: Int, val consistencyFit: Int, val windDown: Int, val reflection: Int)

object ProgressionCalculator {
    fun calculate(session: ResolvedSleepSession, targetDurationMinutes: Int, consistencyOffsetMinutes: Int, reflected: Boolean, rules: ProgressionRules): ProgressionQuality {
        val duration = ((1f - kotlin.math.abs(session.estimatedSleepMinutes - targetDurationMinutes) / targetDurationMinutes.toFloat()).coerceIn(0f, 1f) * rules.durationWeight).roundToInt()
        val timing = ((1f - session.timingOffsetMinutes / 180f).coerceIn(0f, 1f) * rules.timingWeight).roundToInt()
        val consistency = ((1f - consistencyOffsetMinutes / 180f).coerceIn(0f, 1f) * rules.consistencyWeight).roundToInt()
        val windDown = if (session.windDownCompleted) rules.windDownWeight else 0
        val reflection = if (reflected) rules.reflectionWeight else 0
        return ProgressionQuality(rules.baseParticipationXp + duration + timing + consistency + windDown + reflection, duration, timing, consistency, windDown, reflection)
    }

    fun xpToNextLevel(level: Int): Int = (120 + 70 * level.toDouble().pow(1.32)).roundToInt()
    fun reachBand(quality: Int, energy: Int, resilience: Int): Int = ((quality + energy * 2 + resilience) / 28).coerceIn(1, 5)
}
