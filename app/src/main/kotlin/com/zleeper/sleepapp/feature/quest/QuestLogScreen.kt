package com.zleeper.sleepapp.feature.quest

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.data.content.ObjectiveDefinition
import com.zleeper.sleepapp.data.content.QuestDefinition
import com.zleeper.sleepapp.data.content.RewardDefinition
import com.zleeper.sleepapp.feature.shell.ZleeperUiState

@Composable
fun QuestLogScreen(
    state: ZleeperUiState,
    onAccept: (String) -> Unit,
    onAbandon: (String) -> Unit,
    onTrack: (String, Boolean) -> Unit,
    onClaim: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("QUEST LOG", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Promises across both worlds", style = MaterialTheme.typography.headlineMedium)
            Text("Behavior quests use forgiving windows, world quests use RPG activity, and hybrid quests connect the two. No fragile consecutive-night streak is required.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.trackedQuest?.let { tracked ->
            val quest = state.quests.firstOrNull { it.id == tracked.questId }
            if (quest != null) {
                item {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Column(Modifier.padding(16.dp)) {
                            Text("TRACKED", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
                            Text(quest.name, color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
        items(state.quests, key = { it.id }) { quest ->
            val progress = state.questProgress.firstOrNull { it.questId == quest.id }
            QuestCard(
                quest = quest,
                status = progress?.status ?: "LOCKED",
                tracked = progress?.tracked == true,
                objectives = state.questObjectives.filter { it.questId == quest.id },
                state = state,
                onAccept = onAccept,
                onAbandon = onAbandon,
                onTrack = onTrack,
                onClaim = onClaim,
            )
        }
        state.operationError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
private fun QuestCard(
    quest: QuestDefinition,
    status: String,
    tracked: Boolean,
    objectives: List<com.zleeper.sleepapp.data.local.database.QuestObjectiveProgressEntity>,
    state: ZleeperUiState,
    onAccept: (String) -> Unit,
    onAbandon: (String) -> Unit,
    onTrack: (String, Boolean) -> Unit,
    onClaim: (String) -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(quest.family.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(status.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, color = statusColor(status), style = MaterialTheme.typography.labelLarge)
            }
            Text(quest.name, style = MaterialTheme.typography.titleLarge)
            Text(quest.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (quest.prerequisiteQuests.isNotEmpty()) {
                val names = quest.prerequisiteQuests.map { id -> state.quests.firstOrNull { it.id == id }?.name ?: id }
                Text("Prerequisite: ${names.joinToString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Objectives", fontWeight = FontWeight.SemiBold)
            quest.objectives.forEach { definition ->
                val progress = objectives.firstOrNull { it.objectiveId == definition.id }
                val current = progress?.currentCount ?: 0
                val required = progress?.requiredCount ?: definition.requiredCount
                Text("${if (current >= required) "✓" else "•"} ${objectiveLabel(definition, state)} · $current / $required")
            }
            Text("Rewards", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quest.rewards.forEach { reward -> RewardChip(reward, state) }
            }
            when (status) {
                "AVAILABLE" -> Button(onClick = { onAccept(quest.id) }, modifier = Modifier.fillMaxWidth()) { Text("Accept quest") }
                "ACTIVE" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (tracked) Button(onClick = { onTrack(quest.id, false) }, modifier = Modifier.weight(1f)) { Text("Untrack") }
                        else FilledTonalButton(onClick = { onTrack(quest.id, true) }, modifier = Modifier.weight(1f)) { Text("Track") }
                        OutlinedButton(onClick = { onAbandon(quest.id) }, modifier = Modifier.weight(1f)) { Text("Abandon") }
                    }
                    if (!quest.abandonResetsProgress) {
                        Text("Abandoning this quest preserves recorded progress.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                "COMPLETED" -> Button(onClick = { onClaim(quest.id) }, modifier = Modifier.fillMaxWidth()) { Text("Claim rewards") }
                "CLAIMED" -> Text("Rewards claimed", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                "LOCKED" -> Text("Complete the listed prerequisites to unlock this quest.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RewardChip(reward: RewardDefinition, state: ZleeperUiState) {
    val label = when (reward.type) {
        "ITEM" -> state.items.firstOrNull { it.id == reward.id }?.name ?: reward.id ?: "Item"
        "XP" -> "XP"
        "HEARTH_MEMORY" -> "Hearth memory"
        else -> reward.type.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text("$label ×${reward.quantity}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
    }
}

private fun objectiveLabel(definition: ObjectiveDefinition, state: ZleeperUiState): String = when (definition.type) {
    "COMPLETE_WIND_DOWN" -> "Complete wind-down"
    "COMPLETE_MORNING_REVIEW" -> "Complete morning review"
    "FINALIZE_SLEEP_SESSION" -> "Finalize a sleep session"
    "TALK_TO_NPC" -> "Talk to ${humanize(definition.parameters["npc_id"].orEmpty())}"
    "DISCOVER_NODE" -> {
        val regionId = definition.parameters["region_id"]
        "Discover a node in ${state.regions.firstOrNull { it.id == regionId }?.name ?: humanize(regionId.orEmpty())}"
    }
    "COLLECT_ITEM" -> {
        val itemId = definition.parameters["item_id"]
        "Collect ${state.items.firstOrNull { it.id == itemId }?.name ?: humanize(itemId.orEmpty())}"
    }
    "COMPLETE_EXPEDITION" -> "Complete an expedition at band ${definition.parameters["minimum_band"] ?: "required"}+"
    "COMPLETE_SCENE" -> {
        val sceneId = definition.parameters["scene_id"]
        "Complete ${state.scenes.firstOrNull { it.id == sceneId }?.name ?: humanize(sceneId.orEmpty())}"
    }
    else -> humanize(definition.type)
}

@Composable
private fun statusColor(status: String) = when (status) {
    "COMPLETED", "CLAIMED" -> MaterialTheme.colorScheme.secondary
    "ACTIVE", "AVAILABLE" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun humanize(value: String): String = value
    .removePrefix("npc_")
    .removePrefix("region_")
    .removePrefix("scene_")
    .replace('_', ' ')
    .lowercase()
    .replaceFirstChar { it.uppercase() }
