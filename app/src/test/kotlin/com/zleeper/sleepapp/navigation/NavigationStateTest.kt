package com.zleeper.sleepapp.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationStateTest {
    @Test
    fun startsOnWorld() {
        assertEquals(AppRoute.WORLD, NavigationState().currentRoute)
    }

    @Test
    fun selectChangesCurrentRoute() {
        val state = NavigationState()

        state.select(AppRoute.SLEEP)

        assertEquals(AppRoute.SLEEP, state.currentRoute)
    }
}
