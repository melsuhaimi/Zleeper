package com.zleeper.sleepapp.feature.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.domain.sleep.SleepSchedule
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.components.StorybookBackdrop
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SleepScreen(
    state: ZleeperUiState,
    onSavePlan: (Int, Int) -> Unit,
    onBeginSleep: (Boolean) -> Unit,
    onWake: () -> Unit,
    onAbort: () -> Unit,
) {
    val tracking = state.trackingSession
    if (tracking != null) {
        SleepMode(
            startedAtEpochMs = tracking.sessionStartEpochMs,
            windDownCompleted = tracking.windDownCompleted,
            onWake = onWake,
            onAbort = onAbort,
        )
        return
    }

    var editingPlan by rememberSaveable { mutableStateOf(false) }
    var bedtime by rememberSaveable(state.settings.targetSleepMinutes) { mutableIntStateOf(state.settings.targetSleepMinutes) }
    var wakeTime by rememberSaveable(state.settings.targetWakeMinutes) { mutableIntStateOf(state.settings.targetWakeMinutes) }
    var windDownRemaining by rememberSaveable { mutableIntStateOf(0) }
    var windDownComplete by rememberSaveable { mutableStateOf(false) }
    val plannedMinutes = SleepSchedule.plannedDurationMinutes(bedtime, wakeTime)

    LaunchedEffect(windDownRemaining) {
        if (windDownRemaining > 0) {
            delay(1_000)
            if (windDownRemaining == 1) {
                windDownRemaining = 0
                windDownComplete = true
            } else {
                windDownRemaining--
            }
        }
    }

    StorybookBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("sleep-plan"),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text("Tonight", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text("Make room for rest", style = MaterialTheme.typography.headlineMedium)
                Text("Your plan controls reminders and the nightly expedition window. Sleep remains an estimate you confirm in the morning.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                PlanCard(
                    bedtime = bedtime,
                    wakeTime = wakeTime,
                    plannedMinutes = plannedMinutes,
                    editing = editingPlan,
                    onBedtime = { bedtime = snapQuarterHour(it) },
                    onWakeTime = { wakeTime = snapQuarterHour(it) },
                    onEdit = { editingPlan = true },
                    onSave = {
                        onSavePlan(bedtime, wakeTime)
                        editingPlan = false
                    },
                )
            }
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Scheduled bedtime", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (state.settings.bedtimeReminderEnabled) "Reminder is enabled for ${formatMinutes(state.settings.targetSleepMinutes)}." else "Bedtime reminder is off.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("A reminder never starts sleep by itself. Tracking starts only after you explicitly choose Begin Sleep.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when {
                            windDownRemaining > 0 -> {
                                Text("Wind-down", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                Text("${windDownRemaining}s", style = MaterialTheme.typography.displaySmall)
                                Text("A short, quiet ritual. Nothing is tracked until you explicitly begin sleep.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            windDownComplete -> {
                                Text("Wind-down complete", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                Text("Your night is ready.", style = MaterialTheme.typography.titleLarge)
                                FilledTonalButton(onClick = { onBeginSleep(true) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                    Text("Begin Sleep")
                                }
                                OutlinedButton(
                                    onClick = {
                                        windDownComplete = false
                                        windDownRemaining = 0
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Not yet") }
                            }
                            else -> {
                                Text("Ready for tonight?", style = MaterialTheme.typography.titleLarge)
                                Text("Wind-down is optional. Every finalized night still gives progress.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        windDownComplete = false
                                        windDownRemaining = 60
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                ) { Text("Begin 60-second wind-down") }
                                OutlinedButton(onClick = { onBeginSleep(false) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                    Text("Skip wind-down · Begin Sleep")
                                }
                            }
                        }
                    }
                }
            }
            state.operationError?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun PlanCard(
    bedtime: Int,
    wakeTime: Int,
    plannedMinutes: Int,
    editing: Boolean,
    onBedtime: (Float) -> Unit,
    onWakeTime: (Float) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Sleep plan", style = MaterialTheme.typography.titleLarge)
                    Text("${formatMinutes(bedtime)} → ${formatMinutes(wakeTime)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatDuration(plannedMinutes), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text("Planned window is derived from bedtime and wake time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (editing) {
                PlanSlider("Bedtime", formatMinutes(bedtime), bedtime.toFloat(), onBedtime)
                PlanSlider("Wake time", formatMinutes(wakeTime), wakeTime.toFloat(), onWakeTime)
                Text("Derived window · ${formatDuration(plannedMinutes)}", style = MaterialTheme.typography.labelLarge)
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save plan") }
            } else {
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Adjust bedtime and wake time") }
            }
        }
    }
}

@Composable
private fun PlanSlider(label: String, valueText: String, value: Float, onValue: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(valueText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value, onValueChange = onValue, valueRange = 0f..1439f)
    }
}

@Composable
private fun SleepMode(
    startedAtEpochMs: Long,
    windDownCompleted: Boolean,
    onWake: () -> Unit,
    onAbort: () -> Unit,
) {
    Surface(Modifier.fillMaxSize().testTag("sleep-mode"), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Sleep mode", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Text("The trail is unfolding quietly.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Started ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(startedAtEpochMs))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (windDownCompleted) "Wind-down completed" else "Wind-down skipped", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onWake, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("I'm awake") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onAbort, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Cancel accidental start") }
        }
    }
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
