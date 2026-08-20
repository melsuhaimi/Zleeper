package com.zleeper.sleepapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun AppNavigation(
    navigationState: NavigationState = remember { NavigationState() },
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                AppRoute.entries.forEach { route ->
                    NavigationBarItem(
                        selected = navigationState.currentRoute == route,
                        onClick = { navigationState.select(route) },
                        icon = {
                            Icon(
                                imageVector = when (route) {
                                    AppRoute.WORLD -> Icons.Outlined.Public
                                    AppRoute.SLEEP -> Icons.Outlined.Bedtime
                                    AppRoute.JOURNAL -> Icons.Outlined.MenuBook
                                    AppRoute.MENU -> Icons.Outlined.Menu
                                },
                                contentDescription = route.label,
                            )
                        },
                        label = { Text(route.label) },
                    )
                }
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = navigationState.currentRoute.label,
                modifier = Modifier.testTag("current-destination"),
            )
        }
    }
}
