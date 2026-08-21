package com.zleeper.sleepapp.platform.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.RoomDatabase
import com.zleeper.sleepapp.data.local.database.ZleeperDatabase
import com.zleeper.sleepapp.data.local.preferences.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DataControlRepository @Inject constructor(@ApplicationContext private val context: Context, private val database: ZleeperDatabase, private val settings: SettingsRepository) {
    suspend fun exportJson(): Uri = withContext(Dispatchers.IO) {
        val root = JSONObject().put("format", "zleeper_local_export_v1").put("exported_at_epoch_ms", System.currentTimeMillis())
        TABLES.forEach { table ->
            val rows = JSONArray()
            database.openHelper.readableDatabase.query("SELECT * FROM $table").use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    cursor.columnNames.forEachIndexed { index, name ->
                        val value: Any = when (cursor.getType(index)) { android.database.Cursor.FIELD_TYPE_NULL -> JSONObject.NULL; android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index); android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index); android.database.Cursor.FIELD_TYPE_BLOB -> android.util.Base64.encodeToString(cursor.getBlob(index), android.util.Base64.NO_WRAP); else -> cursor.getString(index) }
                        row.put(name, value)
                    }
                    rows.put(row)
                }
            }
            root.put(table, rows)
        }
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(directory, "zleeper-export-${System.currentTimeMillis()}.json").apply { writeText(root.toString(2)) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun deleteAllLocalData() { withContext(Dispatchers.IO) { database.clearAllTables() }; settings.reset() }

    private companion object {
        val TABLES = listOf("sleep_session", "sleep_signal", "night_outcome", "expedition", "expedition_path_node", "expedition_reward", "pet", "pet_progression_event", "inventory_stack", "inventory_instance", "inventory_transaction", "equipment_slot", "quest_progress", "quest_objective_progress", "world_unlock", "world_discovery", "collection_entry", "morning_note")
    }
}
