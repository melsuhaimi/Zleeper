package com.zleeper.sleepapp.feature.equipment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.zleeper.sleepapp.data.content.CraftingRecipeDefinition
import com.zleeper.sleepapp.data.content.ItemDefinition
import com.zleeper.sleepapp.data.local.database.InventoryInstanceEntity
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.components.rememberAssetImage

@Composable
fun EquipmentScreen(
    state: ZleeperUiState,
    onEquip: (String) -> Unit,
    onUnequip: (String) -> Unit,
    onSynthesize: (String, String?) -> Unit,
    onEnhance: (String) -> Unit,
    onRefine: (String) -> Unit,
    onSalvage: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf("LOADOUT") }
    val equipmentInstances = state.inventoryInstances.filter { instance -> state.equipmentEffects.any { it.itemId == instance.itemId } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("EQUIPMENT", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Build the expedition kit", style = MaterialTheme.typography.headlineMedium)
            Text("Equipment affects game systems only. Synthesis creates deterministic traits; enhancement and refinement consume real stored materials.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("LOADOUT", "SYNTHESIS").forEach { value ->
                    if (tab == value) Button(onClick = { tab = value }, modifier = Modifier.weight(1f)) { Text(value.lowercase().replaceFirstChar { it.uppercase() }) }
                    else FilledTonalButton(onClick = { tab = value }, modifier = Modifier.weight(1f)) { Text(value.lowercase().replaceFirstChar { it.uppercase() }) }
                }
            }
        }
        if (tab == "LOADOUT") {
            items(listOf("HEAD", "CHARM", "PACK", "RELIC")) { slot ->
                val equippedId = state.equipmentSlots.firstOrNull { it.slot == slot }?.inventoryInstanceId
                val equipped = equipmentInstances.firstOrNull { it.instanceId == equippedId }
                val candidates = equipmentInstances.filter { instance -> state.equipmentEffects.firstOrNull { it.itemId == instance.itemId }?.slot == slot }
                SlotCard(slot, equipped, candidates, state, onEquip, onUnequip, onEnhance, onRefine, onSalvage)
            }
        } else {
            items(state.craftingRecipes, key = { it.id }) { recipe ->
                RecipeCard(recipe, state, onSynthesize)
            }
        }
        state.operationError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
private fun SlotCard(
    slot: String,
    equipped: InventoryInstanceEntity?,
    candidates: List<InventoryInstanceEntity>,
    state: ZleeperUiState,
    onEquip: (String) -> Unit,
    onUnequip: (String) -> Unit,
    onEnhance: (String) -> Unit,
    onRefine: (String) -> Unit,
    onSalvage: (String) -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(slot.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            if (equipped == null) {
                Text("Nothing equipped", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                InstanceSummary(equipped, state)
                OutlinedButton(onClick = { onUnequip(slot) }, modifier = Modifier.fillMaxWidth()) { Text("Unequip") }
                ProgressionActions(equipped, state, onEnhance, onRefine)
            }
            candidates.filterNot { it.instanceId == equipped?.instanceId }.forEach { instance ->
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InstanceSummary(instance, state)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onEquip(instance.instanceId) }, modifier = Modifier.weight(1f)) { Text("Equip") }
                            OutlinedButton(onClick = { onSalvage(instance.instanceId) }, modifier = Modifier.weight(1f)) { Text("Salvage") }
                        }
                        ProgressionActions(instance, state, onEnhance, onRefine)
                    }
                }
            }
        }
    }
}

@Composable
private fun InstanceSummary(instance: InventoryInstanceEntity, state: ZleeperUiState) {
    val item = state.items.firstOrNull { it.id == instance.itemId }
    val effect = state.equipmentEffects.firstOrNull { it.itemId == instance.itemId }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ItemArt(item, Modifier.size(58.dp))
        Column(Modifier.weight(1f)) {
            Text(item?.name ?: humanize(instance.itemId), fontWeight = FontWeight.SemiBold)
            Text("Tier ${roman(instance.tier)} · +${instance.upgradeLevel} · Quality ${instance.quality}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            effect?.let { Text("${humanize(it.effect)} ${signed(it.amount)}", style = MaterialTheme.typography.labelMedium) }
            instance.traitId?.let { traitId ->
                val trait = state.equipmentTraits.firstOrNull { it.id == traitId }
                Text("Trait · ${trait?.name ?: humanize(traitId)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            instance.infusionId?.let { infusionId ->
                val infusion = state.equipmentInfusions.firstOrNull { it.id == infusionId }
                Text("Infusion · ${infusion?.name ?: humanize(infusionId)}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ProgressionActions(
    instance: InventoryInstanceEntity,
    state: ZleeperUiState,
    onEnhance: (String) -> Unit,
    onRefine: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
            onClick = { onEnhance(instance.instanceId) },
            enabled = instance.upgradeLevel < state.equipmentMaxUpgradeLevel,
            modifier = Modifier.weight(1f),
        ) { Text(if (instance.upgradeLevel >= state.equipmentMaxUpgradeLevel) "Max +${state.equipmentMaxUpgradeLevel}" else "Enhance") }
        FilledTonalButton(
            onClick = { onRefine(instance.instanceId) },
            enabled = instance.upgradeLevel >= state.equipmentMaxUpgradeLevel,
            modifier = Modifier.weight(1f),
        ) { Text("Refine tier") }
    }
}

@Composable
private fun RecipeCard(
    recipe: CraftingRecipeDefinition,
    state: ZleeperUiState,
    onSynthesize: (String, String?) -> Unit,
) {
    val output = state.items.firstOrNull { it.id == recipe.outputItemId }
    var infusion by rememberSaveable(recipe.id) { mutableStateOf<String?>(null) }
    val petLevel = state.pet?.level ?: 0
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemArt(output, Modifier.size(64.dp))
                Column(Modifier.weight(1f)) {
                    Text(output?.name ?: humanize(recipe.outputItemId), style = MaterialTheme.typography.titleLarge)
                    Text("Requires companion level ${recipe.minimumLevel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Base materials", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            recipe.costs.forEach { cost ->
                val item = state.items.firstOrNull { it.id == cost.itemId }
                val owned = state.inventoryStacks.firstOrNull { it.itemId == cost.itemId }?.quantity ?: 0
                Text("${item?.name ?: humanize(cost.itemId)} · $owned / ${cost.quantity}")
            }
            if (recipe.allowedInfusions.isNotEmpty()) {
                Text("Infusion", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Optional. Catalyst cost is enforced by synthesis.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (infusion == null) Button(onClick = { infusion = null }) { Text("None") }
                    else FilledTonalButton(onClick = { infusion = null }) { Text("None") }
                    recipe.allowedInfusions.forEach { id ->
                        val definition = state.equipmentInfusions.firstOrNull { it.id == id }
                        if (infusion == id) Button(onClick = { infusion = id }) { Text(definition?.name ?: humanize(id)) }
                        else FilledTonalButton(onClick = { infusion = id }) { Text(definition?.name ?: humanize(id)) }
                    }
                }
            }
            Button(
                onClick = { onSynthesize(recipe.id, infusion) },
                enabled = petLevel >= recipe.minimumLevel,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Synthesize") }
            Text("Actual material cost uses the current specialization discounts in the deterministic service.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ItemArt(item: ItemDefinition?, modifier: Modifier = Modifier) {
    if (item == null) return
    val image = rememberAssetImage("game/${item.iconAsset}")
    if (image != null) Image(image, contentDescription = item.name, modifier = modifier)
}

private fun humanize(value: String): String = value.substringAfterLast('_').replaceFirstChar { it.uppercase() }
private fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
private fun roman(value: Int): String = when (value) { 1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"; 5 -> "V"; else -> value.toString() }
