package com.zleeper.sleepapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.zleeper.sleepapp.data.repository.MorningJourneyStep
import com.zleeper.sleepapp.data.repository.MorningResult
import com.zleeper.sleepapp.feature.morning.MorningRevealScreen
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.theme.ZleeperTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MorningRevealTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun revealCanExpandSavedJourneyWithoutResolvingAgain() {
        var returnedToHearth = false
        val result = MorningResult(
            expeditionId = "expedition_1",
            regionId = "region_whispering_grove",
            reachBand = 3,
            xp = 72,
            rewards = listOf("item_material_dewdrop" to 2),
            pathNodeIds = listOf("node_start", "node_destination"),
            journey = listOf(
                MorningJourneyStep("node_destination", "DESTINATION", "Lumi found the lantern clearing."),
            ),
            previousLevel = 2,
            newLevel = 3,
            dreamSparksGained = 1,
            statChanges = emptyList(),
            newDiscoveries = listOf("node_destination"),
            hearthMemoriesGained = 1,
        )

        composeRule.setContent {
            ZleeperTheme {
                MorningRevealScreen(
                    state = ZleeperUiState(morningResult = result),
                    onReturnToHearth = { returnedToHearth = true },
                )
            }
        }

        composeRule.onNodeWithText("JOURNEY RESULT").assertIsDisplayed()
        composeRule.onNodeWithText("View journey").performScrollTo().performClick()
        composeRule.onNodeWithText("A reconstruction of the already-resolved expedition path.").assertIsDisplayed()
        composeRule.onNodeWithText("Lumi found the lantern clearing.").assertIsDisplayed()
        composeRule.onNodeWithText("Return to the Hearth").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(returnedToHearth) }
    }
}
