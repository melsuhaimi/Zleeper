package com.zleeper.sleepapp.domain.expedition

import com.zleeper.sleepapp.data.content.ExpeditionNodeDefinition
import com.zleeper.sleepapp.data.content.LootTableDefinition
import com.zleeper.sleepapp.domain.inventory.ItemGrant
import java.util.Random
import javax.inject.Inject

data class ExpeditionInput(val expeditionId: String, val seed: Long, val regionId: String, val budget: Int, val focus: Int)
data class ExpeditionResult(val expeditionId: String, val path: List<ExpeditionNodeDefinition>, val rewards: List<ItemGrant>)

class ExpeditionResolver @Inject constructor() {
    fun resolve(input: ExpeditionInput, nodes: List<ExpeditionNodeDefinition>, lootTables: List<LootTableDefinition>): ExpeditionResult {
        val byId = nodes.filter { it.regionId == input.regionId }.associateBy { it.id }
        require(byId.isNotEmpty()) { "Region has no expedition graph" }
        val random = Random(input.seed)
        val path = mutableListOf<ExpeditionNodeDefinition>()
        val rewards = mutableListOf<ItemGrant>()
        var remaining = input.budget
        var current = byId.values.firstOrNull { it.type == "START" } ?: error("Region has no START node")
        val visited = mutableSetOf<String>()
        while (current.id !in visited && current.depthCost <= remaining && (current.focusRequired == 0 || input.focus >= current.focusRequired)) {
            visited += current.id
            path += current
            remaining -= current.depthCost
            current.lootTableId?.let { tableId ->
                val table = lootTables.first { it.id == tableId }
                repeat(table.rolls) {
                    val total = table.entries.sumOf { entry -> entry.weight }
                    var roll = random.nextInt(total)
                    val entry = table.entries.first { candidate -> roll < candidate.weight || run { roll -= candidate.weight; false } }
                    rewards += ItemGrant(entry.itemId, entry.minimum + random.nextInt(entry.maximum - entry.minimum + 1))
                }
            }
            val candidates = current.next.mapNotNull(byId::get).filter { it.depthCost <= remaining && (it.focusRequired == 0 || input.focus >= it.focusRequired) }
            if (candidates.isEmpty()) break
            current = candidates[random.nextInt(candidates.size)]
        }
        return ExpeditionResult(input.expeditionId, path, rewards.groupBy { it.itemId }.map { (id, grants) -> ItemGrant(id, grants.sumOf { it.quantity }) })
    }
}
