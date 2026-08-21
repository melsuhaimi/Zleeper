package com.zleeper.sleepapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.zleeper.sleepapp.navigation.NavigationState
import com.zleeper.sleepapp.navigation.TopLevelNavigationBar
import com.zleeper.sleepapp.ui.theme.ZleeperTheme
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun topLevelNavigationChangesVisibleDestination() {
        composeRule.setContent {
            ZleeperTheme {
                val state = remember { NavigationState() }
                androidx.compose.foundation.layout.Column {
                    Text(state.currentRoute.label, Modifier.testTag("current-destination"))
                    TopLevelNavigationBar(state)
                }
            }
        }

        composeRule.onNodeWithContentDescription("Sleep").performClick()
        composeRule.onNodeWithTag("current-destination")
            .assertIsDisplayed()
            .assertTextEquals("Sleep")
    }
}
