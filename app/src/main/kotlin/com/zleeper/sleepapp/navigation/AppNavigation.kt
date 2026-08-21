package com.zleeper.sleepapp.navigation

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.feature.shell.ZleeperViewModel
import com.zleeper.sleepapp.game.scene.PlatformScene
import java.text.DateFormat
import java.util.Date

@Composable
fun AppNavigation(navigationState: NavigationState = remember { NavigationState() }, viewModel: ZleeperViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    if (!state.settings.onboardingComplete || state.pet == null) { OnboardingFlow(state, viewModel); return }
    if (state.pendingReview != null) { MorningReview(state, viewModel); return }
    if (state.morningResult != null) { MorningReveal(state, viewModel); return }
    var worldSceneId by rememberSaveable { mutableStateOf<String?>(null) }
    state.scenes.firstOrNull { it.id == worldSceneId }?.let { scene -> PlatformScene(scene, if (state.pet?.formId == "form_moonmoth_02") "lanternwing" else "glimmerling", Modifier.fillMaxSize()) { worldSceneId = null }; return }
    Scaffold(bottomBar = { TopLevelNavigationBar(navigationState) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).testTag("current-destination")) {
            when (navigationState.currentRoute) {
                AppRoute.WORLD -> WorldScreen(state) { worldSceneId = it }
                AppRoute.SLEEP -> SleepScreen(state, viewModel)
                AppRoute.JOURNAL -> JournalScreen(state)
                AppRoute.MENU -> MenuScreen(state, viewModel)
            }
        }
    }
}

@Composable
fun TopLevelNavigationBar(navigationState: NavigationState) {
    NavigationBar {
        AppRoute.entries.forEach { route ->
            NavigationBarItem(
                selected = navigationState.currentRoute == route,
                onClick = { navigationState.select(route) },
                icon = { Icon(route.icon(), route.label) },
                modifier = Modifier.testTag("nav-${route.name.lowercase()}"),
                label = { Text(route.label) },
            )
        }
    }
}

private fun AppRoute.icon(): ImageVector = when (this) { AppRoute.WORLD -> Icons.Outlined.Public; AppRoute.SLEEP -> Icons.Outlined.Bedtime; AppRoute.JOURNAL -> Icons.Outlined.MenuBook; AppRoute.MENU -> Icons.Outlined.Menu }

@Composable private fun OnboardingFlow(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var petName by rememberSaveable { mutableStateOf("Lumi") }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { step = 4 }
    FullPage(listOf("Welcome to Zleeper", "Meet your Moonmoth", "Choose a gentle rhythm", "Phone-based estimation", "Ready for the grove")[step]) {
        when (step) {
            0 -> Text("Rest opens the trail. Each finalized night sends one persistent companion on a deterministic storybook expedition.")
            1 -> { Text("Your companion grows through participation, exploration, and forgiving routines—not medical scores."); OutlinedTextField(petName, { petName = it }, label = { Text("Pet name") }, singleLine = true) }
            2 -> { Text("The starting plan is 10:30 PM to 7:00 AM. You can change it at any time in Settings."); Text("Target duration: 8 hours", style = MaterialTheme.typography.titleMedium) }
            3 -> Text("Zleeper can use Activity Recognition and Google Sleep API events to improve an estimate. Manual start and wake always work. No sensor stream or medical claim is used.")
            else -> Text("The Whispering Grove, first quests, and all four equipment slots will be created locally on this device.")
        }
        state.operationError?.let { ErrorText(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) OutlinedButton({ step-- }) { Text("Back") }
            Button({
                when (step) {
                    2 -> { viewModel.saveSleepPlan(22 * 60 + 30, 7 * 60, 8 * 60); step++ }
                    3 -> { val requested = buildList { if (Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION); if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }; if (requested.isEmpty()) step = 4 else permissions.launch(requested.toTypedArray()) }
                    4 -> viewModel.createPet(petName)
                    else -> step++
                }
            }) { Text(if (step == 4) "Enter the grove" else "Continue") }
        }
    }
}

@Composable private fun WorldScreen(state: ZleeperUiState, onOpenScene: (String) -> Unit) = Page("World Hub") {
    HeroCard("${state.pet?.displayName}'s Hearth", "Level ${state.pet?.level} Moonmoth • Energy ${state.pet?.energy} • Focus ${state.pet?.focus} • Resilience ${state.pet?.resilience}")
    Text("Regions", style = MaterialTheme.typography.headlineSmall)
    state.regions.forEach { region -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(region.name, fontWeight = FontWeight.Bold); Text(region.description); Button({ onOpenScene(region.scenes.first()) }, enabled = (state.pet?.level ?: 1) >= region.unlockLevel) { Text(if ((state.pet?.level ?: 1) >= region.unlockLevel) "Explore" else "Unlocks at level ${region.unlockLevel}") } } } }
    Text("Hub growth", style = MaterialTheme.typography.headlineSmall)
    Text("The hearth shelter, dew garden, relic shelf, and visiting NPC spaces visibly expand as discoveries and collections accumulate.")
}

@Composable private fun SleepScreen(state: ZleeperUiState, viewModel: ZleeperViewModel) = Page("Sleep") {
    val active = state.activeSession
    if (active == null) {
        HeroCard("Tonight's plan", "Target 10:30 PM • Wake 7:00 AM • 8 hours")
        Text("Wind-down", style = MaterialTheme.typography.headlineSmall)
        Text("Dim the room, place the phone safely, and take a quiet minute. This optional ritual earns Focus affinity.")
        Button({ viewModel.beginSleep(true) }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Bedtime, null); Text(" Begin sleep") }
        OutlinedButton({ viewModel.beginSleep(false) }, Modifier.fillMaxWidth()) { Text("Begin without wind-down") }
    } else {
        HeroCard("Sleep mode", "Started ${formatDate(active.sessionStartEpochMs)}. Zleeper is estimating; manual wake remains available.")
        Text("The overnight expedition is deterministic from the seed already stored on this session.")
        Button(viewModel::wake, Modifier.fillMaxWidth()) { Text("I'm awake") }
    }
    state.operationError?.let { ErrorText(it) }
}

@Composable private fun MorningReview(state: ZleeperUiState, viewModel: ZleeperViewModel) = FullPage("Good morning") {
    val session = requireNotNull(state.pendingReview)
    HeroCard("Estimated sleep", "${session.estimatedSleepMinutes ?: 0} minutes • ${session.confidence?.lowercase()?.replaceFirstChar { it.uppercase() }} confidence")
    Text("This is a phone-based estimate, not medically verified sleep. Confirm it to resolve the recorded expedition exactly once.")
    Button(viewModel::finalizeMorning, Modifier.fillMaxWidth()) { Text("Confirm estimate and reveal journey") }
}

@Composable private fun MorningReveal(state: ZleeperUiState, viewModel: ZleeperViewModel) = FullPage("The trail at dawn") {
    val result = requireNotNull(state.morningResult)
    HeroCard("Reach band ${result.reachBand}", "${result.xp} XP • ${result.pathNodeIds.size} journey moments • ${result.rewards.sumOf { it.second }} items")
    Text("Journey replay", style = MaterialTheme.typography.headlineSmall)
    result.pathNodeIds.forEachIndexed { index, node -> Text("${index + 1}. ${node.removePrefix("node_").replace('_', ' ')}") }
    if (result.rewards.isNotEmpty()) { Text("Rewards", style = MaterialTheme.typography.headlineSmall); result.rewards.forEach { Text("${it.second} × ${it.first.removePrefix("item_").replace('_', ' ')}") } }
    Button(viewModel::dismissMorningReveal, Modifier.fillMaxWidth()) { Text("Return to the hearth") }
}

@Composable private fun JournalScreen(state: ZleeperUiState) = Page("Journal") {
    val resolved = state.sessions.filter { it.state == "FINALIZED" || it.state == "EXPEDITION_RESOLVED" }
    if (resolved.isEmpty()) Text("Your finalized nights will appear here with estimated timing, confidence, expedition reach, and morning notes.")
    resolved.forEach { session -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(formatDate(session.sessionStartEpochMs), fontWeight = FontWeight.Bold); Text("Estimated ${session.estimatedSleepMinutes} minutes • ${session.confidence} confidence"); Text("Method: ${session.resolutionMethod?.replace('_', ' ')}") } } }
    Text("Trends", style = MaterialTheme.typography.headlineSmall); Text("Schedule and duration patterns are shown without an authoritative biological sleep score.")
}

private enum class MenuPage(val title: String, val icon: ImageVector) { PET("Pet Profile", Icons.Outlined.Pets), EQUIPMENT("Equipment", Icons.Outlined.Backpack), INVENTORY("Inventory", Icons.Outlined.Inventory2), QUESTS("Quest Log", Icons.Outlined.MenuBook), COLLECTION("Collections", Icons.Outlined.CollectionsBookmark), SETTINGS("Settings", Icons.Outlined.Settings), PERMISSIONS("Permissions", Icons.Outlined.Settings), DATA("Data Management", Icons.Outlined.Edit) }

@Composable private fun MenuScreen(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf<MenuPage?>(null) }
    if (selected == null) Page("Menu") { MenuPage.entries.forEach { page -> OutlinedButton({ selected = page }, Modifier.fillMaxWidth()) { Icon(page.icon, null); Text("  ${page.title}") } } }
    else Page(selected!!.title) {
        OutlinedButton({ selected = null }) { Text("Back to menu") }
        when (selected) {
            MenuPage.PET -> { var name by rememberSaveable { mutableStateOf(state.pet?.displayName.orEmpty()) }; HeroCard(state.pet?.displayName.orEmpty(), "Form ${state.pet?.formId} • Level ${state.pet?.level} • ${state.pet?.totalXp} XP"); OutlinedTextField(name, { name = it }, label = { Text("Display name") }); Button({ viewModel.renamePet(name) }) { Text("Save name") } }
            MenuPage.EQUIPMENT -> listOf("HEAD", "CHARM", "PACK", "RELIC").forEach { HeroCard(it, "No item equipped") }
            MenuPage.INVENTORY -> state.items.forEach { Text("${raritySymbol(it.rarity)} ${it.name} — ${it.category.lowercase()}") }
            MenuPage.QUESTS -> state.quests.forEach { HeroCard(it.name, "${it.family.lowercase().replaceFirstChar { value -> value.uppercase() }} • ${it.description}") }
            MenuPage.COLLECTION -> Text("Discoveries, relics, creatures, and region keepsakes are recorded here when earned.")
            MenuPage.SETTINGS -> { SettingToggle("Wake alarm", state.settings.alarmEnabled, viewModel::setAlarmEnabled); SettingToggle("Wind-down reminder", state.settings.windDownReminderEnabled, viewModel::setWindDownReminderEnabled); SettingToggle("Large game controls", state.settings.largeControls, viewModel::setLargeControls); Text("Theme: ${state.settings.theme} • Motion: ${state.settings.motion} • Volume ${(state.settings.soundVolume * 100).toInt()}%") }
            MenuPage.PERMISSIONS -> Text("Activity Recognition improves estimation. Notifications provide wake and wind-down alerts. Both are optional; manual sleep remains playable.")
            MenuPage.DATA -> {
                var confirmDelete by rememberSaveable { mutableStateOf(false) }
                Text("All history and game progress is stored locally. Export creates a user-readable JSON archive.")
                Button({ viewModel.exportData { uri -> context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Export Zleeper data")) } }, Modifier.fillMaxWidth()) { Text("Export local data") }
                if (!confirmDelete) OutlinedButton({ confirmDelete = true }, Modifier.fillMaxWidth()) { Text("Delete all local data") }
                else { ErrorText("This permanently removes sleep history and all game progress from this device."); Button({ viewModel.deleteAllData() }, Modifier.fillMaxWidth()) { Text("Confirm permanent deletion") }; OutlinedButton({ confirmDelete = false }, Modifier.fillMaxWidth()) { Text("Cancel") } }
            }
            null -> Unit
        }
    }
}

@Composable private fun SettingToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Switch(checked, onChange) }
@Composable private fun Page(title: String, content: @Composable ColumnScope.() -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }; item { Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content) } } }
@Composable private fun FullPage(title: String, content: @Composable ColumnScope.() -> Unit) { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) { Column(Modifier.padding(28.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) { Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); content() } } }
@Composable private fun HeroCard(title: String, body: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(body) } } }
@Composable private fun ErrorText(value: String) = Text(value, color = MaterialTheme.colorScheme.error)
private fun formatDate(epoch: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epoch))
private fun raritySymbol(rarity: String): String = when (rarity) { "COMMON" -> "●"; "UNCOMMON" -> "◆"; "RARE" -> "✦"; "EPIC" -> "✧"; else -> "★" }
