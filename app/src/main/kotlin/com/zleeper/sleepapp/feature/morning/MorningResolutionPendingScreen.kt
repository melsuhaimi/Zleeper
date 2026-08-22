package com.zleeper.sleepapp.feature.morning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zleeper.sleepapp.ui.components.StorybookBackdrop

@Composable
fun MorningResolutionPendingScreen(
    sessionId: String,
    error: String?,
    onRetry: () -> Unit,
) {
    LaunchedEffect(sessionId) { onRetry() }
    StorybookBackdrop(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (error == null) {
                CircularProgressIndicator()
                Spacer(Modifier.height(18.dp))
                Text("Reading the saved night trail", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "The finalized sleep record is safe. Reward resolution is transactional and retrying does not reroll the expedition.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("The saved journey could not be resolved yet.", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Try resolving again") }
            }
        }
    }
}
