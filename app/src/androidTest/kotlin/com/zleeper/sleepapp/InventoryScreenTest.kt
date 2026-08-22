package com.zleeper.sleepapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.zleeper.sleepapp.feature.inventory.InventoryScreen
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.theme.ZleeperTheme
import org.junit.Rule
import org.junit.Test

class InventoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyInventoryExplainsWhereOwnedRewardsWillAppear() {
        composeRule.setContent {
            ZleeperTheme {
                InventoryScreen(ZleeperUiState())
            }
        }

        composeRule.onNodeWithText("INVENTORY").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing in this category yet").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Expedition rewards, waking-world finds, quest rewards and synthesis output appear here when they are actually owned.",
        ).assertIsDisplayed()
    }
}
