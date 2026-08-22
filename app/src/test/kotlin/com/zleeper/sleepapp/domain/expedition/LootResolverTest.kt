package com.zleeper.sleepapp.domain.expedition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LootResolverTest {
    private val nodes = listOf(
        ExpeditionNode("start", "region", "START", 0, next = listOf("reward"), textKey = "start"),
        ExpeditionNode("reward", "region", "REWARD", 1, lootTableId = "loot", textKey = "reward"),
    )

    @Test
    fun sameSeedProducesSameWeightedLoot() {
        val loot = listOf(
            LootTable(
                "loot",
                rolls = 4,
                entries = listOf(
                    LootEntry("moss", weight = 3, minimum = 1, maximum = 2, material = true),
                    LootEntry("thread", weight = 1, minimum = 2, maximum = 4, material = true),
                ),
            ),
        )
        val input = ExpeditionInput("expedition", 991L, "region", budget = 5, focus = 0)

        val first = ExpeditionResolver.resolve(input, nodes, loot)
        val second = ExpeditionResolver.resolve(input, nodes, loot)

        assertEquals(first.rewards, second.rewards)
        assertTrue(first.rewards.sumOf { it.quantity } >= 4)
    }

    @Test
    fun bonusMaterialRollsIgnoreNonMaterialEntries() {
        val loot = listOf(
            LootTable(
                "loot",
                rolls = 0,
                entries = listOf(
                    LootEntry("material", weight = 1, minimum = 1, maximum = 1, material = true),
                    LootEntry("relic", weight = 100, minimum = 1, maximum = 1, material = false),
                ),
            ),
        )
        val input = ExpeditionInput(
            "expedition",
            seed = 1L,
            regionId = "region",
            budget = 5,
            focus = 0,
            bonusMaterialRolls = 3,
        )

        val result = ExpeditionResolver.resolve(input, nodes, loot)

        assertEquals(listOf("material"), result.rewards.map { it.itemId })
        assertEquals(3, result.rewards.single().quantity)
    }
}
