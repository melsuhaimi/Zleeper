package com.zleeper.sleepapp.feature.morning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.components.StorybookBackdrop
import kotlin.math.roundToInt

@Composable
fun MorningReviewScreen(
    state: ZleeperUiState,
    onFinalize: (Long?, Long?, Int?, String) -> Unit,
) {
    val session = requireNotNull(state.pendingReview)
    val sessionEnd = requireNotNull(session.sessionEndEpochMs)
    val sessionMinutes = ((sessionEnd - session.sessionStartEpochMs) / 60_000L).toInt().coerceAtLeast(0)
    val estimatedStartOffset = (((session.estimatedSleepStartEpochMs ?: session.sessionStartEpochMs) - session.sessionStartEpochMs) / 60_000L)
        .toInt()
        .coerceIn(0, (sessionMinutes - 1).coerceAtLeast(0))
    val estimatedEndOffset = (((session.estimatedSleepEndEpochMs ?: sessionEnd) - session.sessionStartEpochMs) / 60_000L)
        .toInt()
        .coerceIn((estimatedStartOffset + 1).coerceAtMost(sessionMinutes), sessionMinutes)
    val savedNote = state.morningNotes.firstOrNull { it.sleepSessionId == session.id }

    var correcting by rememberSaveable(session.id) { mutableStateOf(false) }
    var startOffset by rememberSaveable(session.id) { mutableIntStateOf(estimatedStartOffset) }
    var endOffset by rememberSaveable(session.id) { mutableIntStateOf(estimatedEndOffset) }
    var mood by rememberSaveable(session.id) { mutableStateOf(savedNote?.mood) }
    var note by rememberSaveable(session.id) { mutableStateOf(savedNote?.note.orEmpty()) }

    StorybookBackdrop(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text("GOOD MORNING", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text("Confirm the estimate", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Zleeper estimated this night from the signals available to your phone. You can confirm it or correct the estimated sleep window before the saved expedition resolves.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Estimated sleep", style = MaterialTheme.typography.titleLarge)
                        Text(formatDuration(session.estimatedSleepMinutes ?: 0), style = MaterialTheme.typography.displaySmall)
                        Text(
                            "${session.confidence?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Unknown"} confidence · ${session.resolutionMethod?.lowercase()?.replace('_', ' ') ?: "manual estimate"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("This is an estimate, not medically verified sleep.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (sessionMinutes >= 2) {
                item {
                    if (correcting) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Correct the estimate", style = MaterialTheme.typography.titleLarge)
                                CorrectionSlider(
                                    label = "Fell asleep",
                                    valueLabel = "$startOffset min after Begin Sleep",
                                    value = startOffset.toFloat(),
                                    range = 0f..(sessionMinutes - 1).toFloat(),
                                ) {
                                    startOffset = it.roundToInt().coerceIn(0, endOffset - 1)
                                }
                                CorrectionSlider(
                                    label = "Woke",
                                    valueLabel = "${sessionMinutes - endOffset} min before session end",
                                    value = endOffset.toFloat(),
                                    range = 1f..sessionMinutes.toFloat(),
                                ) {
                                    endOffset = it.roundToInt().coerceIn(startOffset + 1, sessionMinutes)
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            correcting = !correcting
                            if (!correcting) {
                                startOffset = estimatedStartOffset
                                endOffset = estimatedEndOffset
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (correcting) "Use original estimate" else "Correct the estimate")
                    }
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Morning reflection", style = MaterialTheme.typography.titleLarge)
                        Text("Optional. A reflection can contribute the configured morning-reflection progression bonus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Rough", "Low", "Okay", "Good", "Rested").forEachIndexed { index, label ->
                                val value = index + 1
                                if (mood == value) {
                                    Button(onClick = { mood = null; note = "" }) { Text(label) }
                                } else {
                                    FilledTonalButton(onClick = { mood = value }) { Text(label) }
                                }
                            }
                        }
                        if (mood != null) {
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it.take(500) },
                                label = { Text("A note for later (optional)") },
                                supportingText = { Text("${note.length}/500") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            state.operationError?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        if (correcting) {
                            onFinalize(
                                session.sessionStartEpochMs + startOffset * 60_000L,
                                session.sessionStartEpochMs + endOffset * 60_000L,
                                mood,
                                note,
                            )
                        } else {
                            onFinalize(null, null, mood, note)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text(if (correcting) "Save correction and continue" else "Confirm and continue")
                }
            }
        }
    }
}

@Composable
private fun CorrectionSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(valueLabel, color = MaterialTheme.colorScheme.primary)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}

private fun formatDuration(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"
