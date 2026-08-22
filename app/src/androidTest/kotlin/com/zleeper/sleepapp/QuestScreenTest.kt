package com.zleeper.sleepapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zleeper.sleepapp.data.content.ObjectiveDefinition
import com.zleeper.sleepapp.data.content.QuestDefinition
import com.zleeper.sleepapp.data.local.database.QuestProgressEntity
import com.zleeper.sleepapp.feature.quest.QuestLogScreen
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.ui.theme.ZleeperTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuestScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun availableQuestCanBeAcceptedFromQuestLog() {
        var acceptedQuestId: String? = null
        val quest = QuestDefinition(
            id = "quest_wind_down",
            name = "Lantern Routine",
            family = "BEHAVIOR",
            description = "Complete one wind-down.",
            objectives = listOf(ObjectiveDefinition("wind_down", "COMPLETE_WIND_DOWN", 1)),
            rewards = emptyList(),
        )
        val state = ZleeperUiState(
            quests = listOf(quest),
            questProgress = listOf(
                QuestProgressEntity(
                    questId = quest.id,
                    status = "AVAILABLE",
                    acceptedAtEpochMs = 0L,
                    completedAtEpochMs = null,
                    claimedAtEpochMs = null,
                ),
            ),
        )

        composeRule.setContent {
            ZleeperTheme {
                QuestLogScreen(
                    state = state,
                    onAccept = { acceptedQuestId = it },
                    onAbandon = {},
                    onTrack = { _, _ -> },
                    onClaim = {},
                )
            }
        }

        composeRule.onNodeWithText("Lantern Routine").assertIsDisplayed()
        composeRule.onNodeWithText("Accept quest").performClick()
        composeRule.runOnIdle { assertEquals("quest_wind_down", acceptedQuestId) }
    }
}
