package com.example.minimallauncher

import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.ui.assistant.HomeAssistant
import com.example.minimallauncher.ui.home.HomeScreen
import org.junit.Rule
import org.junit.Test

class InlineAssistantTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val homeRequest = mutableStateOf(0L)
    private val active = mutableStateOf(true)
    private val expanded = mutableStateOf(false)
    private fun show(favoriteCount: Int = 1) {
        composeRule.setContent {
            HomeScreen(favoriteApps = List(favoriteCount) { index ->
                LaunchableApp("camera$index", "Activity", if (index == 0) "Camera" else "App $index", Process.myUserHandle(), false)
            },
                showDate = true, doubleTapToLock = false, assistantExpansion = expanded, onOpenSearch = {}, onOpenSettings = {},
                onSetDefaultLauncher = {}, onLaunchApp = { error("Outside tap launched a hidden favourite") }, onMoveFavorite = { _, _ -> }, onRemoveFavorite = {},
                assistantContent = { expansion ->
                    HomeAssistant(apps = emptyList(), deviceSearch = null, onLaunchApp = {},
                        isActive = active.value, homeRequest = homeRequest.value, expansionState = expansion)
                })
        }
    }
    private fun open() {
        show()
        composeRule.runOnIdle { expanded.value = true }
        composeRule.onNodeWithTag("Assistant status").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close assistant").assertDoesNotExist()
        // The home context stays in the same composition and window; favourites yield their space.
        composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Favorite Camera").assertDoesNotExist()
    }
    private fun assertRestored() {
        composeRule.onNodeWithContentDescription("Close assistant").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Voice assistant").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Favorite Camera").assertIsDisplayed()
    }
    @Test fun outsideTapRestoresHomeFavorites() {
        open()
        composeRule.onNodeWithContentDescription("Current time").performTouchInput { down(center); up() }
        assertRestored()
    }
    private fun tapHiddenFavorite(favoriteCount: Int) {
        show(favoriteCount)
        val favoriteCenter = composeRule.onNodeWithContentDescription("Favorite Camera")
            .fetchSemanticsNode().boundsInRoot.center
        composeRule.runOnIdle { expanded.value = true }
        composeRule.onNodeWithTag("Assistant status").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { down(favoriteCenter); up() }
        assertRestored()
    }
    @Test fun tapOverHiddenFavoriteDismissesAssistant() {
        tapHiddenFavorite(1)
    }
    @Test fun tapOverScrollableHiddenFavoritesDismissesAssistant() {
        tapHiddenFavorite(30)
    }
    @Test fun backClosesInlineAssistantBeforeLeavingHome() {
        open()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        assertRestored()
    }
    @Test fun tappingAssistantResultKeepsAssistantOpen() {
        open()
        composeRule.onNodeWithTag("Assistant status").performTouchInput { down(center); up() }
        composeRule.onNodeWithTag("Assistant status").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Favorite Camera").assertDoesNotExist()
    }
    @Test fun homeRequestDismissesAssistant() {
        open()
        composeRule.runOnIdle { homeRequest.value++ }
        assertRestored()
    }
    @Test fun movingAwayFromHomeDismissesAssistant() {
        open()
        composeRule.runOnIdle { active.value = false }
        assertRestored()
    }
}
