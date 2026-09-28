package com.example.minimallauncher

import androidx.activity.ComponentActivity
import android.os.Process
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
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
        val user = Process.myUserHandle()
        val camera = LaunchableApp("camera", "CameraActivity", "Camera", user, false)
        val phone = LaunchableApp("phone", "PhoneActivity", "Phone", user, false)
        var launched: LaunchableApp? = null

        composeRule.setContent {
            LauncherTheme(preference = ThemePreference.LIGHT) {
                AppDrawerScreen(
                    apps = listOf(camera, phone),
                    listState = rememberLazyListState(),
                    autoOpenKeyboard = false,
                    isLoading = false,
                    failedToLoad = false,
                    favoriteKeys = emptySet(),
                    onBack = {},
                    onLaunchApp = { app -> launched = app },
                    onToggleFavorite = {},
                )
            }
        }

        composeRule.onNodeWithText("Search apps...").performTextInput("cam")
        composeRule.onNodeWithText("Camera").assertIsDisplayed().performClick()

        assertEquals(camera, launched)
    }

    @Test
    fun longPressOffersAddToFavorites() {
        val app = LaunchableApp("camera", "CameraActivity", "Camera", Process.myUserHandle(), false)
        var favorite: LaunchableApp? = null
        composeRule.setContent {
            LauncherTheme(preference = ThemePreference.LIGHT) {
                AppDrawerScreen(
                    apps = listOf(app),
                    listState = rememberLazyListState(),
                    autoOpenKeyboard = false,
                    isLoading = false,
                    failedToLoad = false,
                    favoriteKeys = emptySet(),
                    onBack = {},
                    onLaunchApp = {},
                    onToggleFavorite = { favorite = it },
                )
            }
        }

        composeRule.onNodeWithText("Camera").performTouchInput { longClick() }
        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        assertEquals(app, favorite)
    }
}
