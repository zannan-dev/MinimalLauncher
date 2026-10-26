package com.example.minimallauncher

import android.os.Process
import androidx.lifecycle.Lifecycle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.data.search.DeviceSearchRepository
import com.example.minimallauncher.domain.*
import com.example.minimallauncher.ui.apps.HomeSearchScreen
import com.example.minimallauncher.ui.theme.LauncherTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RecentSearchResultsTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val camera = LaunchableApp("camera", "Activity", "Camera", Process.myUserHandle(), false)
    private val calculator = LaunchableApp("calculator", "Activity", "Calculator", Process.myUserHandle(), false)
    private val contactUri = "content://com.android.contacts/contacts/lookup/alex/1"
    private val contact = DeviceSearchResult("contact:$contactUri", "Alex Updated", "+123456789", "view", contactUri)
    private val wifi = DeviceSearchResult("settings:wifi", "Wi-Fi", "Phone settings", "wifi")

    private inner class Harness(initial: List<RecentSearchResult> = emptyList()) : DeviceSearchRepository {
        var history by mutableStateOf(initial)
        var allowed by mutableStateOf(true)
        var allApps by mutableStateOf(false)
        var active by mutableStateOf(true)
        var homeRequest by mutableStateOf(0L)
        var apps by mutableStateOf(listOf(camera, calculator))
        var appLaunchSucceeds = true
        var deviceLaunchSucceeds = true
        val launchedApps = mutableListOf<LaunchableApp>()
        val openedDevices = mutableListOf<DeviceSearchResult>()
        val recorded = mutableListOf<RecentSearchResult>()
        val resolved = mutableListOf<RecentSearchResult>()
        var resolutionGate: CompletableDeferred<Unit>? = null
        override fun settings(query: String) = if (query == "wifi") listOf(wifi) else emptyList()
        override suspend fun contacts(query: String) = if (query == "alex") listOf(contact) else emptyList()
        override suspend fun resolveRecent(result: RecentSearchResult): DeviceSearchResult? {
            resolved += result
            resolutionGate?.await()
            return when (result) {
                is RecentSearchResult.App -> null
                is RecentSearchResult.Contact -> contact.takeIf { it.uri == result.lookupUri }
                is RecentSearchResult.Setting -> wifi.takeIf { it.key == result.key }
            }
        }
        override fun open(result: DeviceSearchResult): Boolean {
            openedDevices += result
            return deviceLaunchSucceeds
        }
    }

    private fun show(harness: Harness) {
        composeRule.setContent {
            LauncherTheme(ThemePreference.LIGHT) {
                HomeSearchScreen(
                    apps = harness.apps, listState = rememberLazyListState(), autoOpenKeyboard = false,
                    isLoading = false, failedToLoad = false, favoriteKeys = emptySet(),
                    onBack = {}, onToggleFavorite = {},
                    onLaunchApp = { harness.launchedApps += it; harness.appLaunchSucceeds },
                    deviceSearch = harness, contactsAllowed = harness.allowed,
                    animateFromHome = true, homeSearchProgress = 1f,
                    showAllApps = harness.allApps, onShowAllApps = { harness.allApps = true },
                    isActive = harness.active, homeRequest = harness.homeRequest, recentResults = harness.history,
                    onRecordRecentResult = { harness.recorded += it; harness.history = harness.history.withRecentResult(it) },
                    onClearRecentResults = { harness.history = emptyList() },
                )
            }
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(5_000) { composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
    private fun query(text: String) = composeRule.onNodeWithTag("Home search").performTextReplacement(text)

    private fun returnFromRecentAndCheckKeyboard() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitUntil(5_000) {
            composeRule.onNodeWithTag("Home search").fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.Focused]
        }
        composeRule.onNodeWithTag("Home search").assertIsFocused()
        composeRule.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
    }

    @Test fun returningFromRecentAppRestoresKeyboardWithAnEmptyQuery() {
        val harness = Harness(listOf(RecentSearchResult.App(camera.key)))
        show(harness)
        composeRule.onNodeWithText("Camera").performClick()
        composeRule.onNodeWithTag("Home search").assertIsNotFocused()
        returnFromRecentAndCheckKeyboard()
        composeRule.onNodeWithText("Recent").assertIsDisplayed()
        assertEquals("", composeRule.onNodeWithTag("Home search").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
    }

    @Test fun returningFromKeyboardLaunchedRecentRestoresKeyboard() {
        val harness = Harness(listOf(RecentSearchResult.App(camera.key)))
        show(harness)
        composeRule.onNodeWithTag("Home search").performClick().performImeAction()
        returnFromRecentAndCheckKeyboard()
    }

    @Test fun returningFromRecentContactsAndSettingsRestoresKeyboard() {
        val harness = Harness(listOf(contact.asRecentResult(), wifi.asRecentResult()))
        show(harness)
        waitForText("Alex Updated")
        composeRule.onNodeWithText("Alex Updated").performClick()
        returnFromRecentAndCheckKeyboard()
        waitForText("Wi-Fi")
        composeRule.onNodeWithText("Wi-Fi").performClick()
        returnFromRecentAndCheckKeyboard()
    }

    @Test fun homeRequestCancelsKeyboardRestoreAfterARecentLaunch() {
        val harness = Harness(listOf(RecentSearchResult.App(camera.key)))
        show(harness)
        composeRule.onNodeWithText("Camera").performClick()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        harness.homeRequest++
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.onNodeWithTag("Home search").assertIsNotFocused()
    }

    @Test fun keyboardGoOpensTheArrowMarkedLatestAppAndFollowsReordering() {
        val harness = Harness(listOf(RecentSearchResult.App(camera.key), RecentSearchResult.App(calculator.key)))
        show(harness)
        composeRule.onAllNodesWithContentDescription("Opens with keyboard Go").assertCountEquals(1)
        composeRule.onNodeWithText("Camera").assertContentDescriptionEquals("Opens with keyboard Go")
        composeRule.onNodeWithTag("Home search").performImeAction()
        composeRule.runOnIdle { assertEquals(listOf(camera), harness.launchedApps) }
        composeRule.onNodeWithText("Calculator").performClick()
        composeRule.onNodeWithText("Calculator").assertContentDescriptionEquals("Opens with keyboard Go")
        composeRule.onNodeWithTag("Home search").performImeAction()
        composeRule.runOnIdle {
            assertEquals(listOf(camera, calculator, calculator), harness.launchedApps)
            assertEquals(2, harness.history.size)
        }
    }

    @Test fun keyboardGoOpensRecentContactsAndSettings() {
        val harness = Harness(listOf(contact.asRecentResult(), RecentSearchResult.App(camera.key)))
        show(harness)
        waitForText("Alex Updated")
        composeRule.onNodeWithText("Alex Updated").assertContentDescriptionEquals("Opens with keyboard Go")
        composeRule.onNodeWithTag("Home search").performImeAction()
        composeRule.runOnIdle {
            assertEquals(contact, harness.openedDevices.last())
            harness.history = listOf(wifi.asRecentResult(), contact.asRecentResult())
        }
        waitForText("Wi-Fi")
        composeRule.onNodeWithTag("Home search").performImeAction()
        composeRule.runOnIdle { assertEquals(wifi, harness.openedDevices.last()) }
    }

    @Test fun typedSearchUsesMatchingResultInsteadOfRecentKeyboardShortcut() {
        val harness = Harness(listOf(RecentSearchResult.App(camera.key)))
        show(harness)
        query("calc")
        composeRule.onNodeWithContentDescription("Opens with keyboard Go").assertDoesNotExist()
        composeRule.onNodeWithTag("Home search").performImeAction()
        composeRule.runOnIdle { assertEquals(listOf(calculator), harness.launchedApps) }
    }

    @Test fun emptyHistoryAndAllAppsHaveNoRecentKeyboardShortcut() {
        val harness = Harness()
        show(harness)
        composeRule.onNodeWithContentDescription("Opens with keyboard Go").assertDoesNotExist()
        composeRule.onNodeWithTag("Home search").performImeAction()
        composeRule.runOnIdle {
            assertTrue(harness.launchedApps.isEmpty())
            harness.history = listOf(RecentSearchResult.App(camera.key))
        }
        composeRule.onNodeWithText("All apps").performClick()
        composeRule.onNodeWithContentDescription("Opens with keyboard Go").assertDoesNotExist()
        composeRule.onNodeWithTag("Home search").performImeAction()
        composeRule.runOnIdle { assertTrue(harness.launchedApps.isEmpty()) }
    }

    @Test fun recentsShowTypesAndHideWhileTypingOrBrowsingAllApps() {
        val harness = Harness(listOf(wifi.asRecentResult(), RecentSearchResult.App(camera.key),
            RecentSearchResult.Contact(contactUri, "Old name")))
        show(harness)
        waitForText("Alex Updated")
        composeRule.onNodeWithText("Recent").assertIsDisplayed()
        listOf("Setting", "App", "Contact").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        query("cam")
        composeRule.onNodeWithText("Recent").assertDoesNotExist()
        query("")
        waitForText("Recent")
        composeRule.onNodeWithText("All apps").performClick()
        composeRule.onNodeWithText("Recent").assertDoesNotExist()
        composeRule.onNodeWithText("Calculator").assertIsDisplayed()
    }

    @Test fun clearImmediatelyHidesTheSectionAndKeepsSearchUsable() {
        val harness = Harness(listOf(RecentSearchResult.App(camera.key)))
        show(harness)
        composeRule.onNodeWithContentDescription("Clear recent searches").performClick()
        composeRule.runOnIdle { assertTrue(harness.history.isEmpty()) }
        composeRule.onNodeWithText("Recent").assertDoesNotExist()
        composeRule.onNodeWithText("Clear").assertDoesNotExist()
        composeRule.onNodeWithText("All apps").assertIsDisplayed()
        query("cam")
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
    }

    @Test fun typedAppResultsAreRecordedOnlyAfterSuccessfulLaunch() {
        val harness = Harness()
        show(harness)
        query("cam")
        composeRule.runOnIdle { assertTrue(harness.history.isEmpty()) }
        composeRule.onNodeWithText("Camera").performClick()
        composeRule.runOnIdle { assertEquals(listOf(RecentSearchResult.App(camera.key)), harness.history) }
        harness.appLaunchSucceeds = false
        query("calc")
        composeRule.onNodeWithText("Calculator").performClick()
        composeRule.runOnIdle { assertEquals(listOf(RecentSearchResult.App(camera.key)), harness.history) }
        assertEquals(1, harness.recorded.size)
    }

    @Test fun unfilteredAppDrawerLaunchesAreNotRecorded() {
        val harness = Harness()
        show(harness)
        composeRule.onNodeWithText("All apps").performClick()
        composeRule.onNodeWithText("Camera").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(camera), harness.launchedApps)
            assertTrue(harness.history.isEmpty())
        }
    }

    @Test fun recentTapOpensDirectlyAndMovesEntryToTop() {
        val harness = Harness(listOf(RecentSearchResult.App(calculator.key), RecentSearchResult.App(camera.key)))
        show(harness)
        composeRule.onNodeWithText("Camera").performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(camera), harness.launchedApps)
            assertEquals(listOf(RecentSearchResult.App(camera.key), RecentSearchResult.App(calculator.key)), harness.history)
        }
        composeRule.onNodeWithText("Recent").assertIsDisplayed()
    }

    @Test fun contactAndSettingsLaunchesSaveDestinationsWithoutPhoneNumbers() {
        val harness = Harness()
        show(harness)
        query("alex")
        waitForText("Alex Updated")
        composeRule.onNodeWithText("Alex Updated").performClick()
        composeRule.runOnIdle { assertEquals(listOf(RecentSearchResult.Contact(contactUri, "Alex Updated")), harness.history) }
        query("wifi")
        waitForText("Wi-Fi")
        composeRule.onNodeWithText("Wi-Fi").performClick()
        composeRule.runOnIdle { assertEquals(wifi.asRecentResult(), harness.history.first()) }
        query("")
        waitForText("Wi-Fi")
        composeRule.onNodeWithText("Wi-Fi").performClick()
        composeRule.runOnIdle {
            assertEquals(wifi, harness.openedDevices.last())
            assertEquals(2, harness.history.size)
        }
        composeRule.onNodeWithText("Alex Updated").performClick()
        composeRule.runOnIdle { assertEquals(contact.asRecentResult(), harness.history.first()) }
        composeRule.onNodeWithText("+123456789").assertDoesNotExist()
    }

    @Test fun failedDeviceLaunchDoesNotEnterHistory() {
        val harness = Harness().apply { deviceLaunchSucceeds = false }
        show(harness)
        query("wifi")
        waitForText("Wi-Fi")
        composeRule.onNodeWithText("Wi-Fi").performClick()
        composeRule.runOnIdle { assertTrue(harness.history.isEmpty()) }
    }

    @Test fun missingDestinationsAreHiddenAndContactsDisappearWhenPermissionIsRevoked() {
        val harness = Harness(listOf(RecentSearchResult.App("uninstalled"),
            RecentSearchResult.Contact("content://com.android.contacts/contacts/lookup/deleted/2", "Deleted"),
            RecentSearchResult.Setting("missing", "Missing setting", "missing"),
            contact.asRecentResult(), RecentSearchResult.App(camera.key)))
        show(harness)
        waitForText("Alex Updated")
        composeRule.onNodeWithText("Deleted").assertDoesNotExist()
        composeRule.onNodeWithText("Missing setting").assertDoesNotExist()
        composeRule.runOnIdle { harness.allowed = false }
        composeRule.onNodeWithText("Alex Updated").assertDoesNotExist()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.runOnIdle { harness.apps = emptyList() }
        composeRule.onNodeWithText("Recent").assertDoesNotExist()
    }

    @Test fun clearingWhileDeviceResolutionIsPendingCannotRestoreOldRecents() {
        val gate = CompletableDeferred<Unit>()
        val harness = Harness(listOf(contact.asRecentResult(), RecentSearchResult.App(camera.key)))
            .apply { resolutionGate = gate }
        show(harness)
        composeRule.onNodeWithContentDescription("Clear recent searches").performClick()
        gate.complete(Unit)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Recent").assertDoesNotExist()
        composeRule.onNodeWithText("Alex Updated").assertDoesNotExist()
    }

    @Test fun recentDeviceResolutionRunsOnlyWhenEmptyHomeSearchIsActive() {
        val harness = Harness(listOf(wifi.asRecentResult())).apply { active = false }
        show(harness)
        composeRule.runOnIdle { assertTrue(harness.resolved.isEmpty()); harness.active = true }
        waitForText("Wi-Fi")
        query("cam")
        val resolvedBefore = harness.resolved.size
        composeRule.runOnIdle { harness.history = listOf(wifi.asRecentResult(), contact.asRecentResult()) }
        composeRule.waitForIdle()
        assertEquals(resolvedBefore, harness.resolved.size)
        query("")
        waitForText("Alex Updated")
    }
}
