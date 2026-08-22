package com.zleeper.sleepapp.domain.quest

enum class QuestLifecycle { LOCKED, AVAILABLE, ACTIVE, COMPLETED, CLAIMED }

data class QuestObjectiveRule(val id: String, val type: ObjectiveType, val requiredCount: Int, val parameters: Map<String, String> = emptyMap()) {
    init { require(requiredCount > 0) }
}
data class QuestObjectiveState(val objectiveId: String, val currentCount: Int, val requiredCount: Int) {
    init { require(currentCount >= 0 && requiredCount > 0 && currentCount <= requiredCount) }
}
data class QuestState(val questId: String, val lifecycle: QuestLifecycle, val tracked: Boolean, val objectives: List<QuestObjectiveState>)

object QuestEngine {
    fun accept(state: QuestState): QuestState {
        require(state.lifecycle == QuestLifecycle.AVAILABLE) { "Only available quests can be accepted" }
        return state.copy(lifecycle = QuestLifecycle.ACTIVE)
    }
    fun abandon(state: QuestState, resetProgress: Boolean = false): QuestState {
        require(state.lifecycle == QuestLifecycle.ACTIVE) { "Only active quests can be abandoned" }
        return state.copy(lifecycle = QuestLifecycle.AVAILABLE, tracked = false, objectives = if (resetProgress) state.objectives.map { it.copy(currentCount = 0) } else state.objectives)
    }
    fun track(state: QuestState, tracked: Boolean): QuestState {
        require(state.lifecycle == QuestLifecycle.ACTIVE || state.lifecycle == QuestLifecycle.COMPLETED)
        return state.copy(tracked = tracked)
    }
    fun apply(state: QuestState, rules: List<QuestObjectiveRule>, event: DomainEvent): QuestState {
        if (state.lifecycle != QuestLifecycle.ACTIVE) return state
        require(rules.map { it.id }.toSet() == state.objectives.map { it.objectiveId }.toSet())
        val next = state.objectives.map { objective ->
            val rule = rules.first { it.id == objective.objectiveId }
            val increment = increment(rule, event)
            if (increment <= 0) objective else objective.copy(currentCount = (objective.currentCount + increment).coerceAtMost(objective.requiredCount))
        }
        return state.copy(lifecycle = if (next.all { it.currentCount >= it.requiredCount }) QuestLifecycle.COMPLETED else state.lifecycle, objectives = next)
    }
    fun claim(state: QuestState): QuestState {
        require(state.lifecycle == QuestLifecycle.COMPLETED) { "Quest rewards can only be claimed after completion" }
        return state.copy(lifecycle = QuestLifecycle.CLAIMED, tracked = false)
    }
    private fun increment(rule: QuestObjectiveRule, event: DomainEvent): Int = when (rule.type) {
        ObjectiveType.SLEEP_WITHIN_TARGET_WINDOW -> (event as? DomainEvent.SleepTimingObserved)?.let { if (it.timingOffsetMinutes <= rule.intParam("tolerance_minutes")) 1 else 0 } ?: 0
        ObjectiveType.COMPLETE_WIND_DOWN -> if (event is DomainEvent.WindDownCompleted) 1 else 0
        ObjectiveType.FINALIZE_SLEEP_SESSION -> if (event is DomainEvent.SleepSessionFinalized) 1 else 0
        ObjectiveType.COMPLETE_MORNING_REVIEW -> if (event is DomainEvent.MorningReviewCompleted) 1 else 0
        ObjectiveType.ACCUMULATE_CONSISTENCY -> (event as? DomainEvent.ConsistencyAccumulated)?.amount ?: 0
        ObjectiveType.REACH_PET_LEVEL -> (event as? DomainEvent.PetLevelReached)?.let { if (it.level >= rule.intParam("level")) 1 else 0 } ?: 0
        ObjectiveType.REACH_PET_STAT -> (event as? DomainEvent.PetStatReached)?.let { if (it.stat == rule.parameters["stat"] && it.value >= rule.intParam("value")) 1 else 0 } ?: 0
        ObjectiveType.ENTER_REGION -> (event as? DomainEvent.EnterRegion)?.let { if (it.regionId == rule.parameters["region_id"]) 1 else 0 } ?: 0
        ObjectiveType.COMPLETE_SCENE -> (event as? DomainEvent.SceneCompleted)?.let { if (it.sceneId == rule.parameters["scene_id"]) 1 else 0 } ?: 0
        ObjectiveType.COLLECT_ITEM -> (event as? DomainEvent.ItemGranted)?.let { if (it.itemId == rule.parameters["item_id"]) it.count else 0 } ?: 0
        ObjectiveType.EQUIP_ITEM -> (event as? DomainEvent.EquipmentChanged)?.let {
            val itemMatches = rule.parameters["item_id"]?.let { expected -> expected == it.itemId } ?: true
            val slotMatches = rule.parameters["slot"]?.let { expected -> expected == it.slot } ?: true
            if (itemMatches && slotMatches) 1 else 0
        } ?: 0
        ObjectiveType.TALK_TO_NPC -> (event as? DomainEvent.TalkToNpc)?.let { if (it.npcId == rule.parameters["npc_id"]) 1 else 0 } ?: 0
        ObjectiveType.INTERACT_WITH_OBJECT -> (event as? DomainEvent.InteractWithObject)?.let { if (it.objectId == rule.parameters["object_id"]) 1 else 0 } ?: 0
        ObjectiveType.DISCOVER_NODE -> (event as? DomainEvent.WorldNodeDiscovered)?.let {
            val nodeMatches = rule.parameters["node_id"]?.let { expected -> expected == it.nodeId } ?: true
            val regionMatches = rule.parameters["region_id"]?.let { expected -> expected == it.regionId } ?: true
            if (nodeMatches && regionMatches) 1 else 0
        } ?: 0
        ObjectiveType.COMPLETE_EXPEDITION -> (event as? DomainEvent.ExpeditionResolved)?.let { if (it.reachBand >= (rule.parameters["minimum_band"]?.toIntOrNull() ?: 1)) 1 else 0 } ?: 0
    }
    private fun QuestObjectiveRule.intParam(name: String): Int = requireNotNull(parameters[name]?.toIntOrNull()) { "Objective $id requires numeric parameter $name" }
}
