package com.zleeper.sleepapp.domain.expedition

import com.zleeper.sleepapp.domain.inventory.ItemGrant
import java.util.Random

data class ExpeditionNode(val id: String, val regionId: String, val type: String, val depthCost: Int, val focusRequired: Int = 0, val lootTableId: String? = null, val next: List<String> = emptyList(), val textKey: String)
data class LootEntry(val itemId: String, val weight: Int, val minimum: Int, val maximum: Int, val material: Boolean = false)
data class LootTable(val id: String, val rolls: Int, val entries: List<LootEntry>)
data class ExpeditionInput(val expeditionId: String, val seed: Long, val regionId: String, val budget: Int, val focus: Int, val bonusMaterialRolls: Int = 0, val materialCapacityBonus: Int = 0)
data class ExpeditionResult(val expeditionId: String, val path: List<ExpeditionNode>, val rewards: List<ItemGrant>)

object ExpeditionResolver {
    fun resolve(input: ExpeditionInput, nodes: List<ExpeditionNode>, lootTables: List<LootTable>): ExpeditionResult {
        val byId = nodes.filter { it.regionId == input.regionId }.associateBy { it.id }
        require(byId.isNotEmpty()) { "Region has no expedition graph" }
        val random = Random(input.seed)
        val path = mutableListOf<ExpeditionNode>()
        val rewards = mutableListOf<ItemGrant>()
        var remaining = input.budget
        var current = byId.values.firstOrNull { it.type == "START" } ?: error("Region has no START node")
        val visited = mutableSetOf<String>()
        while (current.id !in visited && current.depthCost <= remaining && input.focus >= current.focusRequired) {
            visited += current.id
            path += current
            remaining -= current.depthCost
            current.lootTableId?.let { tableId ->
                val table = lootTables.first { it.id == tableId }
                repeat(table.rolls) {
                    val total = table.entries.sumOf { entry -> entry.weight }
                    require(total > 0)
                    var roll = random.nextInt(total)
                    val entry = table.entries.first { candidate -> if (roll < candidate.weight) true else { roll -= candidate.weight; false } }
                    val qty = entry.minimum + random.nextInt(entry.maximum - entry.minimum + 1)
                    rewards += ItemGrant(entry.itemId, qty)
                }
            }
            val candidates = current.next.mapNotNull(byId::get).filter { it.depthCost <= remaining && input.focus >= it.focusRequired }
            if (candidates.isEmpty()) break
            current = candidates[random.nextInt(candidates.size)]
        }
        if (input.bonusMaterialRolls > 0) {
            val materialEntries = lootTables.flatMap { it.entries }.filter { it.material && it.weight > 0 }
            if (materialEntries.isNotEmpty()) {
                repeat(input.bonusMaterialRolls) {
                    val total = materialEntries.sumOf { it.weight }
                    var roll = random.nextInt(total)
                    val entry = materialEntries.first { candidate -> if (roll < candidate.weight) true else { roll -= candidate.weight; false } }
                    rewards += ItemGrant(entry.itemId, entry.minimum + random.nextInt(entry.maximum - entry.minimum + 1))
                }
            }
        }
        val grouped = rewards.groupBy { it.itemId }.map { (id, grants) -> ItemGrant(id, grants.sumOf { it.quantity }) }.toMutableList()
        if (input.materialCapacityBonus > 0) {
            val materialIds = lootTables.flatMap { it.entries }.filter { it.material }.mapTo(mutableSetOf()) { it.itemId }
            val firstMaterial = grouped.firstOrNull { it.itemId in materialIds }
            if (firstMaterial != null) grouped[grouped.indexOf(firstMaterial)] = firstMaterial.copy(quantity = firstMaterial.quantity + input.materialCapacityBonus)
        }
        return ExpeditionResult(input.expeditionId, path, grouped)
    }
}
