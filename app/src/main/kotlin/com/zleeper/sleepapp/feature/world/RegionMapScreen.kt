package com.zleeper.sleepapp.feature.world

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.data.content.RegionDefinition
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.components.StorybookBackdrop
import com.zleeper.sleepapp.ui.components.rememberAssetImage

@Composable
fun RegionMapScreen(
    state: ZleeperUiState,
    onBack: () -> Unit,
    onOpenScene: (String) -> Unit,
) {
    val petLevel = state.pet?.level ?: return
    StorybookBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("region-map"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Hearth")
                    }
                    Column {
                        Text("REGION MAP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        Text("Choose a waking trail", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            state.trackedQuest?.let { tracked ->
                val quest = state.quests.firstOrNull { it.id == tracked.questId }
                if (quest != null) {
                    item { Text("Tracked quest · ${quest.name}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            items(state.regions, key = { it.id }) { region ->
                RegionMapCard(region, unlocked = petLevel >= region.unlockLevel, onOpenScene = onOpenScene)
            }
        }
    }
}

@Composable
private fun RegionMapCard(
    region: RegionDefinition,
    unlocked: Boolean,
    onOpenScene: (String) -> Unit,
) {
    val image = rememberAssetImage("game/atlas/region/region_${region.id.removePrefix("region_")}_bg_far.webp")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (image != null) {
                Image(image, contentDescription = region.name, modifier = Modifier.fillMaxWidth().size(180.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(region.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    if (unlocked) "Open" else "Level ${region.unlockLevel}",
                    color = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(region.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { region.scenes.firstOrNull()?.let(onOpenScene) },
                enabled = unlocked && region.scenes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (unlocked) "Enter ${region.name}" else "Locked")
            }
        }
    }
}
