package com.zleeper.sleepapp.feature.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.feature.shell.ZleeperUiState

private enum class CollectionPage(val label: String) {
    CODEX("Codex"),
    WORLD("World"),
    FORMS("Pet forms"),
    TITLES("Titles"),
}

@Composable
fun CollectionsScreen(
    state: ZleeperUiState,
    onEquipTitle: (String) -> Unit,
) {
    var page by rememberSaveable { mutableStateOf(CollectionPage.CODEX) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("COLLECTIONS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("The world you have discovered", style = MaterialTheme.typography.headlineMedium)
            Text("Collections are permanent discovery history, not current inventory. Consumed or salvaged items remain part of the record once discovered.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionPage.entries.forEach { value ->
                    if (page == value) Button(onClick = { page = value }) { Text(value.label) }
                    else FilledTonalButton(onClick = { page = value }) { Text(value.label) }
                }
            }
        }
        when (page) {
            CollectionPage.CODEX -> {
                val discoveredItemIds = state.collectionEntries.mapTo(mutableSetOf()) { it.entryId }.intersect(state.items.map { it.id }.toSet())
                item { ProgressCard("Items and relics", discoveredItemIds.size, state.items.size) }
                val entries = state.collectionEntries.filter { entry -> state.items.any { it.id == entry.entryId } }
                if (entries.isEmpty()) {
                    item { EmptyCard("No item discoveries recorded yet.") }
                } else {
                    items(entries, key = { it.entryId }) { entry ->
                        val definition = state.items.firstOrNull { it.id == entry.entryId }
                        EntryCard(
                            title = definition?.name ?: humanize(entry.entryId),
                            subtitle = "${entry.category.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }} · first recorded ${formatEpoch(entry.firstDiscoveredAtEpochMs)}",
                            detail = definition?.description,
                        )
                    }
                }
                val storyEntries = state.collectionEntries.filter { it.category == "QUEST_STORY" }
                if (storyEntries.isNotEmpty()) {
                    item { SectionTitle("Quest stories") }
                    items(storyEntries, key = { it.entryId }) { entry ->
                        val questId = entry.entryId.removePrefix("quest_story:")
                        val quest = state.quests.firstOrNull { it.id == questId }
                        EntryCard(quest?.name ?: humanize(questId), "Quest story", quest?.description)
                    }
                }
            }
            CollectionPage.WORLD -> {
                val unlockedRegions = state.worldUnlocks.filter { it.contentType == "REGION" }.mapTo(mutableSetOf()) { it.contentId }
                item { ProgressCard("Regions", unlockedRegions.size, state.regions.size) }
                items(state.regions, key = { it.id }) { region ->
                    EntryCard(
                        title = if (region.id in unlockedRegions) region.name else "Undiscovered region",
                        subtitle = if (region.id in unlockedRegions) "Unlocked" else "Not yet discovered",
                        detail = if (region.id in unlockedRegions) region.description else null,
                    )
                }
                item { SectionTitle("World discoveries · ${state.worldDiscoveries.size}") }
                if (state.worldDiscoveries.isEmpty()) {
                    item { EmptyCard("Permanent world discoveries will appear here.") }
                } else {
                    items(state.worldDiscoveries, key = { it.discoveryId }) { discovery ->
                        val region = state.regions.firstOrNull { it.id == discovery.regionId }
                        EntryCard(
                            title = humanize(discovery.discoveryId),
                            subtitle = region?.name ?: humanize(discovery.regionId),
                            detail = "Recorded ${formatEpoch(discovery.discoveredAtEpochMs)}",
                        )
                    }
                }
                if (state.sceneCompletions.isNotEmpty()) {
                    item { SectionTitle("Completed scenes") }
                    items(state.sceneCompletions, key = { it.sceneId }) { completion ->
                        val scene = state.scenes.firstOrNull { it.id == completion.sceneId }
                        EntryCard(
                            title = scene?.name ?: humanize(completion.sceneId),
                            subtitle = "Completed ${completion.completionCount} time${if (completion.completionCount == 1) "" else "s"}",
                            detail = "First completed ${formatEpoch(completion.firstCompletedAtEpochMs)}",
                        )
                    }
                }
            }
            CollectionPage.FORMS -> {
                val discoveredForms = state.collectionEntries.filter { it.category == "PET_FORM" }.mapTo(mutableSetOf()) { it.entryId }
                item { ProgressCard("Pet forms", discoveredForms.size, state.forms.size) }
                items(state.forms, key = { it.id }) { form ->
                    val discovered = form.id in discoveredForms || state.pet?.formId == form.id
                    EntryCard(
                        title = if (discovered) form.name else "Undiscovered form",
                        subtitle = if (discovered) "Level gate ${form.levelGate}" else "Silhouette remains unknown",
                        detail = null,
                    )
                }
            }
            CollectionPage.TITLES -> {
                item { ProgressCard("Titles", state.titles.size, state.titleDefinitions.size) }
                items(state.titleDefinitions, key = { it.id }) { definition ->
                    val unlocked = state.titles.firstOrNull { it.titleId == definition.id }
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (unlocked != null) definition.name else "Locked title", fontWeight = FontWeight.SemiBold)
                                if (unlocked?.equipped == true) Text("Equipped", color = MaterialTheme.colorScheme.primary)
                            }
                            Text("${definition.conditionType.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }} · ${definition.conditionValue}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (unlocked != null && !unlocked.equipped) {
                                FilledTonalButton(onClick = { onEquipTitle(definition.id) }, modifier = Modifier.fillMaxWidth()) { Text("Equip title") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(label: String, current: Int, total: Int) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text("$current / $total", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EntryCard(title: String, subtitle: String, detail: String?) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelMedium)
            detail?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun SectionTitle(value: String) = Text(value, style = MaterialTheme.typography.titleLarge)

@Composable
private fun EmptyCard(value: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Text(value, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun humanize(value: String): String = value
    .substringAfter(':')
    .removePrefix("item_")
    .removePrefix("region_")
    .removePrefix("scene_")
    .replace('_', ' ')
    .lowercase()
    .replaceFirstChar { it.uppercase() }

private fun formatEpoch(epochMs: Long): String = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(epochMs))
