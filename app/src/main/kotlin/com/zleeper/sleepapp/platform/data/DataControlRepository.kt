package com.zleeper.sleepapp.platform.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import com.zleeper.sleepapp.platform.work.RewardResolutionScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DataControlRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ZleeperDatabase,
    private val settingsRepository: SettingsRepository,
    private val rewardResolutionScheduler: RewardResolutionScheduler,
) {
    suspend fun exportJson(): Uri = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        val root = JSONObject()
            .put("format", "zleeper_local_export_v2")
            .put("exported_at_epoch_ms", System.currentTimeMillis())
            .put(
                "settings",
                JSONObject()
                    .put("onboarding_complete", settings.onboardingComplete)
                    .put("theme", settings.theme.name)
                    .put("music_volume", settings.musicVolume)
                    .put("ambience_volume", settings.ambienceVolume)
                    .put("sfx_volume", settings.sfxVolume)
                    .put("motion", settings.motion.name)
                    .put("target_sleep_minutes", settings.targetSleepMinutes)
                    .put("target_wake_minutes", settings.targetWakeMinutes)
                    .put("target_duration_minutes_derived", settings.targetDurationMinutes)
                    .put("alarm_enabled", settings.alarmEnabled)
                    .put("bedtime_reminder_enabled", settings.bedtimeReminderEnabled)
                    .put("wind_down_reminder_enabled", settings.windDownReminderEnabled)
                    .put("morning_result_notifications_enabled", settings.morningResultNotificationsEnabled)
                    .put("notification_permission_explained", settings.notificationPermissionExplained)
                    .put("activity_permission_explained", settings.activityPermissionExplained)
                    .put("left_handed_controls", settings.leftHandedControls)
                    .put("large_controls", settings.largeControls)
                    .put("haptics_enabled", settings.hapticsEnabled)
                    .put("screen_shake_enabled", settings.screenShakeEnabled)
                    .put("control_opacity", settings.controlOpacity)
                    .put("tracked_quest_id", settings.trackedQuestId ?: JSONObject.NULL)
                    .put("last_revealed_expedition_id", settings.lastRevealedExpeditionId ?: JSONObject.NULL),
            )

        TABLES.forEach { table -> root.put(table, readTable(table)) }

        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(directory, "zleeper-export-${System.currentTimeMillis()}.json").apply { writeText(root.toString(2)) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun deleteSleepHistory() {
        rewardResolutionScheduler.cancelAllAndAwait()
        withContext(Dispatchers.IO) { database.withTransaction { deleteTables(SLEEP_HISTORY_TABLES) } }
    }

    suspend fun resetGameProgress() {
        rewardResolutionScheduler.cancelAllAndAwait()
        withContext(Dispatchers.IO) { database.withTransaction { deleteTables(GAME_PROGRESS_TABLES) } }
    }

    suspend fun deleteAllLocalData() {
        rewardResolutionScheduler.cancelAllAndAwait()
        withContext(Dispatchers.IO) { database.clearAllTables() }
        settingsRepository.reset()
    }

    private fun readTable(table: String): JSONArray {
        val rows = JSONArray()
        database.openHelper.readableDatabase.query("SELECT * FROM $table").use { cursor ->
            while (cursor.moveToNext()) {
                val row = JSONObject()
                cursor.columnNames.forEachIndexed { index, name ->
                    val value: Any = when (cursor.getType(index)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                        android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                        android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                        android.database.Cursor.FIELD_TYPE_BLOB -> android.util.Base64.encodeToString(cursor.getBlob(index), android.util.Base64.NO_WRAP)
                        else -> cursor.getString(index)
                    }
                    row.put(name, value)
                }
                rows.put(row)
            }
        }
        return rows
    }

    private fun deleteTables(tables: List<String>) {
        val sql = database.openHelper.writableDatabase
        tables.forEach { table -> sql.execSQL("DELETE FROM $table") }
    }

    private companion object {
        val TABLES = listOf(
            "sleep_session", "sleep_signal", "night_outcome", "expedition", "expedition_path_node", "expedition_reward",
            "pet", "pet_progression_event", "inventory_stack", "inventory_instance", "inventory_transaction", "equipment_slot",
            "equipment_progression_event", "quest_progress", "quest_objective_progress", "world_unlock", "world_discovery",
            "collection_entry", "morning_note", "pet_specialization", "pet_memory", "hearth_progress", "hearth_progress_event",
            "player_title", "world_scene_completion",
        )
        val SLEEP_HISTORY_TABLES = listOf(
            "morning_note", "night_outcome", "expedition_reward", "expedition_path_node", "expedition", "sleep_signal", "sleep_session",
        )
        val GAME_PROGRESS_TABLES = listOf(
            "equipment_progression_event", "pet_progression_event", "equipment_slot", "inventory_instance", "inventory_stack",
            "inventory_transaction", "quest_objective_progress", "quest_progress", "world_discovery", "world_unlock", "collection_entry",
            "pet_specialization", "pet_memory", "hearth_progress_event", "hearth_progress", "player_title", "world_scene_completion", "pet",
        )
    }
}
