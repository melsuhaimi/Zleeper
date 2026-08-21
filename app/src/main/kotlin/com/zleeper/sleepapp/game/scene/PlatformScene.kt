package com.zleeper.sleepapp.game.scene

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.zleeper.sleepapp.data.content.SceneDefinition
import com.zleeper.sleepapp.game.simulation.GameInput
import com.zleeper.sleepapp.game.simulation.GameSimulation
import com.zleeper.sleepapp.game.simulation.MotionState
import com.zleeper.sleepapp.game.simulation.PlayerState
import com.zleeper.sleepapp.feature.shell.WorldInteraction
import com.zleeper.sleepapp.feature.shell.WorldInteractionKind
import com.zleeper.sleepapp.ui.theme.ZleeperArtColors
import com.zleeper.sleepapp.ui.components.rememberAssetImage
import kotlin.math.abs

@Composable
fun PlatformScene(
    scene: SceneDefinition,
    petFormKey: String,
    modifier: Modifier = Modifier,
    largeControls: Boolean = false,
    leftHandedControls: Boolean = false,
    controlOpacity: Float = .90f,
    interaction: WorldInteraction? = null,
    collectedContentIds: Set<String> = emptySet(),
    onClose: () -> Unit,
    onDismissInteraction: () -> Unit,
    onInteract: (String) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val regionKey = scene.regionId.removePrefix("region_")
    val background = rememberAssetImage("game/atlas/region/region_${regionKey}_bg_far.webp")
    val parallaxLayers = listOf("mid", "near").map { layer -> rememberAssetImage("game/atlas/region/region_${regionKey}_bg_${layer}.webp") }
    val petAtlas = rememberAssetImage("game/atlas/pet/pet_moonmoth_${petFormKey}_atlas.webp")
    val interactableArt = mutableMapOf<String, androidx.compose.ui.graphics.ImageBitmap?>()
    for (interactable in scene.interactables) {
            val path = if (interactable.type == "NPC") {
                "game/atlas/npc/${interactable.contentId}_atlas.webp"
            } else {
                "game/item/icon/${interactable.contentId}_icon.webp"
            }
            interactableArt[interactable.id] = rememberAssetImage(path)
    }
    if (background == null || petAtlas == null || parallaxLayers.any { it == null }) {
        Box(modifier.fillMaxSize().background(ZleeperArtColors.DeepNight), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Opening ${scene.name}", style = MaterialTheme.typography.titleLarge); Text("Gathering the trail…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        return
    }
    val readyParallax = parallaxLayers.filterNotNull()
    val simulation = remember(scene.id) { GameSimulation(scene.platforms, scene.worldWidth, scene.worldHeight) }
    var player by remember(scene.id) { mutableStateOf(PlayerState(scene.spawn.x, scene.spawn.y)) }
    var sceneTime by remember(scene.id) { mutableFloatStateOf(0f) }
    var input by remember { mutableStateOf(GameInput()) }
    val visibleInteractables = scene.interactables.filterNot { it.type != "NPC" && it.contentId in collectedContentIds }
    val nearest = visibleInteractables.minByOrNull { abs((it.x + 24f) - (player.x + GameSimulation.WIDTH / 2f)) }
        ?.takeIf { abs(it.x - player.x) < 155f && abs(it.y - player.y) < 180f }

    LaunchedEffect(scene.id) {
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val delta = (now - previous) / 1_000_000_000f
            player = simulation.step(player, input, delta)
            sceneTime += delta.coerceIn(0f, 1f / 20f)
            previous = now
            if (input.jump || input.interact) input = input.copy(jump = false, interact = false)
        }
    }
    LaunchedEffect(interaction) {
        if (interaction != null) input = GameInput()
    }

    Box(modifier.fillMaxSize().background(ZleeperArtColors.DeepNight)) {
        Canvas(Modifier.fillMaxSize()) {
            val worldScale = size.height / scene.worldHeight
            val cameraX = (player.x * worldScale - size.width * .42f)
                .coerceIn(0f, (scene.worldWidth * worldScale - size.width).coerceAtLeast(0f))

            val backdropWidth = maxOf(size.width, background.width * (size.height / background.height))
            val backdropTravel = (backdropWidth - size.width).coerceAtLeast(0f)
            val worldTravel = (scene.worldWidth * worldScale - size.width).coerceAtLeast(1f)
            val backdropX = -(cameraX / worldTravel) * backdropTravel
            drawImage(
                background,
                dstOffset = IntOffset(backdropX.toInt(), 0),
                dstSize = IntSize(backdropWidth.toInt(), size.height.toInt()),
            )
            readyParallax.forEachIndexed { index, layer ->
                val extraTravel = cameraX * (.025f + index * .025f)
                drawImage(
                    layer,
                    dstOffset = IntOffset((backdropX - extraTravel).toInt(), 0),
                    dstSize = IntSize((backdropWidth + size.width * .08f).toInt(), size.height.toInt()),
                )
            }

            translate(left = -cameraX) {
                scene.platforms.forEach { platform ->
                    val left = platform.x * worldScale
                    val top = platform.y * worldScale
                    val width = platform.width * worldScale
                    val height = platform.height * worldScale
                    drawRoundRect(ZleeperArtColors.PlatformBody, Offset(left, top), Size(width, height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
                    drawRoundRect(ZleeperArtColors.PlatformEdge, Offset(left, top), Size(width, minOf(10f, height)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
                    val tuftStep = 34f
                    var tuftX = left + 14f
                    while (tuftX < left + width - 10f) {
                        val grass = Path().apply { moveTo(tuftX - 6f, top + 2f); lineTo(tuftX, top - 8f); lineTo(tuftX + 6f, top + 2f); close() }
                        drawPath(grass, ZleeperArtColors.Grass); tuftX += tuftStep
                    }
                }

                visibleInteractables.forEach { item ->
                    val image = interactableArt[item.id]
                    val isNpc = item.type == "NPC"
                    val sideWorld = if (isNpc) 104f else 58f
                    val side = sideWorld * worldScale
                    val x = item.x * worldScale - side / 2f
                    val y = item.y * worldScale - side
                    if (image != null) {
                        val npcFrame = if (isNpc && interaction?.kind == WorldInteractionKind.DIALOGUE && nearest?.id == item.id) ((sceneTime * 4f).toInt() % 3) else 0
                        drawImage(
                            image,
                            srcOffset = IntOffset(if (isNpc) npcFrame * 128 else 0, 0),
                            srcSize = IntSize(if (isNpc) minOf(128, image.width) else image.width, image.height),
                            dstOffset = IntOffset(x.toInt(), y.toInt()),
                            dstSize = IntSize(side.toInt(), side.toInt()),
                        )
                    } else {
                        drawCircle(ZleeperArtColors.Lantern, side * .24f, Offset(item.x * worldScale, y + side * .5f))
                    }
                    if (nearest?.id == item.id) {
                        drawCircle(ZleeperArtColors.Lantern.copy(alpha = .33f), side * .48f, Offset(item.x * worldScale, y + side * .5f))
                    }
                }

                val row = player.motion.atlasRow()
                val fps = player.motion.framesPerSecond()
                val frame = ((player.motionTimeSeconds * fps).toInt()).coerceAtLeast(0) % 8
                val spriteWorldSize = 132f
                val spriteLeft = (player.x + GameSimulation.WIDTH / 2f - spriteWorldSize / 2f) * worldScale
                val spriteTop = (player.y + GameSimulation.HEIGHT - spriteWorldSize) * worldScale
                val spriteSide = spriteWorldSize * worldScale
                val drawPet = {
                    drawImage(
                        petAtlas,
                        srcOffset = IntOffset(frame * 256, row * 256),
                        srcSize = IntSize(256, 256),
                        dstOffset = IntOffset(spriteLeft.toInt(), spriteTop.toInt()),
                        dstSize = IntSize(spriteSide.toInt(), spriteSide.toInt()),
                    )
                }
                if (player.facingRight) drawPet() else scale(-1f, 1f, pivot = Offset(spriteLeft + spriteSide / 2f, spriteTop + spriteSide / 2f)) { drawPet() }
            }
        }

        Surface(
            Modifier.align(Alignment.TopCenter).padding(horizontal = 14.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .86f),
            shape = RoundedCornerShape(22.dp),
        ) {
            Row(Modifier.padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClose) { Icon(Icons.Rounded.Close, "Leave ${scene.name}") }
                Column { Text(scene.name, style = MaterialTheme.typography.titleLarge); Text("${scene.regionId.removePrefix("region_").replace('_', ' ').replaceFirstChar { it.uppercase() }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            }
        }

        nearest?.takeIf { interaction == null }?.let { target ->
            Surface(
                Modifier.align(Alignment.BottomCenter).padding(bottom = if (largeControls) 126.dp else 108.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .90f),
                shape = CircleShape,
            ) { Text(if (target.type == "NPC") "Talk" else "Collect", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge) }
        }

        interaction?.let { message ->
            Surface(
                onClick = onDismissInteraction,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = if (largeControls) 118.dp else 104.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .97f),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 10.dp,
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(if (message.kind == WorldInteractionKind.DIALOGUE) Icons.Rounded.Pets else Icons.Rounded.KeyboardArrowUp, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) { Text(message.title, style = MaterialTheme.typography.titleLarge); Text(message.body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                    Text("Tap to close", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        val controlSize = if (largeControls) 72.dp else 62.dp
        val movement: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldControl(Icons.AutoMirrored.Rounded.ArrowBack, "Move left", "move-left", controlSize, controlOpacity) { pressed -> if (pressed) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); input = input.copy(horizontal = if (pressed) -1f else if (input.horizontal < 0f) 0f else input.horizontal) }
                HoldControl(Icons.AutoMirrored.Rounded.ArrowForward, "Move right", "move-right", controlSize, controlOpacity) { pressed -> if (pressed) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); input = input.copy(horizontal = if (pressed) 1f else if (input.horizontal > 0f) 0f else input.horizontal) }
            }
        }
        val actions: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                if (nearest != null) ActionControl(Icons.Rounded.Pets, if (nearest.type == "NPC") "Talk" else "Collect", "interact", controlSize - 8.dp, controlOpacity) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress); input = input.copy(interact = true); onInteract(nearest.contentId)
                }
                ActionControl(Icons.Rounded.KeyboardArrowUp, "Jump", "jump", controlSize, controlOpacity) { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); input = input.copy(jump = true) }
            }
        }
        if (interaction == null) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                if (leftHandedControls) { actions(); movement() } else { movement(); actions() }
            }
        }
    }
}

private fun MotionState.atlasRow(): Int = when (this) {
    MotionState.IDLE -> 0; MotionState.WALK -> 2; MotionState.RUN -> 3; MotionState.JUMP_START -> 4
    MotionState.JUMP_LOOP -> 5; MotionState.FALL -> 6; MotionState.LAND -> 7; MotionState.INTERACT -> 8
}

private fun MotionState.framesPerSecond(): Int = when (this) {
    MotionState.IDLE -> 5; MotionState.WALK -> 9; MotionState.RUN -> 13
    MotionState.JUMP_START, MotionState.LAND -> 14
    MotionState.JUMP_LOOP, MotionState.FALL -> 10
    MotionState.INTERACT -> 9
}

@Composable
private fun HoldControl(icon: ImageVector, description: String, tag: String, size: androidx.compose.ui.unit.Dp, opacity: Float, onHold: (Boolean) -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .size(size)
            .testTag(tag)
            .semantics { contentDescription = description; role = Role.Button }
            .pointerInput(description) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true; onHold(true)
                    try { waitForUpOrCancellation() } finally { pressed = false; onHold(false) }
                }
            },
        shape = CircleShape,
        color = if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = opacity) else MaterialTheme.colorScheme.surface.copy(alpha = opacity),
        shadowElevation = if (pressed) 1.dp else 8.dp,
    ) { Box(contentAlignment = Alignment.Center) { Icon(icon, description, tint = if (pressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface) } }
}

@Composable
private fun ActionControl(icon: ImageVector, description: String, tag: String, size: androidx.compose.ui.unit.Dp, opacity: Float, onPress: () -> Unit) {
    Surface(onClick = onPress, modifier = Modifier.size(size).testTag(tag), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = opacity), shadowElevation = 9.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, description, tint = MaterialTheme.colorScheme.onPrimary) }
    }
}
