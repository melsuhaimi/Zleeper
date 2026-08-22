package com.zleeper.sleepapp.feature.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zleeper.sleepapp.domain.sleep.SleepSchedule
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.feature.shell.ZleeperViewModel
import com.zleeper.sleepapp.ui.components.StorybookBackdrop
import kotlin.math.roundToInt

@Composable
fun OnboardingFlow(
    state: ZleeperUiState,
    viewModel: ZleeperViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var petName by rememberSaveable { mutableStateOf("Lumi") }
    var bedtime by rememberSaveable { mutableIntStateOf(state.settings.targetSleepMinutes) }
    var wakeTime by rememberSaveable { mutableIntStateOf(state.settings.targetWakeMinutes) }
    var pendingAlarmEnable by remember { mutableStateOf(false) }
    val plannedMinutes = SleepSchedule.plannedDurationMinutes(bedtime, wakeTime)

    val activityPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        step = 4
    }

    DisposableEffect(lifecycleOwner, pendingAlarmEnable) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingAlarmEnable) {
                val exactAlarmAllowed = Build.VERSION.SDK_INT < 31 ||
                    context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                if (exactAlarmAllowed) {
                    pendingAlarmEnable = false
                    viewModel.setAlarmEnabled(true)
                    step = 5
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    StorybookBackdrop(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("ZLEEPER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                when (step) {
                    0 -> {
                        Text("A world that keeps living because you went to sleep.", style = MaterialTheme.typography.headlineLarge)
                        Text("Build a quiet nightly routine, then wake to the journey your companion brought home. Phone-based sleep estimates remain local and can be corrected each morning.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    1 -> {
                        Text("Meet your companion", style = MaterialTheme.typography.headlineLarge)
                        Text("One persistent pet grows with your nights, explorations, memories, equipment and evolution choices.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = petName,
                            onValueChange = { petName = it.take(24) },
                            label = { Text("Companion name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    2 -> {
                        Text("Plan your night", style = MaterialTheme.typography.headlineLarge)
                        Text("Choose bedtime and wake time. The planned window is derived automatically, so the three values can never contradict one another.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TimeSlider("Bedtime", bedtime) { bedtime = snapQuarterHour(it) }
                        TimeSlider("Wake time", wakeTime) { wakeTime = snapQuarterHour(it) }
                        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                            Row(Modifier.padding(18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Planned window", fontWeight = FontWeight.SemiBold)
                                Text(formatDuration(plannedMinutes), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    3 -> {
                        Text("Phone-based estimation", style = MaterialTheme.typography.headlineLarge)
                        Text("Activity Recognition lets Google Play services provide sleep-related phone signals. If you deny it or the capability is unavailable, manual Begin Sleep and wake still work.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Other permissions are just in time", style = MaterialTheme.typography.titleMedium)
                        Text("Notification access is requested only when you enable a reminder or morning result, and exact-alarm access only when you enable the wake alarm.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    4 -> {
                        Text("Optional wake alarm", style = MaterialTheme.typography.headlineLarge)
                        Text("Zleeper can schedule a precise wake alarm for ${formatMinutes(wakeTime)}. This is optional and does not control whether a sleep session is valid.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = {
                                val exactAlarmNeeded = Build.VERSION.SDK_INT >= 31 &&
                                    !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                                if (exactAlarmNeeded) {
                                    pendingAlarmEnable = true
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
                                } else {
                                    viewModel.setAlarmEnabled(true)
                                    step = 5
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Enable wake alarm")
                        }
                        if (pendingAlarmEnable) {
                            Text("Return here after choosing Android alarm access. You can still continue without enabling it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        Text("Your first night is ready", style = MaterialTheme.typography.headlineLarge)
                        Text("${formatMinutes(bedtime)} → ${formatMinutes(wakeTime)} · ${formatDuration(plannedMinutes)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        Text("You can change the plan, permissions, wake alarm and reminder settings later. Poor sleep never harms your companion and every legitimate finalized night gives progress.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.operationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) {
                    OutlinedButton(onClick = { pendingAlarmEnable = false; step-- }, modifier = Modifier.weight(.38f).height(54.dp)) { Text("Back") }
                }
                Button(
                    onClick = {
                        when (step) {
                            0, 1 -> step++
                            2 -> {
                                viewModel.saveSleepPlan(bedtime, wakeTime)
                                step++
                            }
                            3 -> {
                                viewModel.markActivityPermissionExplained()
                                if (Build.VERSION.SDK_INT < 29) step = 4
                                else activityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            }
                            4 -> {
                                pendingAlarmEnable = false
                                step = 5
                            }
                            5 -> viewModel.createPet(petName)
                        }
                    },
                    enabled = step != 1 || petName.trim().length >= 2,
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Text(
                        when (step) {
                            4 -> "Not now"
                            5 -> "Enter the Hearth"
                            else -> "Continue"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeSlider(label: String, minutes: Int, onValue: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(formatMinutes(minutes), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = minutes.toFloat(), onValueChange = onValue, valueRange = 0f..1439f)
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
