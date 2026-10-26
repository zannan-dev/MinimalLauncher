package com.example.minimallauncher

import com.example.minimallauncher.domain.RecentSearchResult
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
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
import com.example.minimallauncher.ui.apps.HomeSearchScreen
import com.example.minimallauncher.ui.theme.LauncherTheme
import com.example.minimallauncher.data.preferences.ThemePreference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UnifiedSearchTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun firstContactOnlyResultHasNoForcedSlide() {
        val gate = CompletableDeferred<Unit>()
        val search = object : DeviceSearchRepository {
            override fun settings(query: String) = emptyList<DeviceSearchResult>()
            override suspend fun contacts(query: String): List<DeviceSearchResult> {
                gate.await()
                return listOf(DeviceSearchResult("contact:first", "First contact", "Contact", "view"))
            }
            override suspend fun resolveRecent(result: RecentSearchResult): DeviceSearchResult? = null
            override fun open(result: DeviceSearchResult) = true
        }
        composeRule.setContent {
            LauncherTheme(ThemePreference.LIGHT) {
                HomeSearchScreen(
                    apps = emptyList(), listState = rememberLazyListState(), autoOpenKeyboard = false,
                    isLoading = false, failedToLoad = false, favoriteKeys = emptySet(),
                    onBack = {}, onLaunchApp = { true }, onToggleFavorite = {},
                    deviceSearch = search, contactsAllowed = true,
                )
            }
        }
        composeRule.onNode(hasSetTextAction()).performTextInput("lu")
        composeRule.onNodeWithText("First contact").assertDoesNotExist()
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.runOnIdle { gate.complete(Unit) }
            composeRule.waitUntil(5_000) {
                composeRule.mainClock.advanceTimeByFrame()
                composeRule.onAllNodes(androidx.compose.ui.test.hasText("First contact")).fetchSemanticsNodes().isNotEmpty()
            }
            val contact = composeRule.onNodeWithText("First contact")
            val start = contact.fetchSemanticsNode().boundsInRoot.top
            composeRule.mainClock.advanceTimeBy(64)
            composeRule.waitForIdle()
            val during = contact.fetchSemanticsNode().boundsInRoot.top
            composeRule.mainClock.advanceTimeBy(1_000)
            composeRule.waitForIdle()
            val end = contact.fetchSemanticsNode().boundsInRoot.top
            assertEquals("A single contact should not slide on screen", start, during, 1f)
            assertEquals("A single contact should stay at its final position", start, end, 1f)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test fun unchangedContactAndSettingStayMountedWhileRefiningASearch() {
        var gate = CompletableDeferred<Unit>().apply { complete(Unit) }
        var completedQuery = ""
        val alex = DeviceSearchResult("contact:alex", "Alex", "Contact", "view")
        val alarm = DeviceSearchResult("settings:alarm", "Alarm", "Phone settings", "settings")
        val search = object : DeviceSearchRepository {
            override fun settings(query: String) = if (query.startsWith("a")) listOf(alarm) else emptyList()
            override suspend fun contacts(query: String): List<DeviceSearchResult> {
                gate.await()
                completedQuery = query
                return if (query.startsWith("a")) listOf(alex) else emptyList()
            }
            override suspend fun resolveRecent(result: RecentSearchResult): DeviceSearchResult? = null
            override fun open(result: DeviceSearchResult) = true
        }
        composeRule.setContent {
            LauncherTheme(ThemePreference.LIGHT) {
                HomeSearchScreen(
                    apps = emptyList(), listState = rememberLazyListState(), autoOpenKeyboard = false,
                    isLoading = false, failedToLoad = false, favoriteKeys = emptySet(),
                    onBack = {}, onLaunchApp = { true }, onToggleFavorite = {},
                    deviceSearch = search, contactsAllowed = true,
                )
            }
        }
        composeRule.onNode(hasSetTextAction()).performTextInput("a")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Alex")).fetchSemanticsNodes().isNotEmpty()
        }
        val contact = composeRule.onNodeWithText("Alex")
        val setting = composeRule.onNodeWithText("Alarm")
        val contactTop = contact.fetchSemanticsNode().boundsInRoot.top
        val settingTop = setting.fetchSemanticsNode().boundsInRoot.top
        composeRule.runOnIdle { gate = CompletableDeferred() }
        composeRule.onNode(hasSetTextAction()).performTextReplacement("al")
        contact.assertIsDisplayed()
        setting.assertIsDisplayed()
        assertEquals(contactTop, contact.fetchSemanticsNode().boundsInRoot.top, 1f)
        assertEquals(settingTop, setting.fetchSemanticsNode().boundsInRoot.top, 1f)
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.runOnIdle { gate.complete(Unit) }
            composeRule.waitUntil(5_000) {
                composeRule.mainClock.advanceTimeByFrame()
                completedQuery == "al"
            }
            composeRule.mainClock.advanceTimeBy(64)
            composeRule.waitForIdle()
            assertEquals(contactTop, contact.fetchSemanticsNode().boundsInRoot.top, 1f)
            assertEquals(settingTop, setting.fetchSemanticsNode().boundsInRoot.top, 1f)
        } finally { composeRule.mainClock.autoAdvance = true }
        // A different query must hide old contacts immediately, without waiting for the provider.
        composeRule.runOnIdle { gate = CompletableDeferred() }
        composeRule.onNode(hasSetTextAction()).performTextReplacement("zz")
        contact.assertDoesNotExist()
        setting.assertDoesNotExist()
        gate.complete(Unit)
    }

    @Test fun newQueryAndDelayedContactsResetPartiallyScrolledResults() {
        val listState = LazyListState()
        var gate = CompletableDeferred<Unit>().apply { complete(Unit) }
        val search = object : DeviceSearchRepository {
            override fun settings(query: String) = if (query.isBlank()) emptyList() else
                (0..30).map { DeviceSearchResult("setting:$it", "Setting $it", "", "settings") }
            override suspend fun contacts(query: String): List<DeviceSearchResult> {
                gate.await()
                return listOf(DeviceSearchResult("contact:first", "First contact", "Contact", "view"))
            }
            override suspend fun resolveRecent(result: RecentSearchResult): DeviceSearchResult? = null
            override fun open(result: DeviceSearchResult) = true
        }
        composeRule.setContent {
            LauncherTheme(ThemePreference.LIGHT) {
                HomeSearchScreen(
                    apps = emptyList(), listState = listState, autoOpenKeyboard = false,
                    isLoading = false, failedToLoad = false, favoriteKeys = emptySet(),
                    onBack = {}, onLaunchApp = { true }, onToggleFavorite = {},
                    deviceSearch = search, contactsAllowed = true,
                )
            }
        }
        composeRule.onNode(hasSetTextAction()).performTextInput("l")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("First contact")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.runOnIdle {
            runBlocking { listState.scrollToItem(10, 24) }
            gate = CompletableDeferred()
        }
        composeRule.onNode(hasSetTextAction()).performTextReplacement("lu")
        composeRule.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(0, listState.firstVisibleItemScrollOffset)
        }
        gate.complete(Unit)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("First contact")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(0, listState.firstVisibleItemScrollOffset)
        }
        composeRule.onNodeWithText("Contacts").assertIsDisplayed()
        composeRule.onNodeWithText("First contact").assertIsDisplayed()
    }

    private class FakeSearch : DeviceSearchRepository {
        var contactQueries = 0
        var contactGate: CompletableDeferred<Unit>? = null
        var opened: DeviceSearchResult? = null
        var noContacts = false
        var failContacts = false
        override fun settings(query: String) = if (query == "cam")
            listOf(DeviceSearchResult("settings:camera", "Camera privacy", "Phone settings", "settings")) else emptyList()
        override suspend fun contacts(query: String): List<DeviceSearchResult> {
            contactQueries++
            contactGate?.await()
            if (failContacts) error("Contact lookup failed")
            if (noContacts) return emptyList()
            delay(if (query == "cam") 200 else 10)
            return listOf(DeviceSearchResult("contact:$query", if (query == "cam") "Camila" else "Other person", "Contact", "view"))
        }
        override suspend fun resolveRecent(result: RecentSearchResult): DeviceSearchResult? = null
        override fun open(result: DeviceSearchResult): Boolean { opened = result; return true }
    }

    private fun show(search: FakeSearch, allowed: androidx.compose.runtime.State<Boolean>, revision: androidx.compose.runtime.State<Int> = mutableStateOf(0), request: () -> Unit = {}) {
        composeRule.setContent {
            LauncherTheme(ThemePreference.LIGHT) {
                HomeSearchScreen(
                    apps = listOf(LaunchableApp("camera", "Activity", "Camera", Process.myUserHandle(), false)),
                    listState = rememberLazyListState(), autoOpenKeyboard = false,
                    isLoading = false, failedToLoad = false, favoriteKeys = emptySet(),
                    onBack = {}, onLaunchApp = { true }, onToggleFavorite = {},
                    deviceSearch = search, contactsAllowed = allowed.value, contactsRevision = revision.value, onRequestContacts = request,
                )
            }
        }
    }

    @Test fun delayedContactsAndSettingsAppearTogether() {
        val gate = CompletableDeferred<Unit>()
        val search = FakeSearch().apply { contactGate = gate }
        show(search, mutableStateOf(true))
        composeRule.onNode(hasSetTextAction()).performTextInput("cam")
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertDoesNotExist()
        composeRule.onNodeWithText("Camera privacy").assertDoesNotExist()
        composeRule.onNodeWithText("Camila").assertDoesNotExist()
        gate.complete(Unit)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Camila")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Camila").assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Camera privacy").assertIsDisplayed()
    }

    @Test fun settingsAppearWhenContactLookupReturnsNoMatches() {
        val search = FakeSearch().apply { noContacts = true }
        show(search, mutableStateOf(true))
        composeRule.onNode(hasSetTextAction()).performTextInput("cam")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Camera privacy")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Camera privacy").assertIsDisplayed()
        composeRule.onNodeWithText("Contacts").assertDoesNotExist()
        composeRule.onNodeWithText("No results").assertDoesNotExist()
    }

    @Test fun contactFailureDoesNotBlockSettingsResults() {
        val search = FakeSearch().apply { failContacts = true }
        show(search, mutableStateOf(true))
        composeRule.onNode(hasSetTextAction()).performTextInput("cam")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Contacts unavailable")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Camera privacy").assertIsDisplayed()
        composeRule.onNodeWithText("Contacts unavailable").assertIsDisplayed()
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

    @Test fun pendingContactsDoNotShowLoadingTextOrInterruptSearch() {
        val gate = CompletableDeferred<Unit>()
        val search = FakeSearch().apply { contactGate = gate }
        show(search, mutableStateOf(true))
        composeRule.onNode(hasSetTextAction()).performTextInput("cam")
        composeRule.onNodeWithText("Camera privacy").assertDoesNotExist()
        composeRule.onNodeWithText("Searching contacts…").assertDoesNotExist()
        composeRule.onNodeWithText("Contacts").assertDoesNotExist()
        composeRule.onNodeWithText("No results").assertDoesNotExist()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        gate.complete(Unit)
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(androidx.compose.ui.test.hasText("Camila")).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Phone settings").assertDoesNotExist()
        composeRule.onNodeWithText("Contact").assertDoesNotExist()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test fun refreshingSameQueryKeepsCompletedContactsAndFocus() {
        val search = FakeSearch()
        val revision = mutableStateOf(0)
        show(search, mutableStateOf(true), revision)
        composeRule.onNode(hasSetTextAction()).performTextInput("cam")
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(androidx.compose.ui.test.hasText("Camila")).fetchSemanticsNodes().isNotEmpty() }
        val gate = CompletableDeferred<Unit>()
        composeRule.runOnIdle {
            search.contactGate = gate
            revision.value++
        }
        composeRule.onNodeWithText("Camila").assertIsDisplayed()
        composeRule.onNodeWithText("Searching contacts…").assertDoesNotExist()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        gate.complete(Unit)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Camila").assertIsDisplayed()
    }

}
