package com.zleeper.sleepapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

enum class PetPose(val row: Int, val frames: Int = 8) {
    IDLE(0), BLINK(1), WALK(2), RUN(3), JUMP_START(4), JUMP_LOOP(5),
    FALL(6), LAND(7), INTERACT(8), SLEEP(9), WAKE(10), CELEBRATE(11),
}

@Composable
fun PetSprite(
    formKey: String,
    pose: PetPose,
    frame: Int,
    modifier: Modifier = Modifier,
    facingRight: Boolean = true,
) {
    val atlas = rememberAssetImage("game/atlas/pet/pet_moonmoth_${formKey}_atlas.webp") ?: return
    Canvas(modifier) {
        val side = minOf(size.width, size.height)
        val destination = Size(side, side)
        val topLeft = Offset((size.width - side) / 2f, size.height - side)
        fun DrawScope.paint() = drawImage(
            image = atlas,
            srcOffset = IntOffset((frame % pose.frames) * 256, pose.row * 256),
            srcSize = IntSize(256, 256),
            dstOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
            dstSize = IntSize(destination.width.toInt(), destination.height.toInt()),
        )
        if (facingRight) paint() else scale(-1f, 1f, pivot = center) { paint() }
    }
}
