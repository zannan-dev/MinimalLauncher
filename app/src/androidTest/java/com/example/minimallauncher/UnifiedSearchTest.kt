package com.example.minimallauncher

import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import com.example.minimallauncher.data.search.DeviceSearchRepository
import com.example.minimallauncher.domain.DeviceSearchResult
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.ui.apps.AppDrawerScreen
import com.example.minimallauncher.ui.theme.LauncherTheme
import com.example.minimallauncher.data.preferences.ThemePreference
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UnifiedSearchTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private class FakeSearch : DeviceSearchRepository {
        var contactQueries = 0
        var opened: DeviceSearchResult? = null
        override fun settings(query: String) = if (query == "cam")
            listOf(DeviceSearchResult("settings:camera", "Camera privacy", "Phone settings", "settings")) else emptyList()
        override suspend fun contacts(query: String): List<DeviceSearchResult> {
            contactQueries++
            delay(if (query == "cam") 200 else 10)
            return listOf(DeviceSearchResult("contact:$query", if (query == "cam") "Camila" else "Other person", "Contact", "view"))
        }
        override fun open(result: DeviceSearchResult): Boolean { opened = result; return true }
    }

    private fun show(search: FakeSearch, allowed: androidx.compose.runtime.State<Boolean>, request: () -> Unit = {}) {
        composeRule.setContent {
            LauncherTheme(ThemePreference.LIGHT) {
                AppDrawerScreen(
                    apps = listOf(LaunchableApp("camera", "Activity", "Camera", Process.myUserHandle(), false)),
                    listState = rememberLazyListState(), autoOpenKeyboard = false,
                    isLoading = false, failedToLoad = false, favoriteKeys = emptySet(),
                    onBack = {}, onLaunchApp = {}, onToggleFavorite = {},
                    deviceSearch = search, contactsAllowed = allowed.value, onRequestContacts = request,
                )
            }
        }
    }

    @Test fun mixedResultsOpenContactAndRestoreSearchOnReturn() {
        val search = FakeSearch()
        show(search, mutableStateOf(true))
        composeRule.onNode(hasSetTextAction()).performTextInput("cam")
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(androidx.compose.ui.test.hasText("Camila")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("Camera privacy").assertIsDisplayed()
        composeRule.onNodeWithText("Camila").performClick()
        assertEquals("contact:cam", search.opened?.key)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test fun contactsAreQueriedOnlyAfterOptInAndSettingsRemainUsable() {
        val search = FakeSearch()
        val allowed = mutableStateOf(false)
        show(search, allowed) { allowed.value = true }
        composeRule.onNode(hasSetTextAction()).performTextInput("cam")
        composeRule.onNodeWithText("Camera privacy").performClick()
        assertEquals("settings:camera", search.opened?.key)
        assertEquals(0, search.contactQueries)
        composeRule.onNodeWithText("Search contacts").performClick()
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(androidx.compose.ui.test.hasText("Camila")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Camila").assertIsDisplayed()
    }

    @Test fun changingQueryDoesNotShowPreviousContactMatches() {
        val search = FakeSearch()
        show(search, mutableStateOf(true))
        assertEquals(0, search.contactQueries)
        composeRule.onNode(hasSetTextAction()).performTextInput("cam")
        composeRule.onNode(hasSetTextAction()).performTextReplacement("other")
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(androidx.compose.ui.test.hasText("Other person")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Camila").assertDoesNotExist()
        composeRule.onNodeWithText("Camera privacy").assertDoesNotExist()
    }
}
