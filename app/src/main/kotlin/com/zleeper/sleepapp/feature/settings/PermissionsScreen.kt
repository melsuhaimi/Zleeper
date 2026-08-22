package com.zleeper.sleepapp.feature.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zleeper.sleepapp.feature.shell.ZleeperUiState

@Composable
fun PermissionsScreen(
    state: ZleeperUiState,
    onActivityPermissionExplained: () -> Unit,
    onNotificationPermissionExplained: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activityPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshKey++ }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshKey++ }

    val activityGranted = remember(refreshKey) {
        Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    }
    val notificationsGranted = remember(refreshKey) {
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val exactAlarmGranted = remember(refreshKey) {
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("PERMISSIONS", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Optional Android capabilities", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Manual sleep sessions and the game remain usable when optional permissions are denied. Permissions only improve estimation, reminders, or precise wake timing.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            PermissionCard(
                title = "Activity Recognition",
                detail = "Lets Google Play services provide phone-derived sleep signals. Without it, Zleeper falls back to explicit start and manual wake anchors.",
                granted = activityGranted,
                requestable = Build.VERSION.SDK_INT >= 29,
                actionLabel = "Allow",
                onRequest = {
                    onActivityPermissionExplained()
                    activityPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                },
            )
        }
        item {
            PermissionCard(
                title = "Notifications",
                detail = "Used for bedtime, wind-down, and morning-result notifications. Wake-alarm capability remains independently controlled.",
                granted = notificationsGranted,
                requestable = Build.VERSION.SDK_INT >= 33,
                actionLabel = "Allow",
                onRequest = {
                    onNotificationPermissionExplained()
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
            )
        }
        item {
            PermissionCard(
                title = "Alarms & reminders",
                detail = "Precise wake alarms require Android exact-alarm access on supported versions. Zleeper requests it only for the explicit wake-alarm feature.",
                granted = exactAlarmGranted,
                requestable = Build.VERSION.SDK_INT >= 31,
                actionLabel = "Open alarm access",
                onRequest = {
                    if (Build.VERSION.SDK_INT >= 31) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
                    }
                },
            )
        }
        if (!activityGranted || !notificationsGranted || !exactAlarmGranted) {
            item {
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open app settings")
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Permission explanations", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Activity explanation: ${if (state.settings.activityPermissionExplained) "shown" else "not yet shown"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Notification explanation: ${if (state.settings.notificationPermissionExplained) "shown" else "not yet shown"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        state.operationError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    detail: String,
    granted: Boolean,
    requestable: Boolean,
    actionLabel: String,
    onRequest: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (granted) "Allowed" else "Not allowed",
                    color = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            HorizontalDivider()
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!granted && requestable) {
                FilledTonalButton(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text(actionLabel) }
            }
        }
    }
}
