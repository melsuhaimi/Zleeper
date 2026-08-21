package com.zleeper.sleepapp.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemePreference { SYSTEM, LIGHT, DARK }
enum class MotionPreference { FULL, REDUCED }

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val soundVolume: Float = 0.75f,
    val motion: MotionPreference = MotionPreference.FULL,
    val targetSleepMinutes: Int = 22 * 60 + 30,
    val targetWakeMinutes: Int = 7 * 60,
    val targetDurationMinutes: Int = 8 * 60,
    val alarmEnabled: Boolean = false,
    val windDownReminderEnabled: Boolean = true,
    val notificationPermissionExplained: Boolean = false,
    val activityPermissionExplained: Boolean = false,
    val leftHandedControls: Boolean = false,
    val largeControls: Boolean = false,
    val controlOpacity: Float = 0.90f,
    val lastRevealedExpeditionId: String? = null,
)

@Singleton
class SettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = store.data.map { values ->
        AppSettings(
            onboardingComplete = values[Keys.ONBOARDING] ?: false,
            theme = values[Keys.THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM,
            soundVolume = (values[Keys.VOLUME] ?: 0.75f).coerceIn(0f, 1f),
            motion = values[Keys.MOTION]?.let { runCatching { MotionPreference.valueOf(it) }.getOrNull() } ?: MotionPreference.FULL,
            targetSleepMinutes = values[Keys.SLEEP_TARGET] ?: 22 * 60 + 30,
            targetWakeMinutes = values[Keys.WAKE_TARGET] ?: 7 * 60,
            targetDurationMinutes = values[Keys.DURATION_TARGET] ?: 8 * 60,
            alarmEnabled = values[Keys.ALARM] ?: false,
            windDownReminderEnabled = values[Keys.WIND_DOWN] ?: true,
            notificationPermissionExplained = values[Keys.NOTIFICATION_EXPLAINED] ?: false,
            activityPermissionExplained = values[Keys.ACTIVITY_EXPLAINED] ?: false,
            leftHandedControls = values[Keys.LEFT_HANDED] ?: false,
            largeControls = values[Keys.LARGE_CONTROLS] ?: false,
            controlOpacity = (values[Keys.CONTROL_OPACITY] ?: 0.90f).coerceIn(0.45f, 1f),
            lastRevealedExpeditionId = values[Keys.LAST_REVEALED_EXPEDITION],
        )
    }

    suspend fun completeOnboarding() = update(Keys.ONBOARDING, true)
    suspend fun setTheme(value: ThemePreference) = update(Keys.THEME, value.name)
    suspend fun setVolume(value: Float) = update(Keys.VOLUME, value.coerceIn(0f, 1f))
    suspend fun setMotion(value: MotionPreference) = update(Keys.MOTION, value.name)
    suspend fun setSleepPlan(sleepMinutes: Int, wakeMinutes: Int, durationMinutes: Int) {
        require(sleepMinutes in 0..1439 && wakeMinutes in 0..1439 && durationMinutes in 180..900)
        store.edit { it[Keys.SLEEP_TARGET] = sleepMinutes; it[Keys.WAKE_TARGET] = wakeMinutes; it[Keys.DURATION_TARGET] = durationMinutes }
    }
    suspend fun setAlarmEnabled(value: Boolean) = update(Keys.ALARM, value)
    suspend fun setWindDownReminderEnabled(value: Boolean) = update(Keys.WIND_DOWN, value)
    suspend fun markNotificationPermissionExplained() = update(Keys.NOTIFICATION_EXPLAINED, true)
    suspend fun markActivityPermissionExplained() = update(Keys.ACTIVITY_EXPLAINED, true)
    suspend fun setLeftHandedControls(value: Boolean) = update(Keys.LEFT_HANDED, value)
    suspend fun setLargeControls(value: Boolean) = update(Keys.LARGE_CONTROLS, value)
    suspend fun setControlOpacity(value: Float) = update(Keys.CONTROL_OPACITY, value.coerceIn(0.45f, 1f))
    suspend fun markExpeditionRevealed(expeditionId: String) = update(Keys.LAST_REVEALED_EXPEDITION, expeditionId)
    suspend fun reset() { store.edit { it.clear() } }

    private suspend fun <T> update(key: Preferences.Key<T>, value: T) { store.edit { it[key] = value } }

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val THEME = stringPreferencesKey("theme")
        val VOLUME = floatPreferencesKey("sound_volume")
        val MOTION = stringPreferencesKey("motion_preference")
        val SLEEP_TARGET = intPreferencesKey("target_sleep_minutes")
        val WAKE_TARGET = intPreferencesKey("target_wake_minutes")
        val DURATION_TARGET = intPreferencesKey("target_duration_minutes")
        val ALARM = booleanPreferencesKey("alarm_enabled")
        val WIND_DOWN = booleanPreferencesKey("wind_down_reminder_enabled")
        val NOTIFICATION_EXPLAINED = booleanPreferencesKey("notification_permission_explained")
        val ACTIVITY_EXPLAINED = booleanPreferencesKey("activity_permission_explained")
        val LEFT_HANDED = booleanPreferencesKey("left_handed_controls")
        val LARGE_CONTROLS = booleanPreferencesKey("large_controls")
        val CONTROL_OPACITY = floatPreferencesKey("control_opacity")
        val LAST_REVEALED_EXPEDITION = stringPreferencesKey("last_revealed_expedition_id")
    }
}
