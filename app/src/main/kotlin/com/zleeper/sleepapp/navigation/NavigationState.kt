package com.zleeper.sleepapp.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class NavigationState(initialRoute: AppRoute = AppRoute.WORLD) {
    var currentRoute by mutableStateOf(initialRoute)
        private set

    fun select(route: AppRoute) {
        currentRoute = route
    }
}
