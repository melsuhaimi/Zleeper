package com.zleeper.sleepapp.domain

import com.zleeper.sleepapp.domain.equipment.*
import com.zleeper.sleepapp.domain.expedition.*
import com.zleeper.sleepapp.domain.pet.*
import com.zleeper.sleepapp.domain.progression.*
import com.zleeper.sleepapp.domain.quest.*
import com.zleeper.sleepapp.domain.sleep.*
import com.zleeper.sleepapp.domain.world.*

fun main() {
    check(SleepSchedule.durationBetween(22 * 60, 6 * 60) == 480)
    check(runCatching { SleepSchedule.validateConfiguredWindow(8 * 60, 6 * 60) }.isFailure)
    val rules = ProgressionRules(version = 2)
    check(ProgressionCalculator.xpToNextLevel(1, rules.levelCurve) > 120)
    check(LevelProgress.advance(1, 0, 1000, rules).dreamSparksGranted > 0)
    val growth = PetStatGrowth.apply(3, 0, 100, 20)
    check(growth.stat > 3 && growth.affinityRemainder >= 0)
    check(GameEffectMapper.one("material_capacity", 3).materialCapacity == 3)
    val gear = EquipmentState(tier = 1, enhancement = 10)
    check(EquipmentProgression.refine(gear).tier == 2)
    val q = QuestState("q", QuestLifecycle.ACTIVE, true, listOf(QuestObjectiveState("o", 0, 1)))
    val q2 = QuestEngine.apply(q, listOf(QuestObjectiveRule("o", ObjectiveType.COMPLETE_WIND_DOWN, 1)), DomainEvent.WindDownCompleted("s"))
    check(q2.lifecycle == QuestLifecycle.COMPLETED)
    val timed = QuestState("t", QuestLifecycle.ACTIVE, false, listOf(QuestObjectiveState("tw",0,1)))
    check(QuestEngine.apply(timed, listOf(QuestObjectiveRule("tw", ObjectiveType.SLEEP_WITHIN_TARGET_WINDOW, 1, mapOf("tolerance_minutes" to "30"))), DomainEvent.SleepTimingObserved("s", 20)).lifecycle == QuestLifecycle.COMPLETED)
    val stages = listOf(HearthStage("a", "Nest", 0, emptySet()), HearthStage("b", "Hearth", 5, setOf("fireflies")))
    check(HearthProgression.stageFor(6, stages).id == "b")
    val moment = CompanionDirector.moment(1, 2, listOf(CompanionMemoryContext("DISCOVERY", "x", "thought.recent_discovery")), reducedMotion = true)
    check(moment.activity == CompanionActivity.REST && moment.thoughtKey != null)
    val nodes = listOf(ExpeditionNode("s","r","START",0,next=listOf("d"),textKey="start"), ExpeditionNode("d","r","DESTINATION",1,lootTableId="l",textKey="done"))
    val loot = listOf(LootTable("l",1,listOf(LootEntry("m",1,1,1, material = true))))
    val a = ExpeditionResolver.resolve(ExpeditionInput("e", 42, "r", 10, 10, materialCapacityBonus = 2), nodes, loot)
    val b = ExpeditionResolver.resolve(ExpeditionInput("e", 42, "r", 10, 10, materialCapacityBonus = 2), nodes, loot)
    check(a == b && a.rewards.single().quantity == 3)
    println("DURABLE_DOMAIN_RECOVERY_OK")
}
