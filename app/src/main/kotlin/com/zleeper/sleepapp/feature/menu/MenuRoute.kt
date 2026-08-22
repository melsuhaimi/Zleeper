package com.zleeper.sleepapp.feature.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.feature.collection.CollectionsScreen
import com.zleeper.sleepapp.feature.equipment.EquipmentScreen
import com.zleeper.sleepapp.feature.inventory.InventoryScreen
import com.zleeper.sleepapp.feature.pet.PetProfileScreen
import com.zleeper.sleepapp.feature.quest.QuestLogScreen
import com.zleeper.sleepapp.feature.settings.DataManagementScreen
import com.zleeper.sleepapp.feature.settings.PermissionsScreen
import com.zleeper.sleepapp.feature.settings.SettingsScreen
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.feature.shell.ZleeperViewModel

private enum class MenuDestination(val title: String, val subtitle: String, val icon: ImageVector) {
    PET("Pet profile", "Growth, memories and specialization", Icons.Outlined.Pets),
    EQUIPMENT("Equipment & synthesis", "Loadout, crafting and refinement", Icons.Outlined.Backpack),
    INVENTORY("Inventory", "What you own now", Icons.Outlined.Inventory2),
    QUESTS("Quest log", "Behavior, world and hybrid quests", Icons.AutoMirrored.Outlined.MenuBook),
    COLLECTIONS("Collections", "Permanent discoveries and titles", Icons.Outlined.CollectionsBookmark),
    SETTINGS("Settings", "Routine, audio and accessibility", Icons.Outlined.Settings),
    PERMISSIONS("Permissions", "Optional Android capabilities", Icons.Outlined.Security),
    DATA("Data management", "Export, delete and reset local data", Icons.Outlined.Download),
}

@Composable
fun MenuRoute(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    var destination by rememberSaveable { mutableStateOf<MenuDestination?>(null) }
    BackHandler(enabled = destination != null) { destination = null }

    Column(Modifier.fillMaxSize()) {
        destination?.let { current ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { destination = null }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to Menu")
                }
                Text(current.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (destination) {
                null -> MenuHomeScreen(state = state, onOpen = { destination = it })
                MenuDestination.PET -> PetProfileScreen(
                    state = state,
                    onRename = { viewModel.renamePet(it) },
                    onSpendSpark = { viewModel.spendDreamSpark(it) },
                    onChooseEvolution = { viewModel.chooseEvolution(it) },
                )
                MenuDestination.EQUIPMENT -> EquipmentScreen(
                    state = state,
                    onEquip = { viewModel.equip(it) },
                    onUnequip = { viewModel.unequip(it) },
                    onSynthesize = { recipeId, infusionId -> viewModel.synthesize(recipeId, infusionId) },
                    onEnhance = { viewModel.enhance(it) },
                    onRefine = { viewModel.refine(it) },
                    onSalvage = { viewModel.salvage(it) },
                )
                MenuDestination.INVENTORY -> InventoryScreen(state)
                MenuDestination.QUESTS -> QuestLogScreen(
                    state = state,
                    onAccept = { viewModel.acceptQuest(it) },
                    onAbandon = { viewModel.abandonQuest(it) },
                    onTrack = { questId, tracked -> viewModel.trackQuest(questId, tracked) },
                    onClaim = { viewModel.claimQuest(it) },
                )
                MenuDestination.COLLECTIONS -> CollectionsScreen(
                    state = state,
                    onEquipTitle = { viewModel.equipTitle(it) },
                )
                MenuDestination.SETTINGS -> SettingsScreen(
                    state = state,
                    onSaveSleepPlan = { sleep, wake -> viewModel.saveSleepPlan(sleep, wake) },
                    onAlarmEnabled = { viewModel.setAlarmEnabled(it) },
                    onBedtimeReminderEnabled = { viewModel.setBedtimeReminderEnabled(it) },
                    onWindDownReminderEnabled = { viewModel.setWindDownReminderEnabled(it) },
                    onMorningResultsEnabled = { viewModel.setMorningResultNotificationsEnabled(it) },
                    onMusicVolume = { viewModel.setMusicVolume(it) },
                    onAmbienceVolume = { viewModel.setAmbienceVolume(it) },
                    onSfxVolume = { viewModel.setSfxVolume(it) },
                    onLargeControls = { viewModel.setLargeControls(it) },
                    onLeftHandedControls = { viewModel.setLeftHandedControls(it) },
                    onControlOpacity = { viewModel.setControlOpacity(it) },
                    onHaptics = { viewModel.setHapticsEnabled(it) },
                    onScreenShake = { viewModel.setScreenShakeEnabled(it) },
                    onReducedMotion = { viewModel.setReducedMotion(it) },
                    onTheme = { viewModel.setTheme(it) },
                )
                MenuDestination.PERMISSIONS -> PermissionsScreen(
                    state = state,
                    onActivityPermissionExplained = viewModel::markActivityPermissionExplained,
                    onNotificationPermissionExplained = viewModel::markNotificationPermissionExplained,
                )
                MenuDestination.DATA -> DataManagementScreen(
                    state = state,
                    onExport = { onReady -> viewModel.exportData(onReady) },
                    onDeleteSleepHistory = { viewModel.deleteSleepHistory() },
                    onResetGameProgress = { viewModel.resetGameProgress() },
                    onDeleteAllData = { viewModel.deleteAllData() },
                )
            }
        }
    }
}

@Composable
private fun MenuHomeScreen(state: ZleeperUiState, onOpen: (MenuDestination) -> Unit) {
    val pet = state.pet
    val equippedTitle = state.titles.firstOrNull { it.equipped }
        ?.let { unlocked -> state.titleDefinitions.firstOrNull { it.id == unlocked.titleId }?.name }
    val activeQuestCount = state.questProgress.count { it.status == "ACTIVE" || it.status == "COMPLETED" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("MENU", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Your Zleeper world", style = MaterialTheme.typography.headlineMedium)
            Text(
                buildString {
                    if (pet != null) append("${pet.displayName} · Level ${pet.level}") else append("Companion")
                    equippedTitle?.let { append(" · $it") }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("World record", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            "${state.collectionEntries.size} discoveries · $activeQuestCount active/ready quests",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text("${state.hearth?.memories ?: 0} memories", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        items(MenuDestination.entries) { item ->
            Surface(
                onClick = { onOpen(item) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(item.icon, contentDescription = null, modifier = Modifier.padding(11.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        state.operationError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
    }
}
