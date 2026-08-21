package com.zleeper.sleepapp.game.scene

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.data.content.SceneDefinition
import com.zleeper.sleepapp.game.simulation.GameInput
import com.zleeper.sleepapp.game.simulation.GameSimulation
import com.zleeper.sleepapp.game.simulation.PlayerState
import com.zleeper.sleepapp.game.simulation.MotionState

@Composable
fun PlatformScene(scene: SceneDefinition, petFormKey: String, modifier: Modifier = Modifier, onInteract: (String) -> Unit) {
    val context = LocalContext.current
    val regionKey = scene.regionId.removePrefix("region_")
    val layers = remember(scene.regionId) { listOf("far", "mid", "near").map { layer -> context.assets.open("game/atlas/region/region_${regionKey}_bg_${layer}.webp").use { BitmapFactory.decodeStream(it).asImageBitmap() } } }
    val petAtlas = remember(petFormKey) { context.assets.open("game/atlas/pet/pet_moonmoth_${petFormKey}_atlas.webp").use { BitmapFactory.decodeStream(it).asImageBitmap() } }
    val simulation = remember(scene.id) { GameSimulation(scene.platforms, scene.worldWidth, scene.worldHeight) }
    var player by remember(scene.id) { mutableStateOf(PlayerState(scene.spawn.x, scene.spawn.y)) }
    var input by remember { mutableStateOf(GameInput()) }
    LaunchedEffect(scene.id) {
        var previous = withFrameNanos { it }
        while (true) { val now = withFrameNanos { it }; player = simulation.step(player, input, (now - previous) / 1_000_000_000f); previous = now; if (input.jump || input.interact) input = input.copy(jump = false, interact = false) }
    }
    Box(modifier.fillMaxSize().background(Color(0xFF152F3B))) {
        Canvas(Modifier.fillMaxSize()) {
            val scale = size.height / scene.worldHeight
            val cameraX = (player.x * scale - size.width * 0.42f).coerceIn(0f, (scene.worldWidth * scale - size.width).coerceAtLeast(0f))
            layers.forEachIndexed { index, image -> drawImage(image, dstOffset = IntOffset((-cameraX * index * .12f).toInt(), 0), dstSize = IntSize((size.width * 1.15f).toInt(), size.height.toInt())) }
            translate(left = -cameraX) {
                scene.platforms.forEach { platform -> drawRoundRect(Color(0xFF72916E), Offset(platform.x*scale, platform.y*scale), Size(platform.width*scale, platform.height*scale)) }
                scene.interactables.forEach { item -> drawCircle(if (item.type == "NPC") Color(0xFFE6A66E) else Color(0xFFBEE9D0), 18f, Offset(item.x*scale, item.y*scale)) }
                val row = player.motion.atlasRow()
                val fps = player.motion.framesPerSecond()
                val frame = ((System.currentTimeMillis() / (1000L / fps)) % 8L).toInt()
                val spriteWorldSize = 128f
                drawImage(
                    petAtlas,
                    srcOffset = IntOffset(frame * 256, row * 256),
                    srcSize = IntSize(256, 256),
                    dstOffset = IntOffset(((player.x + GameSimulation.WIDTH / 2f - spriteWorldSize / 2f) * scale).toInt(), ((player.y + GameSimulation.HEIGHT - spriteWorldSize) * scale).toInt()),
                    dstSize = IntSize((spriteWorldSize * scale).toInt(), (spriteWorldSize * scale).toInt()),
                )
            }
        }
        Row(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            HoldControl(Icons.Rounded.ArrowBack, "Move left") { pressed -> input = input.copy(horizontal = if (pressed) -1f else 0f) }
            HoldControl(Icons.Rounded.ArrowForward, "Move right") { pressed -> input = input.copy(horizontal = if (pressed) 1f else 0f) }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Control(Icons.Rounded.KeyboardArrowUp, "Jump") { input = input.copy(jump = true) }
            Control(Icons.Rounded.Pets, "Interact") {
                input = input.copy(interact = true)
                scene.interactables.minByOrNull { kotlin.math.abs(it.x - player.x) }?.takeIf { kotlin.math.abs(it.x - player.x) < 180f }?.let { onInteract(it.contentId) }
            }
        }
    }
}

private fun MotionState.atlasRow(): Int = when (this) { MotionState.IDLE -> 0; MotionState.WALK -> 2; MotionState.RUN -> 3; MotionState.JUMP_START -> 4; MotionState.JUMP_LOOP -> 5; MotionState.FALL -> 6; MotionState.LAND -> 7; MotionState.INTERACT -> 8 }
private fun MotionState.framesPerSecond(): Int = when (this) { MotionState.IDLE -> 5; MotionState.WALK -> 9; MotionState.RUN -> 13; MotionState.JUMP_START, MotionState.LAND -> 14; MotionState.JUMP_LOOP, MotionState.FALL -> 10; MotionState.INTERACT -> 9 }

@Composable private fun Control(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onPress: () -> Unit) {
    FloatingActionButton(onClick = onPress, modifier = Modifier.padding(6.dp).size(58.dp), shape = CircleShape) { Icon(icon, description) }
}

@Composable private fun HoldControl(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onHold: (Boolean) -> Unit) {
    FloatingActionButton(onClick = {}, modifier = Modifier.padding(6.dp).size(58.dp).pointerInput(Unit) { detectTapGestures(onPress = { onHold(true); try { tryAwaitRelease() } finally { onHold(false) } }) }, shape = CircleShape) { Icon(icon, description) }
}
