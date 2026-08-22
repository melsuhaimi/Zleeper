package com.zleeper.sleepapp.feature.pet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.zleeper.sleepapp.domain.progression.LevelCurve
import com.zleeper.sleepapp.domain.progression.ProgressionCalculator
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.components.PetPose
import com.zleeper.sleepapp.ui.components.PetSprite
import com.zleeper.sleepapp.ui.components.ProgressTrack

@Composable
fun PetProfileScreen(
    state: ZleeperUiState,
    onRename: (String) -> Unit,
    onSpendSpark: (String) -> Unit,
    onChooseEvolution: (String) -> Unit,
) {
    val pet = state.pet ?: return
    val currentForm = state.forms.firstOrNull { it.id == pet.formId } ?: return
    val progression = state.progressionRules ?: return
    val curve = progression.levelCurve.let { LevelCurve(it.base, it.linear, it.exponent) }
    val xpIntoLevel = ProgressionCalculator.xpIntoLevel(pet.totalXp, pet.level, curve)
    val xpToNext = ProgressionCalculator.xpToNextLevel(pet.level, curve)
    var name by rememberSaveable(pet.instanceId) { mutableStateOf(pet.displayName) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("COMPANION", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text(pet.displayName, style = MaterialTheme.typography.headlineMedium)
            Text("${currentForm.name} · Level ${pet.level}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        PetSprite(currentForm.assetKey, PetPose.IDLE, frame = 0, modifier = Modifier.size(210.dp))
                    }
                    Text("XP $xpIntoLevel / $xpToNext", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    ProgressTrack((xpIntoLevel.toFloat() / xpToNext.coerceAtLeast(1)).coerceIn(0f, 1f), MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Core affinities", style = MaterialTheme.typography.titleLarge)
                    Text("These grow from configured behavior affinities; Dream Sparks do not replace them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Energy", pet.energy, pet.energyAffinity, progression.statGrowthRates.energy, "Expedition reach")
                        StatCard("Focus", pet.focus, pet.focusAffinity, progression.statGrowthRates.focus, "Hidden routes")
                        StatCard("Resilience", pet.resilience, pet.resilienceAffinity, progression.statGrowthRates.resilience, "Hazard resistance")
                    }
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Dream Sparks · ${pet.dreamSparksAvailable}", style = MaterialTheme.typography.titleLarge)
                    Text("Spend level-up Sparks on game-only specializations. Core sleep-linked affinities remain automatic.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(state.specializations, key = { it.id }) { node ->
            val rank = state.petSpecializations.firstOrNull { it.nodeId == node.id }?.rank ?: 0
            val prerequisitesMet = node.prerequisites.all { required -> (state.petSpecializations.firstOrNull { it.nodeId == required }?.rank ?: 0) > 0 }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(node.name, fontWeight = FontWeight.SemiBold)
                        Text("$rank / ${node.maxRank}", color = MaterialTheme.colorScheme.primary)
                    }
                    Text(node.path.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(node.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(
                        onClick = { onSpendSpark(node.id) },
                        enabled = rank < node.maxRank && prerequisitesMet && pet.dreamSparksAvailable >= node.costPerRank,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (rank >= node.maxRank) "Max rank" else "Spend ${node.costPerRank} Dream Spark${if (node.costPerRank == 1) "" else "s"}")
                    }
                }
            }
        }
        item {
            Text("Evolution", style = MaterialTheme.typography.titleLarge)
            Text("You choose among forms whose defined level gate is met; Zleeper never assigns a ‘bad’ form from poor sleep.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(state.forms.filter { it.speciesId == pet.speciesId }, key = { it.id }) { form ->
            val current = form.id == pet.formId
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PetSprite(form.assetKey, PetPose.IDLE, frame = 0, modifier = Modifier.size(76.dp))
                    Column(Modifier.weight(1f)) {
                        Text(form.name, fontWeight = FontWeight.SemiBold)
                        Text("Level ${form.levelGate}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    when {
                        current -> Text("Current", color = MaterialTheme.colorScheme.primary)
                        pet.level >= form.levelGate -> Button(onClick = { onChooseEvolution(form.id) }) { Text("Choose") }
                        else -> Text("Locked", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Text("Recent memories", style = MaterialTheme.typography.titleLarge) }
        if (state.petMemories.isEmpty()) {
            item { Text("Shared memories will appear as the world changes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.petMemories.take(8), key = { it.id }) { memory ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(14.dp)) {
                        Text(memory.memoryType.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                        memory.detail?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Rename companion", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { onRename(name) }, enabled = name.trim().length >= 2, modifier = Modifier.fillMaxWidth()) { Text("Save name") }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, stat: Int, affinity: Int, growthRate: Int, detail: String) {
    val nextThreshold = growthRate * (stat + 1)
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text("$label $stat", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("$affinity / $nextThreshold affinity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
