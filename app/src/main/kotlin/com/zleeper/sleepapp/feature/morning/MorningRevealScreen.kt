package com.zleeper.sleepapp.feature.morning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.components.PetPose
import com.zleeper.sleepapp.ui.components.PetSprite
import com.zleeper.sleepapp.ui.components.StorybookBackdrop

@Composable
fun MorningRevealScreen(
    state: ZleeperUiState,
    onReturnToHearth: () -> Unit,
) {
    val result = requireNotNull(state.morningResult)
    val pet = state.pet
    val formKey = pet?.let { current -> state.forms.firstOrNull { it.id == current.formId }?.assetKey } ?: "glimmerling"
    val regionName = state.regions.firstOrNull { it.id == result.regionId }?.name ?: result.regionId
    var showJourney by rememberSaveable(result.expeditionId) { mutableStateOf(false) }

    StorybookBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("morning-reveal"),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text("AT FIRST LIGHT", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text("${pet?.displayName ?: "Your companion"} came home", style = MaterialTheme.typography.headlineMedium)
                Text("The saved expedition has resolved. Everything below comes from that immutable result; viewing it does not run rewards again.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                    PetSprite(formKey, PetPose.CELEBRATE, frame = 0, modifier = Modifier.size(220.dp))
                }
            }
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("JOURNEY RESULT", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text(regionName, style = MaterialTheme.typography.titleLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ResultChip("Reach", reachLabel(result.reachBand))
                            ResultChip("XP", "+${result.xp}")
                            ResultChip("Finds", result.rewards.sumOf { it.second }.toString())
                        }
                    }
                }
            }
            if (result.newLevel > result.previousLevel || result.dreamSparksGained > 0 || result.statChanges.any { it.gained > 0 }) {
                item {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("COMPANION GROWTH", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            if (result.newLevel > result.previousLevel) {
                                Text("Level ${result.previousLevel} → ${result.newLevel}", style = MaterialTheme.typography.titleLarge)
                            }
                            if (result.dreamSparksGained > 0) {
                                Text("+${result.dreamSparksGained} Dream Spark${if (result.dreamSparksGained == 1) "" else "s"}")
                            }
                            result.statChanges.filter { it.gained > 0 }.forEach { change ->
                                Text("${humanize(change.stat)} ${change.previous} → ${change.current}")
                            }
                        }
                    }
                }
            }
            if (result.rewards.isNotEmpty()) {
                item { Text("Returned with", style = MaterialTheme.typography.titleLarge) }
                items(result.rewards, key = { it.first }) { reward ->
                    val item = state.items.firstOrNull { it.id == reward.first }
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${reward.second} × ${item?.name ?: humanize(reward.first)}", fontWeight = FontWeight.SemiBold)
                            item?.let { Text(it.description, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
            if (result.newDiscoveries.isNotEmpty() || result.hearthMemoriesGained > 0) {
                item {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("THE WORLD REMEMBERS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            if (result.newDiscoveries.isNotEmpty()) {
                                Text("${result.newDiscoveries.size} new discover${if (result.newDiscoveries.size == 1) "y" else "ies"}", style = MaterialTheme.typography.titleMedium)
                                result.newDiscoveries.forEach { discovery -> Text("• ${humanize(discovery)}") }
                            }
                            if (result.hearthMemoriesGained > 0) {
                                Text("+${result.hearthMemoriesGained} Hearth memories")
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showJourney = !showJourney },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (showJourney) "Hide journey" else "View journey")
                }
            }
            if (showJourney) {
                item {
                    Text("Saved journey", style = MaterialTheme.typography.titleLarge)
                    Text("A reconstruction of the already-resolved expedition path.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(result.journey, key = { it.nodeId }) { step ->
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(humanize(step.nodeType), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            Text(step.narrative, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            item {
                Button(onClick = onReturnToHearth, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Return to the Hearth")
                }
            }
        }
    }
}

@Composable
private fun ResultChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

private fun reachLabel(band: Int): String = when (band) {
    1 -> "Trail"
    2 -> "Path"
    3 -> "Depth"
    4 -> "Far Reach"
    5 -> "Veil"
    else -> "Band $band"
}

private fun humanize(value: String): String = value
    .removePrefix("node_")
    .removePrefix("item_")
    .replace('_', ' ')
    .lowercase()
    .replaceFirstChar { it.uppercase() }
