package com.zleeper.sleepapp.domain.expedition

import com.zleeper.sleepapp.data.content.ExpeditionNodeDefinition
import com.zleeper.sleepapp.data.content.LootEntryDefinition
import com.zleeper.sleepapp.data.content.LootTableDefinition
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpeditionResolverTest {
    @Test fun sameInputAlwaysProducesSameJourneyAndRewards() {
        val nodes = listOf(ExpeditionNodeDefinition("node_start", "region_test", "START", 0, next = listOf("node_reward"), textKey = "start"), ExpeditionNodeDefinition("node_reward", "region_test", "REWARD", 5, lootTableId = "loot_test", textKey = "reward"))
        val loot = listOf(LootTableDefinition("loot_test", 2, listOf(LootEntryDefinition("item_material_test", 1, 1, 3))))
        val input = ExpeditionInput("expedition", 42, "region_test", 10, 1)
        val resolver = ExpeditionResolver()
        assertEquals(resolver.resolve(input, nodes, loot), resolver.resolve(input, nodes, loot))
    }
}
