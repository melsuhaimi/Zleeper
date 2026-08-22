package com.zleeper.sleepapp.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.feature.shell.ZleeperUiState

private enum class DataAction(val label: String, val warning: String) {
    SLEEP_HISTORY(
        "Delete sleep history",
        "Permanently removes sleep sessions, temporary signals, morning notes, night outcomes, expeditions, saved paths, and expedition rewards. Game progress is kept.",
    ),
    GAME_PROGRESS(
        "Reset game progress",
        "Permanently removes the companion, equipment, inventory, quests, discoveries, Hearth progress, titles, memories, and scene history. Sleep history is kept.",
    ),
    ALL_DATA(
        "Delete all local data",
        "Permanently removes settings, sleep history, and all game progress from this device. Zleeper returns to first-run onboarding.",
    ),
}

@Composable
fun DataManagementScreen(
    state: ZleeperUiState,
    onExport: ((Uri) -> Unit) -> Unit,
    onDeleteSleepHistory: () -> Unit,
    onResetGameProgress: () -> Unit,
    onDeleteAllData: () -> Unit,
) {
    val context = LocalContext.current
    var pendingAction by rememberSaveable { mutableStateOf<DataAction?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("DATA MANAGEMENT", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Text("Local-first and under your control", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Zleeper does not require an account or cloud progression. Resolved sleep history remains on this device until you delete it.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Export", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Create a readable JSON archive containing current settings and the persisted local Room records.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = {
                            onExport { uri ->
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(share, "Export Zleeper data"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Export data")
                    }
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Delete or reset", style = MaterialTheme.typography.titleLarge)
                    Text("Each action requires a second explicit confirmation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { pendingAction = DataAction.SLEEP_HISTORY }, modifier = Modifier.fillMaxWidth()) {
                        Text(DataAction.SLEEP_HISTORY.label)
                    }
                    OutlinedButton(onClick = { pendingAction = DataAction.GAME_PROGRESS }, modifier = Modifier.fillMaxWidth()) {
                        Text(DataAction.GAME_PROGRESS.label)
                    }
                    OutlinedButton(onClick = { pendingAction = DataAction.ALL_DATA }, modifier = Modifier.fillMaxWidth()) {
                        Text(DataAction.ALL_DATA.label)
                    }
                }
            }
        }
        pendingAction?.let { action ->
            item {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Confirm permanent action", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(action.warning, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(
                            onClick = {
                                when (action) {
                                    DataAction.SLEEP_HISTORY -> onDeleteSleepHistory()
                                    DataAction.GAME_PROGRESS -> onResetGameProgress()
                                    DataAction.ALL_DATA -> onDeleteAllData()
                                }
                                pendingAction = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text("Confirm ${action.label.lowercase()}")
                        }
                        OutlinedButton(onClick = { pendingAction = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
        state.operationError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        item {
            Text(
                "Raw normalized sleep signals are temporary; resolved sessions are the long-term Journal record until you delete them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
