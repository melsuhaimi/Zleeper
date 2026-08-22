package com.zleeper.sleepapp.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.zleeper.sleepapp.data.local.preferences.MotionPreference
import com.zleeper.sleepapp.feature.journal.JournalScreen as RefinedJournalScreen
import com.zleeper.sleepapp.feature.shell.ZleeperViewModel
import com.zleeper.sleepapp.feature.world.WorldHubScreen
import com.zleeper.sleepapp.game.scene.PlatformScene

@Composable
fun AppNavigation(
    navigationState: NavigationState = remember { NavigationState() },
    viewModel: ZleeperViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    if (!state.settings.onboardingComplete || state.pet == null) {
        OnboardingFlow(state, viewModel)
        return
    }
    if (state.pendingReview != null) {
        MorningReview(state, viewModel)
        return
    }
    if (state.morningResult != null) {
        MorningReveal(state, viewModel)
        return
    }
    state.pendingResolution?.let { session ->
        MorningResolutionPending(session.id, state.operationError, viewModel::resumePendingResolution)
        return
    }
    state.pendingReveal?.let { expedition ->
        MorningResolutionPending(expedition.id, state.operationError, viewModel::resumePendingResolution)
        return
    }

    var worldSceneId by rememberSaveable { mutableStateOf<String?>(null) }
    state.scenes.firstOrNull { it.id == worldSceneId }?.let { scene ->
        DisposableEffect(scene.id) {
            viewModel.enterRegion(scene.regionId)
            viewModel.playSceneAudio(scene.regionId)
            onDispose { viewModel.stopSceneAudio() }
        }
        val petFormKey = state.pet
            ?.let { pet -> state.forms.firstOrNull { it.id == pet.formId }?.assetKey }
            ?: "glimmerling"
        val trackedQuestText = state.trackedQuest
            ?.let { progress -> state.quests.firstOrNull { it.id == progress.questId }?.name ?: progress.questId }
        PlatformScene(
            scene = scene,
            petFormKey = petFormKey,
            largeControls = state.settings.largeControls,
            leftHandedControls = state.settings.leftHandedControls,
            controlOpacity = state.settings.controlOpacity,
            reducedMotion = state.settings.motion == MotionPreference.REDUCED,
            hapticsEnabled = state.settings.hapticsEnabled,
            screenShakeEnabled = state.settings.screenShakeEnabled,
            coyoteBonusSeconds = state.gameEffects.coyoteTimeMillis / 1_000f,
            trackedQuestText = trackedQuestText,
            interaction = state.worldInteraction,
            collectedContentIds = state.collectionEntries.mapTo(mutableSetOf()) { it.entryId },
            modifier = Modifier.fillMaxSize(),
            onClose = { worldSceneId = null },
            onDismissInteraction = viewModel::dismissWorldInteraction,
            onInteract = { viewModel.interactWith(it, scene.id, scene.regionId) },
            onSceneCompleted = { viewModel.completeScene(scene.id) },
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { TopLevelNavigationBar(navigationState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).testTag("current-destination")) {
            val duration = if (state.settings.motion == MotionPreference.REDUCED) 0 else 260
            AnimatedContent(
                targetState = navigationState.currentRoute,
                transitionSpec = { fadeIn(tween(duration)) togetherWith fadeOut(tween(duration / 2)) },
                label = "top-level-destination",
            ) { route ->
                when (route) {
                    AppRoute.WORLD -> WorldHubScreen(state) { worldSceneId = it }
                    AppRoute.SLEEP -> SleepScreen(state, viewModel)
                    AppRoute.JOURNAL -> RefinedJournalScreen()
                    AppRoute.MENU -> MenuScreen(state, viewModel)
                }
            }
        }
    }
}

@Composable
fun TopLevelNavigationBar(navigationState: NavigationState) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        AppRoute.entries.forEach { route ->
            NavigationBarItem(
                selected = navigationState.currentRoute == route,
                onClick = { navigationState.select(route) },
                icon = { Icon(route.icon(), route.label) },
                modifier = Modifier.testTag("nav-${route.name.lowercase()}"),
                label = { Text(route.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

private fun AppRoute.icon(): ImageVector = when (this) {
    AppRoute.WORLD -> Icons.Outlined.Public
    AppRoute.SLEEP -> Icons.Outlined.Bedtime
    AppRoute.JOURNAL -> Icons.AutoMirrored.Outlined.MenuBook
    AppRoute.MENU -> Icons.Outlined.Menu
}
