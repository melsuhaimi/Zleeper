package com.zleeper.sleepapp.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zleeper.sleepapp.domain.sleep.SleepSchedule
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemePreference { SYSTEM, LIGHT, DARK }
enum class MotionPreference { FULL, REDUCED }

data class AppSettings(
    val onboardingComplete: Boolean = false,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val musicVolume: Float = 0.70f,
    val ambienceVolume: Float = 0.75f,
    val sfxVolume: Float = 0.80f,
    val motion: MotionPreference = MotionPreference.FULL,
    val targetSleepMinutes: Int = 22 * 60 + 30,
    val targetWakeMinutes: Int = 7 * 60,
    val alarmEnabled: Boolean = false,
    val bedtimeReminderEnabled: Boolean = true,
    val windDownReminderEnabled: Boolean = true,
    val morningResultNotificationsEnabled: Boolean = true,
    val notificationPermissionExplained: Boolean = false,
    val activityPermissionExplained: Boolean = false,
    val leftHandedControls: Boolean = false,
    val largeControls: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val screenShakeEnabled: Boolean = true,
    val controlOpacity: Float = 0.90f,
    val trackedQuestId: String? = null,
    val lastRevealedExpeditionId: String? = null,
) {
    val targetDurationMinutes: Int
        get() = SleepSchedule.plannedDurationMinutes(targetSleepMinutes, targetWakeMinutes)

    /** Legacy rendering bridge; persistence remains three independent audio channels. */
    val soundVolume: Float
        get() = (musicVolume + ambienceVolume + sfxVolume) / 3f
}

@Singleton
class SettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    val settings: Flow<AppSettings> = store.data.map { values ->
        AppSettings(
            onboardingComplete = values[Keys.ONBOARDING] ?: false,
            theme = values[Keys.THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM,
            musicVolume = (values[Keys.MUSIC_VOLUME] ?: values[Keys.LEGACY_VOLUME] ?: 0.70f).coerceIn(0f, 1f),
            ambienceVolume = (values[Keys.AMBIENCE_VOLUME] ?: values[Keys.LEGACY_VOLUME] ?: 0.75f).coerceIn(0f, 1f),
            sfxVolume = (values[Keys.SFX_VOLUME] ?: values[Keys.LEGACY_VOLUME] ?: 0.80f).coerceIn(0f, 1f),
            motion = values[Keys.MOTION]?.let { runCatching { MotionPreference.valueOf(it) }.getOrNull() } ?: MotionPreference.FULL,
            targetSleepMinutes = values[Keys.SLEEP_TARGET] ?: 22 * 60 + 30,
            targetWakeMinutes = values[Keys.WAKE_TARGET] ?: 7 * 60,
            alarmEnabled = values[Keys.ALARM] ?: false,
            bedtimeReminderEnabled = values[Keys.BEDTIME_REMINDER] ?: true,
            windDownReminderEnabled = values[Keys.WIND_DOWN] ?: true,
            morningResultNotificationsEnabled = values[Keys.MORNING_RESULTS] ?: true,
            notificationPermissionExplained = values[Keys.NOTIFICATION_EXPLAINED] ?: false,
            activityPermissionExplained = values[Keys.ACTIVITY_EXPLAINED] ?: false,
            leftHandedControls = values[Keys.LEFT_HANDED] ?: false,
            largeControls = values[Keys.LARGE_CONTROLS] ?: false,
            hapticsEnabled = values[Keys.HAPTICS] ?: true,
            screenShakeEnabled = values[Keys.SCREEN_SHAKE] ?: true,
            controlOpacity = (values[Keys.CONTROL_OPACITY] ?: 0.90f).coerceIn(0.45f, 1f),
            trackedQuestId = values[Keys.TRACKED_QUEST],
            lastRevealedExpeditionId = values[Keys.LAST_REVEALED_EXPEDITION],
        )
    }

    suspend fun completeOnboarding() = update(Keys.ONBOARDING, true)
    suspend fun setTheme(value: ThemePreference) = update(Keys.THEME, value.name)
    suspend fun setMusicVolume(value: Float) = update(Keys.MUSIC_VOLUME, value.coerceIn(0f, 1f))
    suspend fun setAmbienceVolume(value: Float) = update(Keys.AMBIENCE_VOLUME, value.coerceIn(0f, 1f))
    suspend fun setSfxVolume(value: Float) = update(Keys.SFX_VOLUME, value.coerceIn(0f, 1f))
    suspend fun setMotion(value: MotionPreference) = update(Keys.MOTION, value.name)

    suspend fun setSleepPlan(sleepMinutes: Int, wakeMinutes: Int) {
        require(sleepMinutes in 0..1439 && wakeMinutes in 0..1439) { "Sleep and wake times must be valid minutes of day" }
        val duration = SleepSchedule.plannedDurationMinutes(sleepMinutes, wakeMinutes)
        require(duration in MIN_PLAN_MINUTES..MAX_PLAN_MINUTES) { "Sleep plan must be between 3 and 15 hours" }
        store.edit { values ->
            values[Keys.SLEEP_TARGET] = sleepMinutes
            values[Keys.WAKE_TARGET] = wakeMinutes
            values.remove(Keys.LEGACY_DURATION_TARGET)
        }
    }

    suspend fun setAlarmEnabled(value: Boolean) = update(Keys.ALARM, value)
    suspend fun setBedtimeReminderEnabled(value: Boolean) = update(Keys.BEDTIME_REMINDER, value)
    suspend fun setWindDownReminderEnabled(value: Boolean) = update(Keys.WIND_DOWN, value)
    suspend fun setMorningResultNotificationsEnabled(value: Boolean) = update(Keys.MORNING_RESULTS, value)
    suspend fun markNotificationPermissionExplained() = update(Keys.NOTIFICATION_EXPLAINED, true)
    suspend fun markActivityPermissionExplained() = update(Keys.ACTIVITY_EXPLAINED, true)
    suspend fun setLeftHandedControls(value: Boolean) = update(Keys.LEFT_HANDED, value)
    suspend fun setLargeControls(value: Boolean) = update(Keys.LARGE_CONTROLS, value)
    suspend fun setHapticsEnabled(value: Boolean) = update(Keys.HAPTICS, value)
    suspend fun setScreenShakeEnabled(value: Boolean) = update(Keys.SCREEN_SHAKE, value)
    suspend fun setControlOpacity(value: Float) = update(Keys.CONTROL_OPACITY, value.coerceIn(0.45f, 1f))
    suspend fun setTrackedQuestId(value: String?) = store.edit { values -> if (value == null) values.remove(Keys.TRACKED_QUEST) else values[Keys.TRACKED_QUEST] = value }
    suspend fun markExpeditionRevealed(expeditionId: String) = update(Keys.LAST_REVEALED_EXPEDITION, expeditionId)
    suspend fun reset() { store.edit { it.clear() } }

    private suspend fun <T> update(key: Preferences.Key<T>, value: T) { store.edit { it[key] = value } }

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val THEME = stringPreferencesKey("theme")
        val LEGACY_VOLUME = floatPreferencesKey("sound_volume")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val AMBIENCE_VOLUME = floatPreferencesKey("ambience_volume")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val MOTION = stringPreferencesKey("motion_preference")
        val SLEEP_TARGET = intPreferencesKey("target_sleep_minutes")
        val WAKE_TARGET = intPreferencesKey("target_wake_minutes")
        val LEGACY_DURATION_TARGET = intPreferencesKey("target_duration_minutes")
        val ALARM = booleanPreferencesKey("alarm_enabled")
        val BEDTIME_REMINDER = booleanPreferencesKey("bedtime_reminder_enabled")
        val WIND_DOWN = booleanPreferencesKey("wind_down_reminder_enabled")
        val MORNING_RESULTS = booleanPreferencesKey("morning_result_notifications_enabled")
        val NOTIFICATION_EXPLAINED = booleanPreferencesKey("notification_permission_explained")
        val ACTIVITY_EXPLAINED = booleanPreferencesKey("activity_permission_explained")
        val LEFT_HANDED = booleanPreferencesKey("left_handed_controls")
        val LARGE_CONTROLS = booleanPreferencesKey("large_controls")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val SCREEN_SHAKE = booleanPreferencesKey("screen_shake_enabled")
        val CONTROL_OPACITY = floatPreferencesKey("control_opacity")
        val TRACKED_QUEST = stringPreferencesKey("tracked_quest_id")
        val LAST_REVEALED_EXPEDITION = stringPreferencesKey("last_revealed_expedition_id")
    }

    private companion object {
        const val MIN_PLAN_MINUTES = 180
        const val MAX_PLAN_MINUTES = 900
    }
}
