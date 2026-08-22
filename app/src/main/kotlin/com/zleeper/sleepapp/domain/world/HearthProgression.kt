package com.zleeper.sleepapp.domain.world

data class HearthStage(val id: String, val name: String, val minimumMemories: Int, val featureIds: Set<String>)
data class HearthProgress(val stage: HearthStage, val currentMemories: Int, val next: HearthStage?, val memoriesToNext: Int)

object HearthProgression {
    fun stageFor(memories: Int, stages: List<HearthStage>): HearthStage {
        require(memories >= 0 && stages.isNotEmpty())
        return stages.filter { memories >= it.minimumMemories }.maxByOrNull { it.minimumMemories } ?: stages.minBy { it.minimumMemories }
    }
    fun progressToNext(memories: Int, stages: List<HearthStage>): HearthProgress {
        val current = stageFor(memories, stages)
        val next = stages.filter { it.minimumMemories > current.minimumMemories }.minByOrNull { it.minimumMemories }
        return HearthProgress(current, memories, next, next?.let { (it.minimumMemories - memories).coerceAtLeast(0) } ?: 0)
    }
}
