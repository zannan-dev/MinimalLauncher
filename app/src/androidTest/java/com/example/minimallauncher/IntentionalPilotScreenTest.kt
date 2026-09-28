package com.example.minimallauncher

import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.ui.apps.IntentionalPilotScreen
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class IntentionalPilotScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pauseScreenShowsPurposePromptWithoutBreathingExercise() {
        val app = LaunchableApp("camera", "CameraActivity", "Camera", Process.myUserHandle(), false)
        var launched = false

        composeRule.setContent {
            IntentionalPilotScreen(app, onLaunchApp = { launched = true }, onCancel = {})
        }

        composeRule.onNodeWithText("Do you need to open Camera?").assertIsDisplayed()
        composeRule.onNodeWithText("What is your purpose for opening it?").assertIsDisplayed()
        composeRule.onNodeWithText("Wait (3)").assertIsDisplayed()
        composeRule.onNodeWithText("Inhale").assertDoesNotExist()
        composeRule.onNodeWithText("Exhale").assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(3_100)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Open app").performClick()
        composeRule.runOnIdle { assertTrue(launched) }
    }
}
