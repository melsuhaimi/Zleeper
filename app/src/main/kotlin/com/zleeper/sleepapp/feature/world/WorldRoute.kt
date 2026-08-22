package com.zleeper.sleepapp.feature.world

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zleeper.sleepapp.feature.shell.ZleeperUiState

@Composable
fun WorldRoute(
    state: ZleeperUiState,
    onOpenScene: (String) -> Unit,
) {
    var showingMap by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showingMap) { showingMap = false }

    if (showingMap) {
        RegionMapScreen(
            state = state,
            onBack = { showingMap = false },
            onOpenScene = onOpenScene,
        )
    } else {
        WorldHubScreen(
            state = state,
            onOpenRegionMap = { showingMap = true },
        )
    }
}
