package com.zleeper.sleepapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ZleeperColorScheme = darkColorScheme(
    primary = MineralAccent,
    onPrimary = WarmCharcoal,
    background = WarmCharcoal,
    onBackground = OffWhite,
    surface = DeepNeutral,
    onSurface = OffWhite,
    onSurfaceVariant = MutedWarmGray,
)

@Composable
fun ZleeperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZleeperColorScheme,
        typography = ZleeperTypography,
        shapes = ZleeperShapes,
        content = content,
    )
}
