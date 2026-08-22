package com.zleeper.sleepapp.feature.inventory

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.data.content.ItemDefinition
import com.zleeper.sleepapp.data.local.database.InventoryInstanceEntity
import com.zleeper.sleepapp.data.local.database.InventoryTransactionEntity
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.components.rememberAssetImage
import java.text.DateFormat
import java.util.Date

private const val FILTER_ALL = "ALL"

@Composable
fun InventoryScreen(state: ZleeperUiState) {
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    val categories = listOf(FILTER_ALL) + state.items.map { it.category }.distinct().sorted()
    val stacks = state.inventoryStacks
        .filter { it.quantity > 0 }
        .filter { stack -> filter == FILTER_ALL || state.items.firstOrNull { it.id == stack.itemId }?.category == filter }
    val instances = state.inventoryInstances
        .filter { instance -> filter == FILTER_ALL || state.items.firstOrNull { it.id == instance.itemId }?.category == filter }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("INVENTORY", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("What you own now", style = MaterialTheme.typography.headlineMedium)
            Text("Stacks and individual equipment live here. Collections remain a separate permanent record of what you have discovered.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    val label = if (category == FILTER_ALL) "All" else category.lowercase().replaceFirstChar { it.uppercase() }
                    if (filter == category) Button(onClick = { filter = category }) { Text(label) }
                    else FilledTonalButton(onClick = { filter = category }) { Text(label) }
                }
            }
        }
        if (stacks.isEmpty() && instances.isEmpty()) {
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Nothing in this category yet", style = MaterialTheme.typography.titleLarge)
                        Text("Expedition rewards, waking-world finds, quest rewards and synthesis output appear here when they are actually owned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(stacks, key = { "stack:${it.itemId}" }) { stack ->
            val item = state.items.firstOrNull { it.id == stack.itemId } ?: return@items
            val provenance = state.inventoryTransactions.firstOrNull { it.itemId == stack.itemId && it.quantityDelta > 0 }
            StackCard(item, stack.quantity, provenance)
        }
        items(instances, key = { it.instanceId }) { instance ->
            val item = state.items.firstOrNull { it.id == instance.itemId } ?: return@items
            val provenance = state.inventoryTransactions.firstOrNull { transaction ->
                transaction.instanceId == instance.instanceId && transaction.quantityDelta > 0
            }
            InstanceCard(item, instance, provenance)
        }
    }
}

@Composable
private fun StackCard(item: ItemDefinition, quantity: Int, provenance: InventoryTransactionEntity?) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemArt(item, Modifier.size(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.name, fontWeight = FontWeight.SemiBold)
                    Text("×$quantity", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text("${raritySymbol(item.rarity)} ${item.rarity.lowercase().replaceFirstChar { it.uppercase() }} · ${item.category.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelMedium)
                Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Provenance(provenance)
            }
        }
    }
}

@Composable
private fun InstanceCard(item: ItemDefinition, instance: InventoryInstanceEntity, provenance: InventoryTransactionEntity?) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ItemArt(item, Modifier.size(64.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text("${raritySymbol(item.rarity)} ${item.rarity.lowercase().replaceFirstChar { it.uppercase() }} · ${item.category.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelMedium)
                if (item.category == "EQUIPMENT" || item.category == "RELIC") {
                    Text("Tier ${instance.tier} · +${instance.upgradeLevel} · Quality ${instance.quality}", color = MaterialTheme.colorScheme.primary)
                    instance.equippedSlot?.let { Text("Equipped · ${it.lowercase().replaceFirstChar { ch -> ch.uppercase() }}") }
                }
                Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Provenance(provenance)
            }
        }
    }
}

@Composable
private fun Provenance(transaction: InventoryTransactionEntity?) {
    if (transaction == null) {
        Text("Acquisition source unavailable in the retained ledger.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val reason = transaction.reason.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(transaction.occurredAtEpochMs))
    Text("$reason · $date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    Text("Source: ${transaction.sourceId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ItemArt(item: ItemDefinition, modifier: Modifier) {
    val image = rememberAssetImage("game/${item.iconAsset}")
    if (image != null) Image(image, contentDescription = item.name, modifier = modifier)
}

private fun raritySymbol(rarity: String): String = when (rarity) {
    "COMMON" -> "●"
    "UNCOMMON" -> "◆"
    "RARE" -> "✦"
    "EPIC" -> "✧"
    "MYTHIC" -> "★"
    else -> "•"
}
