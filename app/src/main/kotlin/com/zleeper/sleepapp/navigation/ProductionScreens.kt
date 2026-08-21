package com.zleeper.sleepapp.navigation

import android.Manifest
import android.app.AlarmManager
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zleeper.sleepapp.data.content.RegionDefinition
import com.zleeper.sleepapp.data.local.preferences.ThemePreference
import com.zleeper.sleepapp.data.local.preferences.MotionPreference
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.feature.shell.ZleeperViewModel
import com.zleeper.sleepapp.ui.components.GlassPanel
import com.zleeper.sleepapp.ui.components.PetPose
import com.zleeper.sleepapp.ui.components.PetSprite
import com.zleeper.sleepapp.ui.components.ProgressTrack
import com.zleeper.sleepapp.ui.components.SectionHeader
import com.zleeper.sleepapp.ui.components.StatPill
import com.zleeper.sleepapp.ui.components.StorybookBackdrop
import com.zleeper.sleepapp.ui.components.rememberAssetImage
import com.zleeper.sleepapp.ui.theme.ZleeperArtColors
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun OnboardingFlow(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var petName by rememberSaveable { mutableStateOf("Lumi") }
    var bedtime by rememberSaveable { mutableIntStateOf(state.settings.targetSleepMinutes) }
    var wakeTime by rememberSaveable { mutableIntStateOf(state.settings.targetWakeMinutes) }
    var duration by rememberSaveable { mutableIntStateOf(state.settings.targetDurationMinutes) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { step = 4 }
    val titles = listOf("Rest opens the trail", "Meet your Moonmoth", "Choose your rhythm", "Private by design", "The grove is listening")
    val subtitles = listOf(
        "A gentle sleep ritual becomes a small journey by morning.",
        "One persistent companion grows through care, curiosity, and showing up.",
        "Start with a calm schedule. You can reshape it whenever life changes.",
        "Phone-based estimates stay on your device. Manual sleep always works.",
        "Your first trail, quests, and keepsakes are ready.",
    )
    StorybookBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(5) { index ->
                    Box(
                        Modifier.weight(1f).height(4.dp).background(
                            if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                    )
                }
            } }
            item { Box(Modifier.fillMaxWidth().height(if (step == 2) 160.dp else 250.dp), contentAlignment = Alignment.Center) {
                PetSprite("glimmerling", if (step == 4) PetPose.CELEBRATE else PetPose.IDLE, rememberPetFrame(step != 0 && state.settings.motion == MotionPreference.FULL, step), Modifier.size(if (step == 2) 160.dp else 250.dp))
            } }
            item { Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("STEP ${step + 1} OF 5", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(titles[step], style = MaterialTheme.typography.displaySmall)
                Text(subtitles[step], color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                if (step == 1) {
                    OutlinedTextField(
                        value = petName,
                        onValueChange = { petName = it },
                        label = { Text("Companion name") },
                        supportingText = { Text("You can change this later") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (step == 2) {
                    GlassPanel(Modifier.fillMaxWidth()) {
                        PlanSlider("Bedtime", formatMinutes(bedtime), bedtime.toFloat(), 0f..1425f) { bedtime = ((it / 15f).roundToInt() * 15).coerceIn(0, 1439) }
                        PlanSlider("Wake", formatMinutes(wakeTime), wakeTime.toFloat(), 0f..1425f) { wakeTime = ((it / 15f).roundToInt() * 15).coerceIn(0, 1439) }
                        PlanSlider("Target duration", "${duration / 60}h ${duration % 60}m", duration.toFloat(), 180f..900f) { duration = ((it / 15f).roundToInt() * 15).coerceIn(180, 900) }
                    }
                }
                state.operationError?.let { ErrorText(it) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (step > 0) OutlinedButton({ step-- }, Modifier.weight(.38f).height(54.dp)) { Text("Back") }
                    Button(
                        onClick = {
                            when (step) {
                                2 -> { viewModel.saveSleepPlan(bedtime, wakeTime, duration); step++ }
                                3 -> {
                                    val requested = buildList {
                                        if (Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
                                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.markPermissionExplanations()
                                    if (requested.isEmpty()) step = 4 else permissions.launch(requested.toTypedArray())
                                }
                                4 -> viewModel.createPet(petName)
                                else -> step++
                            }
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                    ) { Text(if (step == 4) "Enter the grove" else "Continue") }
                }
            } }
        }
    }
}

@Composable
internal fun WorldScreen(state: ZleeperUiState, onOpenScene: (String) -> Unit) {
    val pet = state.pet ?: return
    val form = if (pet.formId == "form_moonmoth_02") "lanternwing" else "glimmerling"
    val hearth = rememberAssetImage("game/atlas/hub/hub_moonmoth_hearth.webp")
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(360.dp)) {
                Canvas(Modifier.fillMaxSize()) { hearth?.let { drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt())) } }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ZleeperArtColors.NightInk.copy(alpha = .69f), Color.Transparent, ZleeperArtColors.NightInk.copy(alpha = .78f)))))
                Column(Modifier.fillMaxSize().padding(22.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("THE MOONMOTH HEARTH", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            Text(pet.displayName, style = MaterialTheme.typography.headlineLarge)
                            Text("Level ${pet.level} · ${if (form == "lanternwing") "Lanternwing" else "Glimmerling"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .68f), shape = CircleShape) {
                            Text("${pet.totalXp} XP", Modifier.padding(horizontal = 13.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                        PetSprite(form, PetPose.IDLE, rememberPetFrame(state.settings.motion == MotionPreference.FULL), Modifier.size(205.dp))
                        val shelfItems = state.collectionEntries.mapNotNull { entry -> state.items.firstOrNull { it.id == entry.entryId } }.takeLast(3)
                        if (shelfItems.isNotEmpty()) {
                            Column(Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.End) {
                                Text("RELIC SHELF", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    shelfItems.forEach { ItemIcon(it.id, Modifier.size(38.dp)) }
                                }
                            }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill("Energy", pet.energy.toString(), ZleeperArtColors.Lantern)
                        StatPill("Focus", pet.focus.toString(), ZleeperArtColors.Sky)
                        StatPill("Grit", pet.resilience.toString(), ZleeperArtColors.Rose)
                    }
                }
            }
        }
        item { SectionHeader("The waking world", "Choose a trail", Modifier.padding(horizontal = 20.dp)) }
        items(state.regions, key = { it.id }) { region ->
            RegionCard(region, unlocked = pet.level >= region.unlockLevel, onOpen = { onOpenScene(region.scenes.first()) })
        }
        item {
            GlassPanel(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Text("THE HEARTH REMEMBERS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(7.dp))
                Text("A home shaped by every night", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Your shelter, dew garden, relic shelf, and visiting paths grow as discoveries return from the trail.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                ProgressTrack(((pet.level - 1) / 9f).coerceIn(0f, 1f), MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(8.dp))
                Text("Hearth growth · level ${pet.level} of 10", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun RegionCard(region: RegionDefinition, unlocked: Boolean, onOpen: () -> Unit) {
    val regionKey = region.id.removePrefix("region_")
    val image = rememberAssetImage("game/atlas/region/region_${regionKey}_bg_far.webp")
    Surface(
        onClick = onOpen,
        enabled = unlocked,
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box {
            Canvas(Modifier.fillMaxSize()) { image?.let { drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt())) } }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, ZleeperArtColors.NightInk.copy(alpha = .91f)), startY = 35f)))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp).fillMaxWidth(.78f)) {
                Text(if (unlocked) "OPEN TRAIL" else "LEVEL ${region.unlockLevel}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(region.name, style = MaterialTheme.typography.titleLarge)
                Text(region.description, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(Modifier.align(Alignment.BottomEnd).padding(18.dp), shape = CircleShape, color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant) {
                Icon(Icons.Outlined.ChevronRight, if (unlocked) "Explore ${region.name}" else "Locked", Modifier.padding(11.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
internal fun SleepScreen(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    val active = state.activeSession
    var editingPlan by rememberSaveable { mutableStateOf(false) }
    var bedtime by rememberSaveable(state.settings.targetSleepMinutes) { mutableIntStateOf(state.settings.targetSleepMinutes) }
    var wakeTime by rememberSaveable(state.settings.targetWakeMinutes) { mutableIntStateOf(state.settings.targetWakeMinutes) }
    var duration by rememberSaveable(state.settings.targetDurationMinutes) { mutableIntStateOf(state.settings.targetDurationMinutes) }
    StorybookBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { SectionHeader(if (active == null) "Tonight" else "Sleep mode", if (active == null) "Make room for rest" else "The trail is unfolding") }
            item {
                Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    SleepDial(active != null)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.DarkMode, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(if (active == null) formatMinutes(state.settings.targetSleepMinutes) else "Resting", style = MaterialTheme.typography.headlineLarge)
                        Text(if (active == null) "wake at ${formatMinutes(state.settings.targetWakeMinutes)}" else "since ${formatDate(active.sessionStartEpochMs)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (active == null) {
                item { ScheduleSummary(state.settings.targetSleepMinutes, state.settings.targetWakeMinutes, state.settings.targetDurationMinutes) }
                item { OutlinedButton({ editingPlan = !editingPlan }, Modifier.fillMaxWidth()) { Text(if (editingPlan) "Close schedule editor" else "Adjust tonight’s plan") } }
                if (editingPlan) item {
                    GlassPanel(Modifier.fillMaxWidth()) {
                        PlanSlider("Bedtime", formatMinutes(bedtime), bedtime.toFloat(), 0f..1425f) { bedtime = ((it / 15f).roundToInt() * 15).coerceIn(0, 1439) }
                        PlanSlider("Wake", formatMinutes(wakeTime), wakeTime.toFloat(), 0f..1425f) { wakeTime = ((it / 15f).roundToInt() * 15).coerceIn(0, 1439) }
                        PlanSlider("Target duration", "${duration / 60}h ${duration % 60}m", duration.toFloat(), 180f..900f) { duration = ((it / 15f).roundToInt() * 15).coerceIn(180, 900) }
                        Button({ viewModel.saveSleepPlan(bedtime, wakeTime, duration); editingPlan = false }, Modifier.fillMaxWidth()) { Text("Save sleep plan") }
                    }
                }
                item {
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) { Icon(Icons.Outlined.WbTwilight, null, Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.secondary) }
                            Spacer(Modifier.width(14.dp))
                            Column { Text("One-minute wind-down", style = MaterialTheme.typography.titleLarge); Text("Dim the room, settle the phone, and take one quiet breath.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                item { Button({ viewModel.beginSleep(true) }, Modifier.fillMaxWidth().height(58.dp)) { Icon(Icons.Outlined.Bedtime, null); Spacer(Modifier.width(9.dp)); Text("Begin sleep") } }
                item { OutlinedButton({ viewModel.beginSleep(false) }, Modifier.fillMaxWidth().height(52.dp)) { Text("Skip wind-down tonight") } }
            } else {
                item { Text("Your phone is using the available sleep signal to form an estimate. The journey seed is already sealed and cannot be rerolled.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { Button(viewModel::wake, Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("I’m awake") } }
            }
            item { state.operationError?.let { ErrorText(it) } }
        }
    }
}

@Composable
private fun SleepDial(active: Boolean) {
    Canvas(Modifier.size(244.dp).semantics { contentDescription = if (active) "Sleep session active" else "Tonight's sleep schedule" }) {
        drawCircle(ZleeperArtColors.Sky.copy(alpha = .13f), radius = size.minDimension * .48f)
        drawArc(ZleeperArtColors.Outline, -90f, 360f, false, style = Stroke(12f))
        drawArc(if (active) ZleeperArtColors.Lantern else ZleeperArtColors.Fern, -90f, if (active) 255f else 208f, false, style = Stroke(12f))
        drawCircle(ZleeperArtColors.Lantern, radius = 5.5f, center = center + androidx.compose.ui.geometry.Offset(0f, -size.minDimension * .48f))
    }
}

@Composable
private fun ScheduleSummary(sleep: Int, wake: Int, duration: Int) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TimeMetric("Bedtime", formatMinutes(sleep))
            TimeMetric("Wake", formatMinutes(wake))
            TimeMetric("Target", "${duration / 60}h ${duration % 60}m")
        }
    }
}

@Composable private fun TimeMetric(label: String, value: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge); Text(value, style = MaterialTheme.typography.titleLarge) }
@Composable private fun PlanSlider(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontWeight = FontWeight.SemiBold); Text(valueLabel, color = MaterialTheme.colorScheme.primary) }; Slider(value, onChange, valueRange = range); Spacer(Modifier.height(6.dp)) }

@Composable
internal fun MorningReview(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    val session = requireNotNull(state.pendingReview)
    val sessionEnd = requireNotNull(session.sessionEndEpochMs)
    val sessionMinutes = ((sessionEnd - session.sessionStartEpochMs) / 60_000L).toInt().coerceAtLeast(0)
    val estimatedStartOffset = (((session.estimatedSleepStartEpochMs ?: session.sessionStartEpochMs) - session.sessionStartEpochMs) / 60_000L).toInt().coerceIn(0, (sessionMinutes - 1).coerceAtLeast(0))
    val estimatedEndOffset = (((session.estimatedSleepEndEpochMs ?: sessionEnd) - session.sessionStartEpochMs) / 60_000L).toInt().coerceIn((estimatedStartOffset + 1).coerceAtMost(sessionMinutes), sessionMinutes)
    var correcting by rememberSaveable(session.id) { mutableStateOf(false) }
    var startOffset by rememberSaveable(session.id) { mutableIntStateOf(estimatedStartOffset) }
    var endOffset by rememberSaveable(session.id) { mutableIntStateOf(estimatedEndOffset) }
    val savedNote = state.morningNotes.firstOrNull { it.sleepSessionId == session.id }
    var mood by rememberSaveable(session.id) { mutableStateOf(savedNote?.mood) }
    var note by rememberSaveable(session.id) { mutableStateOf(savedNote?.note.orEmpty()) }
    StorybookBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(26.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            item { SectionHeader("Good morning", "How did the night feel?") }
            item { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("${session.estimatedSleepMinutes ?: 0}", style = MaterialTheme.typography.displaySmall)
                Text("estimated minutes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) { Text("${session.confidence?.lowercase()?.replaceFirstChar { it.uppercase() }} confidence", Modifier.padding(horizontal = 16.dp, vertical = 9.dp)) }
            } }
            item { Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("This is a phone-based estimate, not medically verified sleep. Confirming resolves the stored expedition exactly once.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (correcting && sessionMinutes >= 2) {
                    GlassPanel(Modifier.fillMaxWidth()) {
                        PlanSlider("Fell asleep", "${startOffset}m after starting", startOffset.toFloat(), 0f..(sessionMinutes - 1).toFloat()) {
                            startOffset = it.roundToInt().coerceIn(0, endOffset - 1)
                        }
                        PlanSlider("Woke up", "${sessionMinutes - endOffset}m before ending", endOffset.toFloat(), 1f..sessionMinutes.toFloat()) {
                            endOffset = it.roundToInt().coerceIn(startOffset + 1, sessionMinutes)
                        }
                    }
                }
                if (sessionMinutes >= 2) OutlinedButton({ correcting = !correcting }, Modifier.fillMaxWidth()) { Text(if (correcting) "Use original estimate" else "Correct the estimate") }
                GlassPanel(Modifier.fillMaxWidth()) {
                    Text("Morning reflection", style = MaterialTheme.typography.titleLarge)
                    Text("Optional · adds a small reflection bonus", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Rough", "Low", "Okay", "Good", "Rested").forEachIndexed { index, label ->
                            val value = index + 1
                            if (mood == value) Button({ mood = null; note = "" }) { Text(label) }
                            else FilledTonalButton({ mood = value }) { Text(label) }
                        }
                    }
                    if (mood != null) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(500) },
                            label = { Text("A note for later (optional)") },
                            supportingText = { Text("${note.length}/500") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                        )
                    }
                }
                Button(
                    onClick = {
                        if (correcting) viewModel.finalizeMorning(session.sessionStartEpochMs + startOffset * 60_000L, session.sessionStartEpochMs + endOffset * 60_000L, mood, note)
                        else viewModel.finalizeMorning(mood = mood, note = note)
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                ) { Text(if (correcting) "Save correction and reveal" else "Confirm and reveal journey") }
            } }
        }
    }
}

@Composable
internal fun MorningResolutionPending(sessionId: String, error: String?, onRetry: () -> Unit) {
    LaunchedEffect(sessionId) { onRetry() }
    StorybookBackdrop(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (error == null) {
                CircularProgressIndicator()
                Spacer(Modifier.height(18.dp))
                Text("Reading the night trail", style = MaterialTheme.typography.headlineMedium)
                Text("Your saved session is safe while rewards resolve.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                ErrorText(error)
                Spacer(Modifier.height(14.dp))
                Button(onRetry) { Text("Try resolving again") }
            }
        }
    }
}

@Composable
internal fun MorningReveal(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    val result = requireNotNull(state.morningResult)
    StorybookBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { SectionHeader("At first light", "The trail brought something home") }
            item { Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { PetSprite(if (state.pet?.formId == "form_moonmoth_02") "lanternwing" else "glimmerling", PetPose.CELEBRATE, rememberPetFrame(state.settings.motion == MotionPreference.FULL), Modifier.size(220.dp)) } }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatPill("Reach", result.reachBand.toString(), MaterialTheme.colorScheme.tertiary); StatPill("XP", result.xp.toString(), MaterialTheme.colorScheme.primary); StatPill("Finds", result.rewards.sumOf { it.second }.toString(), MaterialTheme.colorScheme.secondary) } }
            item { Text("Journey", style = MaterialTheme.typography.titleLarge) }
            items(result.pathNodeIds) { node -> TimelineRow(node.removePrefix("node_").replace('_', ' ').replaceFirstChar { it.uppercase() }) }
            if (result.rewards.isNotEmpty()) {
                item { Text("Returned with", style = MaterialTheme.typography.titleLarge) }
                items(result.rewards) { reward -> GlassPanel(Modifier.fillMaxWidth(), PaddingValues(14.dp)) { Text("${reward.second} × ${humanize(reward.first)}", fontWeight = FontWeight.SemiBold) } }
            }
            item { Button(viewModel::dismissMorningReveal, Modifier.fillMaxWidth().height(56.dp)) { Text("Return to the hearth") } }
        }
    }
}

@Composable
internal fun JournalScreen(state: ZleeperUiState) {
    val resolved = state.sessions.filter { it.state == "FINALIZED" || it.state == "EXPEDITION_RESOLVED" }
    var expandedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SectionHeader("Sleep journal", "Your nights, without judgment") }
        item {
            GlassPanel(Modifier.fillMaxWidth()) {
                Text("A gentle record", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(7.dp))
                Text("Timing, duration, and confidence appear as patterns—not scores. A difficult night never punishes your companion.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (resolved.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(46.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(14.dp)); Text("Your first page is waiting", style = MaterialTheme.typography.titleLarge)
                    Text("A finalized night will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else items(resolved, key = { it.id }) { session ->
            val morningNote = state.morningNotes.firstOrNull { it.sleepSessionId == session.id }
            Surface(onClick = { expandedSessionId = if (expandedSessionId == session.id) null else session.id }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatDate(session.sessionStartEpochMs), fontWeight = FontWeight.Bold); Text("${session.estimatedSleepMinutes ?: 0} min", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(10.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${session.confidence?.lowercase()?.replaceFirstChar { it.uppercase() }} confidence", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(humanize(session.resolutionMethod ?: "manual"), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    AnimatedVisibility(expandedSessionId == session.id) {
                        Column {
                            Spacer(Modifier.height(12.dp)); Text("Estimated window", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text("${session.estimatedSleepStartEpochMs?.let(::formatDate) ?: "Not available"} — ${session.estimatedSleepEndEpochMs?.let(::formatDate) ?: "Not available"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp)); Text(if (session.windDownCompleted) "Wind-down completed" else "No wind-down recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            morningNote?.let { reflection ->
                                Spacer(Modifier.height(10.dp)); Text("Morning reflection · ${listOf("Rough", "Low", "Okay", "Good", "Rested")[reflection.mood - 1]}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                if (reflection.note.isNotBlank()) Text(reflection.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeader("Trends", "Rhythms over time") }
        if (resolved.size < 3) item { Text("${3 - resolved.size} more finalized ${if (3 - resolved.size == 1) "night" else "nights"} will unlock a stable first pattern. We wait so one estimate never becomes a conclusion.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        else {
            val recent = resolved.take(7)
            val durations = recent.mapNotNull { it.estimatedSleepMinutes }
            val average = if (durations.isEmpty()) 0 else durations.average().toInt()
            val spread = if (durations.isEmpty()) 0 else (durations.maxOrNull() ?: 0) - (durations.minOrNull() ?: 0)
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatPill("Average", "${average / 60}h ${average % 60}m", MaterialTheme.colorScheme.tertiary); StatPill("Range", "${spread}m", MaterialTheme.colorScheme.secondary); StatPill("Nights", recent.size.toString(), MaterialTheme.colorScheme.primary) } }
        }
    }
}

private enum class MenuPage(val title: String, val subtitle: String, val icon: ImageVector) {
    PET("Pet profile", "Name, growth, and affinity", Icons.Outlined.Pets),
    EQUIPMENT("Equipment", "Four meaningful slots", Icons.Outlined.Backpack),
    INVENTORY("Inventory", "Keepsakes from the trail", Icons.Outlined.Inventory2),
    QUESTS("Quest log", "Promises and discoveries", Icons.AutoMirrored.Outlined.MenuBook),
    COLLECTION("Collections", "Creatures, relics, and places", Icons.Outlined.CollectionsBookmark),
    SETTINGS("Settings", "Sleep, sound, and controls", Icons.Outlined.Settings),
    PERMISSIONS("Permissions", "Optional platform access", Icons.Outlined.Security),
    DATA("Your data", "Export or remove local records", Icons.Outlined.Download),
}
private enum class DataAction { SLEEP_HISTORY, GAME_PROGRESS, ALL_DATA }

@Composable
internal fun MenuScreen(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    val context = LocalContext.current
    var permissionRefresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val activityPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionRefresh++ }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionRefresh++ }
    var selected by rememberSaveable { mutableStateOf<MenuPage?>(null) }
    val ownedQuantities = remember(state.inventoryStacks, state.inventoryInstances) {
        buildMap {
            state.inventoryStacks.forEach { put(it.itemId, it.quantity) }
            state.inventoryInstances.groupingBy { it.itemId }.eachCount().forEach { (itemId, count) -> put(itemId, (get(itemId) ?: 0) + count) }
        }
    }
    val ownedItems = state.items.filter { (ownedQuantities[it.id] ?: 0) > 0 }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeader(if (selected == null) "Zleeper" else "Menu", selected?.title ?: "Everything in its place") }
        state.operationError?.let { message -> item { Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) { Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer) } } }
        if (selected == null) {
            items(MenuPage.entries) { page ->
                Surface(onClick = { selected = page }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) { Icon(page.icon, null, Modifier.padding(11.dp), tint = MaterialTheme.colorScheme.secondary) }
                        Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(page.title, fontWeight = FontWeight.SemiBold); Text(page.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item { OutlinedButton({ selected = null }) { Text("Back") } }
            when (requireNotNull(selected)) {
                MenuPage.PET -> item { PetProfile(state, viewModel) }
                MenuPage.EQUIPMENT -> items(listOf("HEAD", "CHARM", "PACK", "RELIC")) { slot -> EquipmentSlotCard(slot, state, ownedItems, viewModel) }
                MenuPage.INVENTORY -> {
                    if (ownedItems.isEmpty()) item { GlassPanel(Modifier.fillMaxWidth()) { Text("The satchel is light", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(7.dp)); Text("Things found on expeditions and waking trails will be kept here.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    else items(ownedItems, key = { it.id }) { item -> InventoryRow(item.id, item.name, item.description, item.rarity, ownedQuantities[item.id] ?: 1) }
                }
                MenuPage.QUESTS -> items(state.quests, key = { it.id }) { quest ->
                    val status = state.questProgress.firstOrNull { it.questId == quest.id }?.status ?: "LOCKED"
                    GlassPanel(Modifier.fillMaxWidth()) { Text(quest.name, style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(5.dp)); Text(quest.description, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(quest.family.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge); Text(status.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }, color = if (status == "COMPLETED") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) } }
                }
                MenuPage.COLLECTION -> {
                    if (state.collectionEntries.isEmpty()) item { GlassPanel(Modifier.fillMaxWidth()) { Text("A living field guide", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(7.dp)); Text("Your first discovery will begin the collection.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    else items(state.collectionEntries, key = { it.entryId }) { entry ->
                        val definition = state.items.firstOrNull { it.id == entry.entryId }
                        GlassPanel(Modifier.fillMaxWidth(), PaddingValues(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { if (definition != null) ItemIcon(definition.id, Modifier.size(58.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(definition?.name ?: humanize(entry.entryId), fontWeight = FontWeight.SemiBold); Text(entry.category.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("×${entry.quantity}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }
                    }
                }
                MenuPage.SETTINGS -> item { SettingsPanel(state, viewModel) }
                MenuPage.PERMISSIONS -> item {
                    PermissionsPanel(
                        refreshKey = permissionRefresh,
                        onRequestActivity = { viewModel.markPermissionExplanations(); activityPermission.launch(ACTIVITY_RECOGNITION_PERMISSION) },
                        onRequestNotifications = { viewModel.markPermissionExplanations(); notificationPermission.launch(NOTIFICATION_PERMISSION) },
                        onRequestExactAlarm = { if (Build.VERSION.SDK_INT >= 31) context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())) },
                        onOpenSettings = { context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())) },
                    )
                }
                MenuPage.DATA -> item {
                    var pendingAction by rememberSaveable { mutableStateOf<DataAction?>(null) }
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Text("Stored on this device", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(7.dp)); Text("Export creates a readable JSON archive of sleep history and game progress.", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(16.dp))
                        Button({ viewModel.exportData { uri -> context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Export Zleeper data")) } }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Download, null); Spacer(Modifier.width(8.dp)); Text("Export local data") }
                        Spacer(Modifier.height(8.dp)); OutlinedButton({ pendingAction = DataAction.SLEEP_HISTORY }, Modifier.fillMaxWidth()) { Text("Delete sleep history") }
                        Spacer(Modifier.height(8.dp)); OutlinedButton({ pendingAction = DataAction.GAME_PROGRESS }, Modifier.fillMaxWidth()) { Text("Reset game progress") }
                        Spacer(Modifier.height(8.dp)); OutlinedButton({ pendingAction = DataAction.ALL_DATA }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text("Delete all local data") }
                        AnimatedVisibility(pendingAction != null) {
                            Column {
                                Spacer(Modifier.height(14.dp))
                                ErrorText(when (pendingAction) { DataAction.SLEEP_HISTORY -> "This permanently removes every sleep session and morning record."; DataAction.GAME_PROGRESS -> "This permanently removes your pet, inventory, quests, and discoveries while keeping sleep history."; else -> "This permanently removes sleep history, settings, and all game progress from this device." })
                                Spacer(Modifier.height(10.dp))
                                Button({ when (pendingAction) { DataAction.SLEEP_HISTORY -> viewModel.deleteSleepHistory(); DataAction.GAME_PROGRESS -> viewModel.resetGameProgress(); else -> viewModel.deleteAllData() }; pendingAction = null }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) { Text("Confirm permanent deletion") }
                                OutlinedButton({ pendingAction = null }, Modifier.fillMaxWidth()) { Text("Cancel") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsPanel(
    refreshKey: Int,
    onRequestActivity: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val activityGranted = remember(refreshKey) { Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED }
    val notificationsGranted = remember(refreshKey) { Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED }
    val exactAlarmGranted = remember(refreshKey) { Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() }
    GlassPanel(Modifier.fillMaxWidth()) {
        Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Text("Optional, never a gate", style = MaterialTheme.typography.titleLarge)
        Text("Manual sleep remains fully playable without either permission.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        PermissionRow("Activity Recognition", "Improves phone-based sleep estimates", activityGranted, Build.VERSION.SDK_INT >= 29, onRequestActivity)
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        PermissionRow("Notifications", "Delivers wake and wind-down alerts", notificationsGranted, Build.VERSION.SDK_INT >= 33, onRequestNotifications)
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        PermissionRow("Alarms & reminders", "Allows the wake alarm to ring precisely", exactAlarmGranted, Build.VERSION.SDK_INT >= 31, onRequestExactAlarm)
        if (!activityGranted || !notificationsGranted || !exactAlarmGranted) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onOpenSettings, Modifier.fillMaxWidth()) { Text("Open system settings") }
        }
    }
}

@Composable
private fun PermissionRow(label: String, detail: String, granted: Boolean, requestable: Boolean, onRequest: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Text(if (granted) "Allowed" else "Not allowed", color = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
        }
        if (!granted && requestable) FilledTonalButton(onRequest) { Text("Allow") }
    }
}

@Composable private fun PetProfile(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    val pet = state.pet ?: return
    var name by rememberSaveable { mutableStateOf(pet.displayName) }
    GlassPanel(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { PetSprite(if (pet.formId == "form_moonmoth_02") "lanternwing" else "glimmerling", PetPose.IDLE, rememberPetFrame(state.settings.motion == MotionPreference.FULL), Modifier.size(180.dp)) }
        Text("Level ${pet.level} · ${pet.totalXp} XP", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(12.dp)); OutlinedTextField(name, { name = it }, label = { Text("Display name") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(10.dp)); Button({ viewModel.renamePet(name) }, Modifier.fillMaxWidth()) { Text("Save name") }
    }
}

@Composable private fun EquipmentSlotCard(slot: String, state: ZleeperUiState, ownedItems: List<com.zleeper.sleepapp.data.content.ItemDefinition>, viewModel: ZleeperViewModel) {
    val equippedInstance = state.equipmentSlots.firstOrNull { it.slot == slot }?.inventoryInstanceId
    val equippedItemId = state.inventoryInstances.firstOrNull { it.instanceId == equippedInstance }?.itemId
    val equipped = state.items.firstOrNull { it.id == equippedItemId }
    val candidates = ownedItems.filter { equipmentSlotFor(it.id) == slot }
    GlassPanel(Modifier.fillMaxWidth()) {
        Text(slot.lowercase().replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        if (equipped != null) {
            Row(verticalAlignment = Alignment.CenterVertically) { ItemIcon(equipped.id, Modifier.size(54.dp)); Spacer(Modifier.width(12.dp)); Column { Text(equipped.name, fontWeight = FontWeight.SemiBold); Text("Equipped", color = MaterialTheme.colorScheme.secondary) } }
        } else Text("Nothing equipped", color = MaterialTheme.colorScheme.onSurfaceVariant)
        candidates.filterNot { it.id == equippedItemId }.forEach { item ->
            Spacer(Modifier.height(12.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { ItemIcon(item.id, Modifier.size(46.dp)); Spacer(Modifier.width(10.dp)); Text(item.name, Modifier.weight(1f)); FilledTonalButton({ viewModel.equip(item.id) }) { Text("Equip") } }
        }
    }
}

@Composable private fun InventoryRow(id: String, name: String, description: String, rarity: String, quantity: Int) = GlassPanel(Modifier.fillMaxWidth(), PaddingValues(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ItemIcon(id, Modifier.size(62.dp)); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.SemiBold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        Column(horizontalAlignment = Alignment.End) { Text(raritySymbol(rarity), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge); if (quantity > 1) Text("×$quantity", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable private fun ItemIcon(itemId: String, modifier: Modifier = Modifier) {
    val image = rememberAssetImage("game/item/icon/${itemId}_icon.webp")
    Canvas(modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) { image?.let { drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt())) } }
}

@Composable private fun SettingsPanel(state: ZleeperUiState, viewModel: ZleeperViewModel) = GlassPanel(Modifier.fillMaxWidth()) {
    SettingToggle("Wake alarm", "A gentle scheduled alarm", state.settings.alarmEnabled, viewModel::setAlarmEnabled)
    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
    SettingToggle("Wind-down reminder", "One hour before bedtime", state.settings.windDownReminderEnabled, viewModel::setWindDownReminderEnabled)
    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
    SettingToggle("Large game controls", "Larger movement and action pads", state.settings.largeControls, viewModel::setLargeControls)
    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
    SettingToggle("Left-handed controls", "Places actions on the left in trails", state.settings.leftHandedControls, viewModel::setLeftHandedControls)
    Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Control opacity", fontWeight = FontWeight.SemiBold); Text("${(state.settings.controlOpacity * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Slider(value = state.settings.controlOpacity, onValueChange = viewModel::setControlOpacity, valueRange = .45f..1f)
    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
    SettingToggle("Reduced motion", "Removes navigation transitions", state.settings.motion == MotionPreference.REDUCED, viewModel::setReducedMotion)
    Spacer(Modifier.height(18.dp)); Text("Appearance", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemePreference.entries.forEach { theme ->
            if (state.settings.theme == theme) Button({ viewModel.setTheme(theme) }) { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) }
            else FilledTonalButton({ viewModel.setTheme(theme) }) { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) }
        }
    }
    Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Sound", fontWeight = FontWeight.SemiBold); Text("${(state.settings.soundVolume * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Slider(value = state.settings.soundVolume, onValueChange = viewModel::setVolume, valueRange = 0f..1f)
}

@Composable private fun SettingToggle(label: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.SemiBold); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }; Switch(checked, onChange) }
@Composable private fun TimelineRow(label: String) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.secondary, CircleShape)); Spacer(Modifier.width(12.dp)); Text(label) }
@Composable private fun ErrorText(value: String) = Text(value, color = MaterialTheme.colorScheme.error)

@Composable
private fun rememberPetFrame(animated: Boolean = true, animationKey: Any? = Unit): Int {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(animated, animationKey) {
        frame = 0
        if (!animated) { frame = 0; return@LaunchedEffect }
        for (next in 1..7) { delay(135); frame = next }
        delay(180); frame = 0
    }
    return frame
}

private fun formatDate(epoch: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epoch))
private fun formatMinutes(value: Int): String { val h = value / 60; val m = value % 60; val hour = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }; return "%d:%02d %s".format(hour, m, if (h < 12) "AM" else "PM") }
private fun humanize(value: String): String = value.removePrefix("item_").replace('_', ' ').replaceFirstChar { it.uppercase() }
private fun equipmentSlotFor(itemId: String): String? = when (itemId) {
    "item_equipment_acorn_cap", "item_equipment_mushroom_crown" -> "HEAD"
    "item_equipment_bell_charm" -> "CHARM"
    "item_equipment_leaf_pack" -> "PACK"
    "item_relic_cloudglass", "item_relic_dawn_compass" -> "RELIC"
    else -> null
}
private fun raritySymbol(rarity: String): String = when (rarity) { "COMMON" -> "●"; "UNCOMMON" -> "◆"; "RARE" -> "✦"; "EPIC" -> "✧"; else -> "★" }

private const val ACTIVITY_RECOGNITION_PERMISSION = "android.permission.ACTIVITY_RECOGNITION"
private const val NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS"
