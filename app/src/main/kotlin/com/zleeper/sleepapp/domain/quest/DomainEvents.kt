package com.zleeper.sleepapp.domain.quest

sealed interface DomainEvent {
    val sourceId: String
    data class SleepSessionStarted(override val sourceId: String) : DomainEvent
    data class WindDownCompleted(override val sourceId: String) : DomainEvent
    data class SleepTimingObserved(override val sourceId: String, val timingOffsetMinutes: Int) : DomainEvent
    data class SleepSessionFinalized(override val sourceId: String) : DomainEvent
    data class MorningReviewCompleted(override val sourceId: String) : DomainEvent
    data class ConsistencyAccumulated(override val sourceId: String, val amount: Int) : DomainEvent
    data class ExpeditionStarted(override val sourceId: String) : DomainEvent
    data class ExpeditionResolved(override val sourceId: String, val reachBand: Int) : DomainEvent
    data class PetXpGranted(override val sourceId: String, val amount: Int) : DomainEvent
    data class PetLevelReached(override val sourceId: String, val level: Int) : DomainEvent
    data class PetStatReached(override val sourceId: String, val stat: String, val value: Int) : DomainEvent
    data class ItemGranted(override val sourceId: String, val itemId: String, val count: Int) : DomainEvent
    data class EquipmentChanged(override val sourceId: String, val slot: String, val itemId: String) : DomainEvent
    data class EnterRegion(override val sourceId: String, val regionId: String) : DomainEvent
    data class SceneCompleted(override val sourceId: String, val sceneId: String) : DomainEvent
    data class TalkToNpc(override val sourceId: String, val npcId: String) : DomainEvent
    data class InteractWithObject(override val sourceId: String, val objectId: String) : DomainEvent
    data class WorldNodeDiscovered(override val sourceId: String, val nodeId: String, val regionId: String) : DomainEvent
}

enum class QuestFamily { BEHAVIOR, WORLD, HYBRID }
enum class ObjectiveType {
    SLEEP_WITHIN_TARGET_WINDOW, COMPLETE_WIND_DOWN, FINALIZE_SLEEP_SESSION, COMPLETE_MORNING_REVIEW,
    ACCUMULATE_CONSISTENCY, REACH_PET_LEVEL, REACH_PET_STAT, ENTER_REGION, COMPLETE_SCENE,
    COLLECT_ITEM, EQUIP_ITEM, TALK_TO_NPC, INTERACT_WITH_OBJECT, DISCOVER_NODE, COMPLETE_EXPEDITION,
}
