package com.example.minimallauncher

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import android.os.Process
import android.os.SystemClock
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.Espresso
import org.junit.Assert.assertFalse
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
    fun filteringAndClearingSearchDoNotDismissKeyboard() {
        showSearchDrawer(autoOpenKeyboard = true)
        val search = composeRule.onNode(hasSetTextAction())
        val list = composeRule.onNode(hasScrollToIndexAction())
        waitForKeyboard(true)
        list.performTouchInput { swipeUp() }
        waitForKeyboard(false)
        list.performScrollToIndex(50)
        search.performClick()
        waitForKeyboard(true)
        search.performTextInput("App 59")
        composeRule.onNode(hasText("App 59") and !hasSetTextAction()).assertIsDisplayed()
        search.assertIsFocused()
        assertKeyboardStaysVisible()
        search.performTextClearance()
        search.performTextInput("No matching application")
        composeRule.waitUntil(5_000) {
            runCatching { composeRule.onNodeWithText("No matching apps").assertIsDisplayed() }.isSuccess
        }
        search.assertIsFocused()
        assertKeyboardStaysVisible()
        search.performTextClearance()
        composeRule.onNodeWithText("App 00").assertIsDisplayed()
        search.assertIsFocused()
        assertKeyboardStaysVisible()
    }

    @Test
    fun disablingAutoKeyboardKeepsTopUnfocusedUntilSearchIsTapped() {
        showSearchDrawer(autoOpenKeyboard = false)
        val search = composeRule.onNode(hasSetTextAction())
        val list = composeRule.onNode(hasScrollToIndexAction())
        search.assertIsNotFocused()
        search.performClick()
        waitForKeyboard(true)
        list.performTouchInput { swipeUp() }
        waitForKeyboard(false)
        list.performScrollToIndex(0)
        waitForKeyboardToSettle()
        search.assertIsNotFocused()
        assertFalse(keyboardVisible())
        search.performClick()
        search.assertIsFocused()
        waitForKeyboard(true)
    }

    @Test
    fun backDismissedKeyboardStaysHiddenUntilSearchIsTapped() {
        showSearchDrawer(autoOpenKeyboard = true)
        waitForKeyboard(true)
        Espresso.pressBack()
        waitForKeyboard(false)
        waitForKeyboardToSettle()
        assertFalse(keyboardVisible())
        composeRule.onNode(hasSetTextAction()).performClick()
        waitForKeyboard(true)
    }

    @Test
    fun returningFromSearchResultRestoresSearchWithAutoKeyboardEnabled() {
        verifyReturnFromSearchResult(autoOpenKeyboard = true)
    }

    @Test
    fun returningFromSearchResultRestoresManualSearchWithAutoKeyboardDisabled() {
        verifyReturnFromSearchResult(autoOpenKeyboard = false)
    }

    private fun verifyReturnFromSearchResult(autoOpenKeyboard: Boolean) {
        var launched: LaunchableApp? = null
        showSearchDrawer(autoOpenKeyboard) { launched = it }
        val search = composeRule.onNode(hasSetTextAction())
        search.performTextInput("App 59")
        waitForKeyboard(true)
        composeRule.onNode(hasText("App 59") and !hasSetTextAction()).performClick()
        assertEquals("app59", launched?.packageName)
        search.assertIsNotFocused()
        // Model the launcher losing its foreground activity while the result app is open.
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitForKeyboard(true)
        search.assertIsFocused()
        search.assertIsDisplayed()
        composeRule.onNode(hasText("App 59") and !hasSetTextAction()).assertIsDisplayed()
        assertKeyboardStaysVisible()
    }

    @Test
    fun returningFromUnfilteredAppDoesNotStartSearchWithAutoKeyboardDisabled() {
        showSearchDrawer(autoOpenKeyboard = false)
        composeRule.onNodeWithText("App 00").performClick()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitForKeyboardToSettle()
        composeRule.onNode(hasSetTextAction()).assertIsNotFocused()
        assertFalse(keyboardVisible())
    }

    private fun showSearchDrawer(autoOpenKeyboard: Boolean, onLaunchApp: (LaunchableApp) -> Unit = {}) {
        val apps = (0..59).map { index ->
            LaunchableApp("app$index", "Activity", "App %02d".format(index), Process.myUserHandle(), false)
        }
        composeRule.setContent {
            LauncherTheme(preference = ThemePreference.LIGHT) {
                AppDrawerScreen(
                    apps = apps,
                    listState = rememberLazyListState(),
                    autoOpenKeyboard = autoOpenKeyboard,
                    isLoading = false,
                    failedToLoad = false,
                    favoriteKeys = emptySet(),
                    onBack = {},
                    onLaunchApp = onLaunchApp,
                    onToggleFavorite = {},
                )
            }
        }
    }

    private fun keyboardVisible() = ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
        ?.isVisible(WindowInsetsCompat.Type.ime()) == true

    private fun waitForKeyboard(visible: Boolean) {
        composeRule.waitUntil(5_000) { keyboardVisible() == visible }
    }

    private fun waitForKeyboardToSettle() {
        val start = SystemClock.uptimeMillis()
        composeRule.waitUntil(3_000) { SystemClock.uptimeMillis() - start >= 1_500 }
    }

    private fun assertKeyboardStaysVisible() {
        waitForKeyboard(true)
        waitForKeyboardToSettle()
        org.junit.Assert.assertTrue(keyboardVisible())
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
