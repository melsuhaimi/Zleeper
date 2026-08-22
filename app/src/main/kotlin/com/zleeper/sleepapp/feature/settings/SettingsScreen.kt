package com.zleeper.sleepapp.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.data.local.preferences.MotionPreference
import com.zleeper.sleepapp.data.local.preferences.ThemePreference
import com.zleeper.sleepapp.domain.sleep.SleepSchedule
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: ZleeperUiState,
    onSaveSleepPlan: (Int, Int) -> Unit,
    onAlarmEnabled: (Boolean) -> Unit,
    onBedtimeReminderEnabled: (Boolean) -> Unit,
    onWindDownReminderEnabled: (Boolean) -> Unit,
    onMorningResultsEnabled: (Boolean) -> Unit,
    onMusicVolume: (Float) -> Unit,
    onAmbienceVolume: (Float) -> Unit,
    onSfxVolume: (Float) -> Unit,
    onLargeControls: (Boolean) -> Unit,
    onLeftHandedControls: (Boolean) -> Unit,
    onControlOpacity: (Float) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onScreenShake: (Boolean) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onTheme: (ThemePreference) -> Unit,
) {
    var bedtime by rememberSaveable(state.settings.targetSleepMinutes) { mutableIntStateOf(state.settings.targetSleepMinutes) }
    var wakeTime by rememberSaveable(state.settings.targetWakeMinutes) { mutableIntStateOf(state.settings.targetWakeMinutes) }
    val plannedMinutes = SleepSchedule.plannedDurationMinutes(bedtime, wakeTime)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("SETTINGS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Routine, sound and play", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            SettingsCard("Sleep") {
                TimeSlider("Bedtime", bedtime) { bedtime = snapQuarterHour(it) }
                TimeSlider("Wake time", wakeTime) { wakeTime = snapQuarterHour(it) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Derived planned window", fontWeight = FontWeight.SemiBold)
                    Text(formatDuration(plannedMinutes), color = MaterialTheme.colorScheme.primary)
                }
                Button(onClick = { onSaveSleepPlan(bedtime, wakeTime) }, modifier = Modifier.fillMaxWidth()) { Text("Save sleep plan") }
                HorizontalDivider()
                Toggle("Wake alarm", "User-critical scheduled wake alarm", state.settings.alarmEnabled, onAlarmEnabled)
                Toggle("Bedtime reminder", "Never starts tracking automatically", state.settings.bedtimeReminderEnabled, onBedtimeReminderEnabled)
                Toggle("Wind-down reminder", "Quiet reminder before the planned bedtime", state.settings.windDownReminderEnabled, onWindDownReminderEnabled)
                Toggle("Morning results", "Independent from the wake alarm channel", state.settings.morningResultNotificationsEnabled, onMorningResultsEnabled)
            }
        }
        item {
            SettingsCard("Audio") {
                VolumeSlider("Game music", state.settings.musicVolume, onMusicVolume)
                VolumeSlider("Region ambience", state.settings.ambienceVolume, onAmbienceVolume)
                VolumeSlider("Game SFX", state.settings.sfxVolume, onSfxVolume)
                Text("Sleep sounds are not exposed because this candidate has no production sleep-sound asset.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SettingsCard("Game controls") {
                Toggle("Large controls", "Larger movement and jump targets", state.settings.largeControls, onLargeControls)
                Toggle("Left-handed layout", "Moves the action layout for left-handed play", state.settings.leftHandedControls, onLeftHandedControls)
                Toggle("Haptics", "Tactile game feedback", state.settings.hapticsEnabled, onHaptics)
                Toggle("Screen shake", "Can be disabled independently", state.settings.screenShakeEnabled, onScreenShake)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Control opacity", fontWeight = FontWeight.SemiBold)
                    Text("${(state.settings.controlOpacity * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary)
                }
                Slider(value = state.settings.controlOpacity, onValueChange = onControlOpacity, valueRange = .45f..1f)
            }
        }
        item {
            SettingsCard("Visual and accessibility") {
                Toggle("Reduced motion", "Stops nonessential navigation and environmental motion", state.settings.motion == MotionPreference.REDUCED, onReducedMotion)
                Text("Appearance", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePreference.entries.forEach { theme ->
                        val label = theme.name.lowercase().replaceFirstChar { it.uppercase() }
                        if (state.settings.theme == theme) Button(onClick = { onTheme(theme) }) { Text(label) }
                        else FilledTonalButton(onClick = { onTheme(theme) }) { Text(label) }
                    }
                }
            }
        }
        state.operationError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun Toggle(label: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun VolumeSlider(label: String, value: Float, onValue: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text("${(value * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary)
    }
    Slider(value = value, onValueChange = onValue, valueRange = 0f..1f)
}

@Composable
private fun TimeSlider(label: String, minutes: Int, onValue: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(formatMinutes(minutes), color = MaterialTheme.colorScheme.primary)
    }
    Slider(value = minutes.toFloat(), onValueChange = onValue, valueRange = 0f..1439f)
}

private fun snapQuarterHour(value: Float): Int = ((value / 15f).roundToInt() * 15).coerceIn(0, 1439)
private fun formatDuration(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"
private fun formatMinutes(minutes: Int): String {
    val hour24 = (minutes / 60) % 24
    val minute = minutes % 60
    val suffix = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val hour = hour24 % 12) { 0 -> 12; else -> hour }
    return "%d:%02d %s".format(hour12, minute, suffix)
}
