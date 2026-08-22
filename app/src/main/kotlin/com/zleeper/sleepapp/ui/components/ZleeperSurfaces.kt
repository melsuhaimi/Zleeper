package com.zleeper.sleepapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.ui.theme.ZleeperArtColors

@Composable
fun StorybookBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.background(
            Brush.verticalGradient(listOf(ZleeperArtColors.BackdropTop, ZleeperArtColors.BackdropMiddle, ZleeperArtColors.BackdropBottom)),
        ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(ZleeperArtColors.Lantern.copy(alpha = .15f), radius = size.minDimension * .42f, center = Offset(size.width * .82f, size.height * .12f))
            drawCircle(ZleeperArtColors.Sky.copy(alpha = .09f), radius = size.minDimension * .50f, center = Offset(size.width * .12f, size.height * .73f))
            drawCircle(ZleeperArtColors.Starlight.copy(alpha = .07f), radius = 2.5f, center = Offset(size.width * .18f, size.height * .16f))
            drawCircle(ZleeperArtColors.Starlight.copy(alpha = .11f), radius = 1.8f, center = Offset(size.width * .62f, size.height * .28f))
            drawCircle(ZleeperArtColors.Starlight.copy(alpha = .09f), radius = 2.1f, center = Offset(size.width * .90f, size.height * .38f))
        }
        content()
    }
}

@Composable
fun SectionHeader(eyebrow: String, title: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(eyebrow.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = .90f),
        shape = RoundedCornerShape(26.dp),
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, color = color.copy(alpha = .14f), shape = CircleShape) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(color, CircleShape))
            Spacer(Modifier.size(7.dp))
            Text("$label  ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProgressTrack(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(7.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(7.dp).background(color, CircleShape))
    }
}
