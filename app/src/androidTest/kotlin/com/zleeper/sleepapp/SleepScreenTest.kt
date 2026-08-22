package com.zleeper.sleepapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.zleeper.sleepapp.data.local.database.SleepSessionEntity
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.feature.sleep.SleepScreen
import com.zleeper.sleepapp.ui.theme.ZleeperTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SleepScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun skippingWindDownRequiresExplicitBeginSleepAction() {
        var beginValue: Boolean? = null
        composeRule.setContent {
            ZleeperTheme {
                SleepScreen(
                    state = ZleeperUiState(),
                    onSavePlan = { _, _ -> },
                    onBeginSleep = { beginValue = it },
                    onWake = {},
                    onAbort = {},
                )
            }
        }

        composeRule.onNodeWithText("A reminder never starts sleep by itself. Tracking starts only after you explicitly choose Begin Sleep.").assertIsDisplayed()
        composeRule.onNodeWithText("Skip wind-down · Begin Sleep").performClick()
        composeRule.runOnIdle { assertEquals(false, beginValue) }
    }

    @Test
    fun trackingSessionShowsWakeAndAccidentalCancelActions() {
        var woke = false
        var aborted = false
        val tracking = SleepSessionEntity(
            id = "night",
            state = "TRACKING",
            sessionStartEpochMs = 1_000L,
            sessionEndEpochMs = null,
            estimatedSleepStartEpochMs = null,
            estimatedSleepEndEpochMs = null,
            targetSleepMinutes = 22 * 60,
            targetWakeMinutes = 6 * 60,
            estimatedSleepMinutes = null,
            timingOffsetMinutes = null,
            windDownCompleted = false,
            resolutionMethod = null,
            confidence = null,
            expeditionSeed = 42L,
            finalizedAtEpochMs = null,
        )

        composeRule.setContent {
            ZleeperTheme {
                SleepScreen(
                    state = ZleeperUiState(sessions = listOf(tracking)),
                    onSavePlan = { _, _ -> },
                    onBeginSleep = {},
                    onWake = { woke = true },
                    onAbort = { aborted = true },
                )
            }
        }

        composeRule.onNodeWithText("Sleep mode").assertIsDisplayed()
        composeRule.onNodeWithText("I'm awake").performClick()
        composeRule.runOnIdle { assertEquals(true, woke) }
        composeRule.onNodeWithText("Cancel accidental start").performClick()
        composeRule.runOnIdle { assertEquals(true, aborted) }
    }
}
