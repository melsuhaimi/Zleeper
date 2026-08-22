package com.zleeper.sleepapp.domain.expedition

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpeditionResolverTest {
    @Test
    fun sameInputAlwaysProducesSameJourneyAndRewards() {
        val nodes = listOf(
            ExpeditionNode(
                id = "node_start",
                regionId = "region_test",
                type = "START",
                depthCost = 0,
                next = listOf("node_reward"),
                textKey = "start",
            ),
            ExpeditionNode(
                id = "node_reward",
                regionId = "region_test",
                type = "REWARD",
                depthCost = 5,
                lootTableId = "loot_test",
                textKey = "reward",
            ),
        )
        val loot = listOf(
            LootTable(
                id = "loot_test",
                rolls = 2,
                entries = listOf(LootEntry("item_material_test", weight = 1, minimum = 1, maximum = 3, material = true)),
            ),
        )
        val input = ExpeditionInput(
            expeditionId = "expedition",
            seed = 42L,
            regionId = "region_test",
            budget = 10,
            focus = 1,
        )

        val first = ExpeditionResolver.resolve(input, nodes, loot)
        val second = ExpeditionResolver.resolve(input, nodes, loot)

        assertEquals(first, second)
    }

    @Test
    fun materialBonusesAreDeterministicAndGameOnly() {
        val nodes = listOf(
            ExpeditionNode("start", "region_test", "START", 0, next = listOf("reward"), textKey = "start"),
            ExpeditionNode("reward", "region_test", "REWARD", 1, lootTableId = "loot_test", textKey = "reward"),
        )
        val loot = listOf(
            LootTable("loot_test", 1, listOf(LootEntry("material", 1, 1, 1, material = true))),
        )
        val input = ExpeditionInput(
            expeditionId = "expedition",
            seed = 7L,
            regionId = "region_test",
            budget = 10,
            focus = 0,
            bonusMaterialRolls = 2,
            materialCapacityBonus = 3,
        )

        val result = ExpeditionResolver.resolve(input, nodes, loot)

        assertEquals(6, result.rewards.single().quantity)
    }
}
