package com.zleeper.sleepapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.zleeper.sleepapp.data.content.RegionDefinition
import com.zleeper.sleepapp.data.local.database.PetEntity
import com.zleeper.sleepapp.feature.shell.ZleeperUiState
import com.zleeper.sleepapp.feature.world.RegionMapScreen
import com.zleeper.sleepapp.ui.theme.ZleeperTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorldScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unlockedRegionOpensItsFirstAuthoredScene() {
        var openedScene: String? = null
        var backedOut = false
        val pet = PetEntity(
            instanceId = "pet",
            speciesId = "species_moonmoth",
            displayName = "Lumi",
            formId = "form_glimmerling",
            level = 5,
            totalXp = 0,
            energy = 3,
            focus = 3,
            resilience = 3,
            energyAffinity = 0,
            focusAffinity = 0,
            resilienceAffinity = 0,
            createdAtEpochMs = 0L,
            updatedAtEpochMs = 0L,
        )
        val region = RegionDefinition(
            id = "region_whispering_grove",
            name = "Whispering Grove",
            description = "A lantern-lit trail.",
            unlockLevel = 1,
            scenes = listOf("scene_whispering_grove_entry"),
            npcs = emptyList(),
            expeditionStart = "node_whispering_start",
            palette = emptyList(),
            musicAsset = "audio/music/mus_whispering_grove_explore_loop.ogg",
            ambienceAsset = "audio/ambience/amb_whispering_grove_night_loop.ogg",
        )

        composeRule.setContent {
            ZleeperTheme {
                RegionMapScreen(
                    state = ZleeperUiState(pet = pet, regions = listOf(region)),
                    onBack = { backedOut = true },
                    onOpenScene = { openedScene = it },
                )
            }
        }

        composeRule.onNodeWithText("REGION MAP").assertIsDisplayed()
        composeRule.onNodeWithTag("region-map").performScrollToNode(hasText("Enter Whispering Grove"))
        composeRule.onNodeWithText("Enter Whispering Grove").performClick()
        composeRule.runOnIdle { assertEquals("scene_whispering_grove_entry", openedScene) }
        composeRule.onNodeWithTag("region-map").performScrollToNode(hasText("REGION MAP"))
        composeRule.onNodeWithContentDescription("Back to Hearth").performClick()
        composeRule.runOnIdle { assertTrue(backedOut) }
    }
}
