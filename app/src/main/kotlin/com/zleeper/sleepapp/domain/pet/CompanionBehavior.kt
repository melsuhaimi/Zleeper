package com.zleeper.sleepapp.domain.pet

import java.util.Random

enum class CompanionActivity { REST, LOOK_AROUND, WANDER_LEFT, WANDER_RIGHT, INSPECT, WATCH_WORLD, CELEBRATE, NAP }
data class CompanionMemoryContext(val memoryType: String, val subjectId: String?, val thoughtKey: String)
data class CompanionMoment(val activity: CompanionActivity, val thoughtKey: String?, val durationMs: Long)

object CompanionDirector {
    fun moment(seed: Long, step: Int, recentMemories: List<CompanionMemoryContext>, reducedMotion: Boolean = false): CompanionMoment {
        if (reducedMotion) return CompanionMoment(CompanionActivity.REST, recentMemories.firstOrNull()?.thoughtKey, 8_000L)
        val random = Random(seed xor (step.toLong() * 0x9E3779B97F4A7C15UL.toLong()))
        val activities = arrayOf(CompanionActivity.REST, CompanionActivity.LOOK_AROUND, CompanionActivity.WANDER_LEFT, CompanionActivity.WANDER_RIGHT, CompanionActivity.INSPECT, CompanionActivity.WATCH_WORLD, CompanionActivity.NAP)
        val thought = if (recentMemories.isNotEmpty() && random.nextInt(100) < 42) recentMemories[random.nextInt(recentMemories.size)].thoughtKey else null
        return CompanionMoment(activities[random.nextInt(activities.size)], thought, 4_500L + random.nextInt(4_501))
    }
}
