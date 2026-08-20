package com.zleeper.sleepapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.zleeper.sleepapp.app.App
import com.zleeper.sleepapp.ui.theme.ZleeperTheme
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun topLevelNavigationChangesVisibleDestination() {
        composeRule.setContent {
            ZleeperTheme { App() }
        }

        composeRule.onNodeWithContentDescription("Sleep").performClick()
        composeRule.onNodeWithTag("current-destination")
            .assertIsDisplayed()
            .assertTextEquals("Sleep")
    }
}
