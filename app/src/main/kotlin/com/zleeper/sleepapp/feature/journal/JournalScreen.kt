package com.zleeper.sleepapp.feature.journal

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

private enum class JournalDestination { OVERVIEW, TRENDS, DETAIL, REPLAY }

@Composable
fun JournalScreen(viewModel: JournalViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var destinationName by rememberSaveable { mutableStateOf(JournalDestination.OVERVIEW.name) }
    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.nights.firstOrNull { it.sessionId == selectedSessionId }
    val destination = runCatching { JournalDestination.valueOf(destinationName) }.getOrDefault(JournalDestination.OVERVIEW)

    when {
        destination == JournalDestination.REPLAY && selected != null -> JourneyReplayScreen(
            night = selected,
            onBack = { destinationName = JournalDestination.DETAIL.name },
        )
        destination == JournalDestination.DETAIL && selected != null -> SleepSessionDetailScreen(
            night = selected,
            onBack = { destinationName = JournalDestination.OVERVIEW.name; selectedSessionId = null },
            onViewJourney = { destinationName = JournalDestination.REPLAY.name },
        )
        destination == JournalDestination.TRENDS -> TrendsScreen(
            state = state,
            onBack = { destinationName = JournalDestination.OVERVIEW.name },
        )
        else -> JournalOverview(
            state = state,
            onOpenNight = { id -> selectedSessionId = id; destinationName = JournalDestination.DETAIL.name },
            onOpenTrends = { destinationName = JournalDestination.TRENDS.name },
        )
    }
}

@Composable
private fun JournalOverview(
    state: JournalUiState,
    onOpenNight: (String) -> Unit,
    onOpenTrends: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("journal-overview"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Sleep journal", style = MaterialTheme.typography.headlineMedium)
            Text("Patterns from finalized nights, without judgment.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.nights.isEmpty()) {
            item {
                JournalCard {
                    Text("Your first page is waiting", style = MaterialTheme.typography.titleLarge)
                    Text("A finalized night will appear here. Raw Sleep API signals are never used as historical Journal data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            state.trends?.let { trends ->
                item {
                    TrendCard(trends)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onOpenTrends, modifier = Modifier.fillMaxWidth().testTag("open-journal-trends")) {
                        Text("Open trends")
                    }
                }
            }
            if (state.trends == null) {
                item { Text("${3 - state.nights.size} more finalized ${if (3 - state.nights.size == 1) "night" else "nights"} before the first pattern summary.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.nights, key = { it.sessionId }) { night ->
                Surface(
                    onClick = { onOpenNight(night.sessionId) },
                    modifier = Modifier.fillMaxWidth().testTag("journal-night-${night.sessionId}"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDate(night.startedAtEpochMs), fontWeight = FontWeight.Bold)
                            Text(formatDuration(night.estimatedSleepMinutes), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                        Text("${humanize(night.confidence)} confidence · ${humanize(night.resolutionMethod)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (night.xpGranted != null) Text("${night.xpGranted} XP · Reach ${night.reachBand ?: 0}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendsScreen(state: JournalUiState, onBack: () -> Unit) {
    val trends = state.trends
    LazyColumn(
        Modifier.fillMaxSize().testTag("journal-trends"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { OutlinedButton(onClick = onBack) { Text("Back to journal") } }
        item {
            Text("Trends", style = MaterialTheme.typography.headlineMedium)
            Text("Behavior patterns from finalized estimates. These are not a medical sleep score.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trends == null) {
            item {
                JournalCard {
                    Text("Not enough finalized nights yet", style = MaterialTheme.typography.titleLarge)
                    Text("Zleeper waits for at least three nights before describing a pattern so one estimate is not treated as a conclusion.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            item {
                JournalCard {
                    Metric("Finalized nights sampled", trends.sampleSize.toString())
                    Metric("Average estimated duration", formatDuration(trends.averageDurationMinutes))
                    Metric("Duration range", "${trends.durationRangeMinutes} min")
                    Metric("Average distance from target", "${trends.averageTimingDistanceMinutes} min")
                }
            }
            item {
                val description = "Estimated sleep duration across ${trends.sampleSize} recent finalized nights"
                JournalCard {
                    Text("Recent estimated duration", style = MaterialTheme.typography.titleLarge)
                    TrendGraph(trends.durationSeries, description)
                }
            }
            item {
                Text(
                    "Use these patterns as descriptive context for your routine. Confidence and corrections remain visible in each nightly detail.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SleepSessionDetailScreen(night: JournalNight, onBack: () -> Unit, onViewJourney: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("sleep-session-detail"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { OutlinedButton(onClick = onBack) { Text("Back to journal") } }
        item {
            Text(formatDate(night.startedAtEpochMs), style = MaterialTheme.typography.headlineMedium)
            Text("Resolved phone-based estimate", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            JournalCard {
                Metric("Estimated duration", formatDuration(night.estimatedSleepMinutes))
                Metric("Timing distance", "${abs(night.timingOffsetMinutes)} min")
                Metric("Confidence", humanize(night.confidence))
                Metric("Method", humanize(night.resolutionMethod))
                Metric("Wind-down", if (night.windDownCompleted) "Completed" else "Not recorded")
            }
        }
        if (night.xpGranted != null || night.reachBand != null) {
            item {
                JournalCard {
                    Text("Night outcome", style = MaterialTheme.typography.titleLarge)
                    night.xpGranted?.let { Metric("Progress", "$it XP") }
                    night.reachBand?.let { Metric("Expedition reach", "Band $it") }
                    night.regionId?.let { Metric("Region", humanize(it.removePrefix("region_"))) }
                }
            }
        }
        night.morningMood?.let { mood ->
            item {
                JournalCard {
                    Text("Morning reflection", style = MaterialTheme.typography.titleLarge)
                    Text(moodLabel(mood), color = MaterialTheme.colorScheme.primary)
                    night.morningNote?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        if (night.journey.isNotEmpty()) {
            item { Button(onClick = onViewJourney, modifier = Modifier.fillMaxWidth().testTag("view-journey-replay")) { Text("View saved journey") } }
        }
    }
}

@Composable
private fun JourneyReplayScreen(night: JournalNight, onBack: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("journey-replay"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { OutlinedButton(onClick = onBack) { Text("Back to night") } }
        item {
            Text("Journey replay", style = MaterialTheme.typography.headlineMedium)
            Text("This reads the saved expedition path and reward ledger. It does not reroll or grant rewards again.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(night.journey, key = { it.sequence }) { step ->
            JournalCard {
                Text("${step.sequence + 1} · ${humanize(step.nodeType)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(step.narrative, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (night.rewards.isNotEmpty()) {
            item { Text("Returned with", style = MaterialTheme.typography.titleLarge) }
            items(night.rewards) { reward ->
                JournalCard { Text("${reward.quantity} × ${reward.contentId?.let(::humanize) ?: humanize(reward.rewardType)}", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun TrendCard(trends: JournalTrends) {
    val durationSummary = "Recent duration average ${formatDuration(trends.averageDurationMinutes)}, range ${trends.durationRangeMinutes} minutes across ${trends.sampleSize} nights"
    val timingSummary = "Average distance from target bedtime ${trends.averageTimingDistanceMinutes} minutes"
    JournalCard {
        Text("Recent rhythms", style = MaterialTheme.typography.titleLarge)
        Text(durationSummary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(timingSummary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        TrendGraph(trends.durationSeries, "$durationSummary. $timingSummary")
    }
}

@Composable
private fun TrendGraph(values: List<Int>, description: String) {
    val lineColor = MaterialTheme.colorScheme.primary
    val guideColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        Modifier.fillMaxWidth().height(110.dp).semantics { contentDescription = description },
    ) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: return@Canvas
        val max = values.maxOrNull() ?: return@Canvas
        val range = (max - min).coerceAtLeast(1)
        drawLine(guideColor, Offset(0f, size.height), Offset(size.width, size.height))
        val points = values.mapIndexed { index, value ->
            Offset(
                x = index * (size.width / (values.size - 1)),
                y = size.height - ((value - min).toFloat() / range) * size.height,
            )
        }
        points.zipWithNext().forEach { (a, b) -> drawLine(lineColor, a, b, strokeWidth = 5f) }
        points.forEach { point -> drawCircle(lineColor, radius = 6f, center = point) }
    }
}

@Composable
private fun JournalCard(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatDate(epochMs: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs))
private fun formatDuration(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"
private fun humanize(value: String): String = value.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
private fun moodLabel(value: Int): String = listOf("Rough", "Low", "Okay", "Good", "Rested").getOrElse(value - 1) { "Recorded" }
