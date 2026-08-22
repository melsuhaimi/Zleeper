package com.zleeper.sleepapp.domain.quest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestEngineTest {
    @Test
    fun availableQuestCanBeAcceptedTrackedCompletedAndClaimed() {
        val rule = QuestObjectiveRule("wind_down", ObjectiveType.COMPLETE_WIND_DOWN, 1)
        val available = quest(QuestLifecycle.AVAILABLE, rule)

        val active = QuestEngine.accept(available)
        val tracked = QuestEngine.track(active, true)
        val completed = QuestEngine.apply(tracked, listOf(rule), DomainEvent.WindDownCompleted("night-1"))
        val claimed = QuestEngine.claim(completed)

        assertEquals(QuestLifecycle.ACTIVE, active.lifecycle)
        assertTrue(tracked.tracked)
        assertEquals(QuestLifecycle.COMPLETED, completed.lifecycle)
        assertEquals(1, completed.objectives.single().currentCount)
        assertEquals(QuestLifecycle.CLAIMED, claimed.lifecycle)
        assertFalse(claimed.tracked)
    }

    @Test
    fun nonMatchingTypedEventDoesNotAdvanceObjective() {
        val rule = QuestObjectiveRule(
            id = "collect",
            type = ObjectiveType.COLLECT_ITEM,
            requiredCount = 3,
            parameters = mapOf("item_id" to "item_material_dewdrop"),
        )
        val active = quest(QuestLifecycle.ACTIVE, rule)

        val unchanged = QuestEngine.apply(
            active,
            listOf(rule),
            DomainEvent.ItemGranted("source", "item_material_softmoss", 3),
        )

        assertEquals(active, unchanged)
    }

    @Test
    fun matchingItemCountClampsAtRequiredCountAndCompletesQuest() {
        val rule = QuestObjectiveRule(
            id = "collect",
            type = ObjectiveType.COLLECT_ITEM,
            requiredCount = 3,
            parameters = mapOf("item_id" to "item_material_dewdrop"),
        )
        val active = quest(QuestLifecycle.ACTIVE, rule)

        val completed = QuestEngine.apply(
            active,
            listOf(rule),
            DomainEvent.ItemGranted("source", "item_material_dewdrop", 8),
        )

        assertEquals(3, completed.objectives.single().currentCount)
        assertEquals(QuestLifecycle.COMPLETED, completed.lifecycle)
    }

    @Test
    fun abandonPreservesProgressUnlessQuestExplicitlyResetsIt() {
        val rule = QuestObjectiveRule("wind_down", ObjectiveType.COMPLETE_WIND_DOWN, 3)
        val active = QuestState(
            questId = "quest",
            lifecycle = QuestLifecycle.ACTIVE,
            tracked = true,
            objectives = listOf(QuestObjectiveState(rule.id, currentCount = 2, requiredCount = 3)),
        )

        val preserved = QuestEngine.abandon(active, resetProgress = false)
        val reset = QuestEngine.abandon(active, resetProgress = true)

        assertEquals(QuestLifecycle.AVAILABLE, preserved.lifecycle)
        assertFalse(preserved.tracked)
        assertEquals(2, preserved.objectives.single().currentCount)
        assertEquals(0, reset.objectives.single().currentCount)
    }

    @Test
    fun inactiveQuestIgnoresDomainEvents() {
        val rule = QuestObjectiveRule("finalize", ObjectiveType.FINALIZE_SLEEP_SESSION, 1)
        val available = quest(QuestLifecycle.AVAILABLE, rule)

        val result = QuestEngine.apply(available, listOf(rule), DomainEvent.SleepSessionFinalized("night-1"))

        assertEquals(available, result)
    }

    @Test
    fun sleepTimingObjectiveUsesConfiguredTolerance() {
        val rule = QuestObjectiveRule(
            id = "timing",
            type = ObjectiveType.SLEEP_WITHIN_TARGET_WINDOW,
            requiredCount = 1,
            parameters = mapOf("tolerance_minutes" to "30"),
        )
        val active = quest(QuestLifecycle.ACTIVE, rule)

        val outside = QuestEngine.apply(active, listOf(rule), DomainEvent.SleepTimingObserved("night-1", 31))
        val inside = QuestEngine.apply(active, listOf(rule), DomainEvent.SleepTimingObserved("night-2", 30))

        assertEquals(0, outside.objectives.single().currentCount)
        assertEquals(QuestLifecycle.COMPLETED, inside.lifecycle)
    }

    private fun quest(lifecycle: QuestLifecycle, rule: QuestObjectiveRule) = QuestState(
        questId = "quest",
        lifecycle = lifecycle,
        tracked = false,
        objectives = listOf(QuestObjectiveState(rule.id, currentCount = 0, requiredCount = rule.requiredCount)),
    )
}
