package com.zleeper.sleepapp.feature.world

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.data.content.HearthStageDefinition
import com.zleeper.sleepapp.data.content.RegionDefinition
import com.zleeper.sleepapp.data.local.preferences.MotionPreference
import com.zleeper.sleepapp.domain.pet.CompanionActivity
import com.zleeper.sleepapp.domain.pet.CompanionDirector
import com.zleeper.sleepapp.domain.pet.CompanionMemoryContext
import com.zleeper.sleepapp.domain.progression.LevelCurve
import com.zleeper.sleepapp.domain.progression.ProgressionCalculator
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.components.PetPose
import com.zleeper.sleepapp.ui.components.PetSprite
import com.zleeper.sleepapp.ui.components.ProgressTrack
import com.zleeper.sleepapp.ui.components.StorybookBackdrop
import com.zleeper.sleepapp.ui.components.rememberAssetImage
import kotlinx.coroutines.delay

@Composable
fun WorldHubScreen(
    state: ZleeperUiState,
    onOpenScene: (String) -> Unit,
) {
    val pet = state.pet ?: return
    val form = state.forms.firstOrNull { it.id == pet.formId } ?: return
    val progressionRules = state.progressionRules ?: return
    val memories = state.hearth?.memories ?: 0
    val orderedStages = state.hearthStages.sortedBy { it.order }
    val stage = orderedStages.lastOrNull { memories >= it.requiredMemories } ?: orderedStages.firstOrNull()
    val nextStage = orderedStages.firstOrNull { it.requiredMemories > memories }
    val curve = progressionRules.levelCurve.let { LevelCurve(it.base, it.linear, it.exponent) }
    val xpIntoLevel = ProgressionCalculator.xpIntoLevel(pet.totalXp, pet.level, curve)
    val xpNeeded = ProgressionCalculator.xpToNextLevel(pet.level, curve)

    StorybookBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("world-hub"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                LivingHearth(
                    state = state,
                    stage = stage,
                    formKey = form.assetKey,
                    xpIntoLevel = xpIntoLevel,
                    xpNeeded = xpNeeded,
                )
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("THE WAKING WORLD", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text("Choose a trail", style = MaterialTheme.typography.headlineMedium)
                    state.trackedQuest?.let { tracked ->
                        val quest = state.quests.firstOrNull { it.id == tracked.questId }
                        if (quest != null) Text("Tracked · ${quest.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(state.regions, key = { it.id }) { region ->
                RegionCard(region, pet.level >= region.unlockLevel, onOpenScene)
            }
            item {
                HearthProgressCard(memories, stage, nextStage)
            }
        }
    }
}

@Composable
private fun LivingHearth(
    state: ZleeperUiState,
    stage: HearthStageDefinition?,
    formKey: String,
    xpIntoLevel: Int,
    xpNeeded: Int,
) {
    val pet = requireNotNull(state.pet)
    val background = rememberAssetImage("game/atlas/hub/hub_moonmoth_hearth.webp")
    val recentMemoryContexts = remember(state.petMemories) {
        state.petMemories.take(8).map { CompanionMemoryContext(it.memoryType, it.subjectId, it.thoughtKey) }
    }
    val reducedMotion = state.settings.motion == MotionPreference.REDUCED
    var step by remember(pet.instanceId) { mutableIntStateOf(0) }
    var moment by remember(pet.instanceId, recentMemoryContexts, reducedMotion) {
        mutableStateOf(CompanionDirector.moment(pet.instanceId.hashCode().toLong(), 0, recentMemoryContexts, reducedMotion))
    }
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(pet.instanceId, recentMemoryContexts, reducedMotion) {
        while (true) {
            val next = CompanionDirector.moment(pet.instanceId.hashCode().toLong(), step, recentMemoryContexts, reducedMotion)
            moment = next
            frame = 0
            delay(next.durationMs)
            step++
        }
    }
    LaunchedEffect(moment.activity, reducedMotion) {
        if (reducedMotion) {
            frame = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(120)
            frame++
        }
    }

    val targetOffset = when (moment.activity) {
        CompanionActivity.WANDER_LEFT -> (-42).dp
        CompanionActivity.WANDER_RIGHT -> 42.dp
        else -> 0.dp
    }
    val petOffset by animateDpAsState(targetValue = if (reducedMotion) 0.dp else targetOffset, label = "companion-wander")
    val pose = when (moment.activity) {
        CompanionActivity.WANDER_LEFT, CompanionActivity.WANDER_RIGHT -> PetPose.WALK
        CompanionActivity.INSPECT -> PetPose.INTERACT
        CompanionActivity.CELEBRATE -> PetPose.CELEBRATE
        CompanionActivity.NAP -> PetPose.SLEEP
        else -> PetPose.IDLE
    }

    Box(Modifier.fillMaxWidth().height(430.dp)) {
        if (background != null) {
            Image(background, contentDescription = "The persistent Hearth", modifier = Modifier.fillMaxSize())
        }
        HearthAtmosphere(stage?.order ?: 1, reducedMotion)
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stage?.name?.uppercase() ?: "THE HEARTH", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text(pet.displayName, style = MaterialTheme.typography.headlineLarge)
                    Text("Level ${pet.level} · ${state.forms.firstOrNull { it.id == pet.formId }?.name.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .78f)) {
                    Text("${pet.dreamSparksAvailable} Dream Sparks", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                PetSprite(
                    formKey = formKey,
                    pose = pose,
                    frame = frame,
                    facingRight = moment.activity != CompanionActivity.WANDER_LEFT,
                    modifier = Modifier.size(215.dp).offset(x = petOffset),
                )
            }
            Text("XP ${xpIntoLevel.coerceAtMost(xpNeeded)} / $xpNeeded", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            ProgressTrack((xpIntoLevel.toFloat() / xpNeeded.coerceAtLeast(1)).coerceIn(0f, 1f), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("Energy", pet.energy, "Expedition reach")
                StatChip("Focus", pet.focus, "Hidden routes and discoveries")
                StatChip("Resilience", pet.resilience, "Hazard cost and difficult regions")
            }
            state.petMemories.firstOrNull()?.let { memory ->
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .84f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("RECENT MEMORY", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        Text(memory.detail?.takeIf { it.isNotBlank() } ?: memory.memoryType.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }
}

@Composable
private fun HearthAtmosphere(stageOrder: Int, reducedMotion: Boolean) {
    val glow = MaterialTheme.colorScheme.primary.copy(alpha = if (reducedMotion) .12f else .20f)
    Canvas(Modifier.fillMaxSize()) {
        val points = listOf(.15f to .30f, .78f to .25f, .28f to .72f, .68f to .64f, .50f to .46f)
        points.take(stageOrder.coerceIn(1, points.size)).forEachIndexed { index, (x, y) ->
            drawCircle(glow, radius = 16f + index * 3f, center = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y))
        }
    }
}

@Composable
private fun StatChip(label: String, value: Int, meaning: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .86f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            Text("$label $value", fontWeight = FontWeight.SemiBold)
            Text(meaning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RegionCard(region: RegionDefinition, unlocked: Boolean, onOpenScene: (String) -> Unit) {
    Surface(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (unlocked) "OPEN TRAIL" else "LEVEL ${region.unlockLevel}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text(region.name, style = MaterialTheme.typography.titleLarge)
            Text(region.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { region.scenes.firstOrNull()?.let(onOpenScene) },
                enabled = unlocked && region.scenes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (unlocked) "Explore" else "Locked")
            }
        }
    }
}

@Composable
private fun HearthProgressCard(
    memories: Int,
    stage: HearthStageDefinition?,
    nextStage: HearthStageDefinition?,
) {
    Surface(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("HEARTH", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Stage ${stage?.order ?: 1} · ${stage?.name ?: "Hearth"}", style = MaterialTheme.typography.titleLarge)
            stage?.features?.takeIf { it.isNotEmpty() }?.let { features ->
                Text(
                    features.joinToString(" · ") { feature -> feature.replace('_', ' ').replaceFirstChar { it.uppercase() } },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (nextStage == null) {
                Text("All current Hearth restorations are visible.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val currentFloor = stage?.requiredMemories ?: 0
                val span = (nextStage.requiredMemories - currentFloor).coerceAtLeast(1)
                val progress = ((memories - currentFloor).toFloat() / span).coerceIn(0f, 1f)
                Text("Next restoration · ${nextStage.name}")
                ProgressTrack(progress, MaterialTheme.colorScheme.secondary)
                Text("$memories / ${nextStage.requiredMemories} memories", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
