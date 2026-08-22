package com.zleeper.sleepapp.feature.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.feature.shell.ZleeperViewModel

private enum class PendingNotificationSetting { BEDTIME, WIND_DOWN, MORNING_RESULTS }

@Composable
fun SettingsRoute(state: ZleeperUiState, viewModel: ZleeperViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var pendingNotification by remember { mutableStateOf<PendingNotificationSetting?>(null) }
    var pendingWakeAlarm by remember { mutableStateOf(false) }

    fun applyNotificationSetting(setting: PendingNotificationSetting, enabled: Boolean) {
        when (setting) {
            PendingNotificationSetting.BEDTIME -> viewModel.setBedtimeReminderEnabled(enabled)
            PendingNotificationSetting.WIND_DOWN -> viewModel.setWindDownReminderEnabled(enabled)
            PendingNotificationSetting.MORNING_RESULTS -> viewModel.setMorningResultNotificationsEnabled(enabled)
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val setting = pendingNotification
        pendingNotification = null
        if (granted && setting != null) applyNotificationSetting(setting, true)
    }

    fun changeNotificationSetting(setting: PendingNotificationSetting, enabled: Boolean) {
        if (!enabled) {
            applyNotificationSetting(setting, false)
            return
        }
        val permissionNeeded = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (permissionNeeded) {
            viewModel.markNotificationPermissionExplained()
            pendingNotification = setting
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            applyNotificationSetting(setting, true)
        }
    }

    DisposableEffect(lifecycleOwner, pendingWakeAlarm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingWakeAlarm) {
                val allowed = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                if (allowed) {
                    pendingWakeAlarm = false
                    viewModel.setAlarmEnabled(true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsScreen(
        state = state,
        onSaveSleepPlan = { sleep, wake -> viewModel.saveSleepPlan(sleep, wake) },
        onAlarmEnabled = { enabled ->
            if (!enabled) {
                pendingWakeAlarm = false
                viewModel.setAlarmEnabled(false)
            } else {
                val exactAlarmNeeded = Build.VERSION.SDK_INT >= 31 &&
                    !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                if (exactAlarmNeeded) {
                    pendingWakeAlarm = true
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
                } else {
                    viewModel.setAlarmEnabled(true)
                }
            }
        },
        onBedtimeReminderEnabled = { changeNotificationSetting(PendingNotificationSetting.BEDTIME, it) },
        onWindDownReminderEnabled = { changeNotificationSetting(PendingNotificationSetting.WIND_DOWN, it) },
        onMorningResultsEnabled = { changeNotificationSetting(PendingNotificationSetting.MORNING_RESULTS, it) },
        onMusicVolume = viewModel::setMusicVolume,
        onAmbienceVolume = viewModel::setAmbienceVolume,
        onSfxVolume = viewModel::setSfxVolume,
        onLargeControls = viewModel::setLargeControls,
        onLeftHandedControls = viewModel::setLeftHandedControls,
        onControlOpacity = viewModel::setControlOpacity,
        onHaptics = viewModel::setHapticsEnabled,
        onScreenShake = viewModel::setScreenShakeEnabled,
        onReducedMotion = viewModel::setReducedMotion,
        onTheme = viewModel::setTheme,
    )
}
