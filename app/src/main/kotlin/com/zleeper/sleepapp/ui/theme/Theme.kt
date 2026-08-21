package com.zleeper.sleepapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.zleeper.sleepapp.data.local.preferences.ThemePreference

private val ZleeperColorScheme = darkColorScheme(
    primary = LanternGold,
    onPrimary = NightInk,
    primaryContainer = FernDark,
    onPrimaryContainer = MoonCream,
    secondary = Fern,
    onSecondary = NightInk,
    secondaryContainer = MossSurface,
    onSecondaryContainer = MoonCream,
    tertiary = SkyBlue,
    onTertiary = NightInk,
    background = NightInk,
    onBackground = MoonCream,
    surface = DeepForest,
    onSurface = MoonCream,
    surfaceVariant = MossSurface,
    onSurfaceVariant = Mist,
    outline = Color(0xFF6E837B),
    outlineVariant = Color(0xFF334D49),
    error = ErrorCoral,
)

private val ZleeperLightColorScheme = lightColorScheme(
    primary = Color(0xFF526D4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E8CF),
    onPrimaryContainer = Color(0xFF142416),
    secondary = Color(0xFF476A66),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBE7E1),
    onSecondaryContainer = Color(0xFF102523),
    tertiary = Color(0xFF4C6570),
    onTertiary = Color.White,
    background = Color(0xFFF9F5E9),
    onBackground = Color(0xFF20251F),
    surface = Color(0xFFFFFBF0),
    onSurface = Color(0xFF20251F),
    surfaceVariant = Color(0xFFE5E8DC),
    onSurfaceVariant = Color(0xFF535B52),
    outline = Color(0xFF747D73),
    outlineVariant = Color(0xFFC4C9BE),
    error = Color(0xFF9A4035),
)

@Composable
fun ZleeperTheme(preference: ThemePreference = ThemePreference.SYSTEM, content: @Composable () -> Unit) {
    val useDark = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (useDark) ZleeperColorScheme else ZleeperLightColorScheme,
        typography = ZleeperTypography,
        shapes = ZleeperShapes,
        content = content,
    )
}
