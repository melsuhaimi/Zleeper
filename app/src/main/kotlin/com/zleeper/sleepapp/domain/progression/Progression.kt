package com.zleeper.sleepapp.domain.progression

import com.zleeper.sleepapp.domain.sleep.ResolvedSleepSession
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

data class LevelCurve(val base: Int = 120, val linear: Int = 70, val exponent: Double = 1.32)
data class ReachThresholds(val band1: Int = 28, val band2: Int = 56, val band3: Int = 84, val band4: Int = 112, val band5: Int = 140)
data class ProgressionRules(
    val version: Int,
    val baseParticipationXp: Int = 30,
    val durationWeight: Int = 20,
    val timingWeight: Int = 20,
    val consistencyWeight: Int = 20,
    val windDownWeight: Int = 15,
    val reflectionWeight: Int = 5,
    val dreamSparksPerLevel: Int = 1,
    val levelCurve: LevelCurve = LevelCurve(),
    val reachThresholds: ReachThresholds = ReachThresholds(),
)

data class ProgressionQuality(val xp: Int, val durationFit: Int, val timingFit: Int, val consistencyFit: Int, val windDown: Int, val reflection: Int)

object ProgressionCalculator {
    fun calculate(session: ResolvedSleepSession, targetDurationMinutes: Int, consistencyOffsetMinutes: Int, reflected: Boolean, rules: ProgressionRules): ProgressionQuality {
        require(targetDurationMinutes > 0)
        val duration = ((1f - abs(session.estimatedSleepMinutes - targetDurationMinutes) / targetDurationMinutes.toFloat()).coerceIn(0f, 1f) * rules.durationWeight).roundToInt()
        val timing = ((1f - session.timingOffsetMinutes.coerceAtLeast(0) / 180f).coerceIn(0f, 1f) * rules.timingWeight).roundToInt()
        val consistency = ((1f - consistencyOffsetMinutes.coerceAtLeast(0) / 180f).coerceIn(0f, 1f) * rules.consistencyWeight).roundToInt()
        val windDown = if (session.windDownCompleted) rules.windDownWeight else 0
        val reflection = if (reflected) rules.reflectionWeight else 0
        return ProgressionQuality(rules.baseParticipationXp + duration + timing + consistency + windDown + reflection, duration, timing, consistency, windDown, reflection)
    }

    fun xpToNextLevel(level: Int, curve: LevelCurve = LevelCurve()): Int {
        require(level >= 1)
        return (curve.base + curve.linear * level.toDouble().pow(curve.exponent)).roundToInt()
    }

    fun xpIntoLevel(totalXp: Long, level: Int, curve: LevelCurve = LevelCurve()): Int {
        var spent = 0L
        for (current in 1 until level) spent += xpToNextLevel(current, curve)
        return (totalXp - spent).coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    fun expeditionReachScore(qualityXp: Int, energy: Int, resilience: Int): Int =
        qualityXp.coerceAtLeast(0) + energy.coerceAtLeast(0) * 2 + resilience.coerceAtLeast(0)

    fun reachBand(score: Int, thresholds: ReachThresholds): Int = when {
        score >= thresholds.band5 -> 5
        score >= thresholds.band4 -> 4
        score >= thresholds.band3 -> 3
        score >= thresholds.band2 -> 2
        else -> 1
    }
}
