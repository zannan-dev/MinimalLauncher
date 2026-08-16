package com.example.minimallauncher

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.ui.apps.AppDrawerScreen
import com.example.minimallauncher.ui.theme.LauncherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppDrawerScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun searchFiltersAppsAndTappingResultLaunchesIt() {
        val camera = LaunchableApp("camera", "CameraActivity", "Camera")
        val phone = LaunchableApp("phone", "PhoneActivity", "Phone")
        var launched: LaunchableApp? = null

        composeRule.setContent {
            LauncherTheme(preference = ThemePreference.LIGHT) {
                AppDrawerScreen(
                    apps = listOf(camera, phone),
                    favoriteKeys = emptySet(),
                    isLoading = false,
                    failedToLoad = false,
                    onBack = {},
                    onLaunchApp = { app -> launched = app },
                    onToggleFavorite = {},
                )
            }
        }

        composeRule.onNodeWithText("Search apps").performTextInput("cam")
        composeRule.onNodeWithText("Camera").assertIsDisplayed().performClick()

        assertEquals(camera, launched)
    }
}
