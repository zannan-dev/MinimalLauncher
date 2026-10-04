package com.example.minimallauncher

import android.os.Process
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import com.example.minimallauncher.data.apps.ApplicationsRepository
import com.example.minimallauncher.data.preferences.LauncherPreferences
import com.example.minimallauncher.data.preferences.LauncherPreferencesRepository
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.ui.LauncherApp
import com.example.minimallauncher.ui.LauncherViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Before
import org.junit.Test

class LauncherAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before fun useLauncherWindowInsets() {
        composeRule.runOnUiThread {
            composeRule.activity.enableEdgeToEdge()
            composeRule.activity.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    @Test
    fun homeScreenDisplaysClock() {
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(),
            preferencesRepository = TestPreferencesRepository(),
        )

        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }

        composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
    }

    @Test
    fun swipingWhileAssistantIsOpenKeepsHomeAndAssistantVisible() {
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(),
            preferencesRepository = TestPreferencesRepository(),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        val clock = composeRule.onNodeWithContentDescription("Current time")
        val initialX = clock.fetchSemanticsNode().positionInRoot.x
        composeRule.onNodeWithContentDescription("Voice assistant").performClick()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.onRoot().performTouchInput { swipeDown() }
        composeRule.onNodeWithContentDescription("Close assistant").assertIsDisplayed()
        assertEquals(initialX, clock.fetchSemanticsNode().positionInRoot.x, 1f)
        composeRule.onNodeWithContentDescription("Close assistant").performClick()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("Home search").assertDoesNotExist()
        composeRule.onNodeWithText("All apps").assertDoesNotExist()
    }

    @Test
    fun allAppsButtonOpensCompleteListWithoutKeyboard() {
        val user = Process.myUserHandle()
        val apps = (0..40).map { index ->
            LaunchableApp("app$index", "Activity$index", "App %02d".format(index), user, false)
        }
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(apps),
            preferencesRepository = TestPreferencesRepository(),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }

        composeRule.onNodeWithText("All apps").assertDoesNotExist()
        composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
        composeRule.onNodeWithText("All apps").performClick()
        composeRule.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        composeRule.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToIndex(30)
        composeRule.onNodeWithText("App 30").assertIsDisplayed()
        composeRule.onNodeWithText("All apps").assertDoesNotExist()

        composeRule.onNodeWithTag("Home search").assertIsNotFocused()
        assertTrue(ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) != true)
        composeRule.onNodeWithContentDescription("Close home search").performClick()
        composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
        composeRule.onNodeWithText("All apps").assertIsDisplayed()
    }

    @Test
    fun allAppsOnlyAppearsForEmptySearchAndHidesForFullList() {
        val user = Process.myUserHandle()
        val apps = (0..40).map { LaunchableApp("app$it", "Activity$it", "App %02d".format(it), user, false) }
        val viewModel = LauncherViewModel(TestApplicationsRepository(apps), TestPreferencesRepository(autoOpenKeyboard = true))
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
        val search = composeRule.onNodeWithTag("Home search")
        composeRule.onNodeWithText("All apps").assertIsDisplayed()
        search.performTextInput("App 30")
        composeRule.onNodeWithText("All apps").assertDoesNotExist()
        search.assertIsFocused()
        composeRule.onNode(hasText("App 30") and !hasSetTextAction()).assertIsDisplayed()
        search.performTextReplacement(" ")
        composeRule.onNodeWithText("All apps").assertDoesNotExist()
        search.performTextReplacement("")
        composeRule.onNodeWithText("All apps").assertIsDisplayed()
        composeRule.onNodeWithText("All apps").performClick()
        composeRule.onNodeWithText("All apps").assertDoesNotExist()
        composeRule.onNodeWithTag("Home search").assertIsNotFocused()
        composeRule.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        composeRule.onNodeWithText("App 00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Current time").assertDoesNotExist()
        composeRule.onNodeWithTag("Home search view").performTouchInput {
            down(Offset(width * 0.95f, height * 0.2f)); up()
        }
        composeRule.onNodeWithContentDescription("Close home search").assertIsDisplayed()
    }

    @Test
    fun horizontalSwipesStayOnHome() {
        val viewModel = LauncherViewModel(TestApplicationsRepository(), TestPreferencesRepository())
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        val clock = composeRule.onNodeWithContentDescription("Current time")
        val initialX = clock.fetchSemanticsNode().positionInRoot.x
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.onRoot().performTouchInput { swipeRight() }
        clock.assertIsDisplayed()
        assertEquals(initialX, clock.fetchSemanticsNode().positionInRoot.x, 1f)
        composeRule.onNodeWithTag("Home search").assertDoesNotExist()
        composeRule.onNodeWithText("All apps").assertDoesNotExist()
    }

    @Test
    fun autoKeyboardReturnsAtTopAndOnReentryButStaysHiddenWhileBrowsing() {
        val user = Process.myUserHandle()
        val apps = (0..40).map { index ->
            LaunchableApp("app$index", "Activity$index", "App %02d".format(index), user, false)
        }
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(apps),
            preferencesRepository = TestPreferencesRepository(autoOpenKeyboard = true),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        fun keyboardVisible() = ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true

        composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
        val search = composeRule.onNodeWithTag("Home search")
        search.assertIsFocused()
        composeRule.waitUntil(5_000) { keyboardVisible() }
        search.performTextInput("App")
        val list = composeRule.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        list.performTouchInput {
            swipe(Offset(center.x, height * 0.35f), Offset(center.x, height * 0.05f), durationMillis = 400)
        }
        search.assertIsNotFocused()
        composeRule.waitUntil(5_000) { !keyboardVisible() }
        list.performTouchInput {
            swipe(Offset(center.x, height * 0.35f), Offset(center.x, height * 0.05f), durationMillis = 400)
        }
        assertTrue("The app list must actually scroll", list.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value() > 0f)
        val afterScroll = SystemClock.uptimeMillis()
        composeRule.waitUntil(3_000) { SystemClock.uptimeMillis() - afterScroll >= 1_500 }
        search.assertIsNotFocused()
        assertTrue("Keyboard must stay hidden after scrolling", !keyboardVisible())
        list.performScrollToIndex(0)
        search.assertIsFocused()
        composeRule.waitUntil(5_000) { keyboardVisible() }
        list.performTouchInput {
            swipe(Offset(center.x, height * 0.35f), Offset(center.x, height * 0.05f), durationMillis = 400)
        }
        search.assertIsNotFocused()
        composeRule.waitUntil(5_000) { !keyboardVisible() }

        composeRule.onNodeWithContentDescription("Close home search").performClick()
        composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
        search.assertIsFocused()
        composeRule.waitUntil(5_000) { keyboardVisible() }
    }

    @Test
    fun cancellingPilotReturnsToHomeAndFreshSearch() {
        val app = LaunchableApp("camera", "Activity", "Camera", Process.myUserHandle(), false)
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(listOf(app)),
            preferencesRepository = TestPreferencesRepository(intentionalPilotAppKeys = setOf(app.key)),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
        composeRule.onNode(hasSetTextAction()).performTextInput("cam")
        composeRule.onNodeWithText("Camera").performClick()
        composeRule.onNodeWithText("Do you need to open Camera?").assertIsDisplayed()
        Espresso.pressBack()
        composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
        composeRule.onNodeWithText("Search apps...").assertIsDisplayed()
    }

    @Test
    fun systemHomeAfterSearchLaunchStaysOnHomeWithoutKeyboard() {
        val app = LaunchableApp("camera", "Activity", "Camera", Process.myUserHandle(), false)
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(listOf(app)),
            preferencesRepository = TestPreferencesRepository(autoOpenKeyboard = true),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        fun keyboardVisible() = ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true

        repeat(2) {
            composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
            composeRule.onNode(hasSetTextAction()).performTextReplacement("cam")
            composeRule.onNodeWithText("Camera").performClick()
            // Emulate leaving for the result, then receiving Home while stopped.
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            viewModel.onHomePressed()
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
            composeRule.waitUntil(5_000) { !keyboardVisible() }
            val returnedAt = SystemClock.uptimeMillis()
            composeRule.waitUntil(3_000) { SystemClock.uptimeMillis() - returnedAt >= 1_500 }
            composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
            assertTrue("System Home must not restore the search keyboard", !keyboardVisible())
        }
    }

    @Test
    fun downwardSwipeOnHomeOpensStatusBarWithoutSearch() {
        val app = LaunchableApp("favorite", "Activity", "Favorite", Process.myUserHandle(), false)
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(listOf(app)),
            preferencesRepository = TestPreferencesRepository(listOf(app.key)),
        )
        var openCount = 0
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {},
                onOpenNotifications = { openCount++ })
        }
        composeRule.onRoot().performTouchInput {
            swipe(center, Offset(center.x, height * 0.8f))
        }
        composeRule.runOnIdle { assertEquals(1, openCount) }
        composeRule.onNodeWithContentDescription("Close home search").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Favorite Favorite").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voice assistant").assertIsDisplayed()
    }

    @Test
    fun upwardSwipeOnHomeOpensFocusedSearchInsteadOfNotifications() {
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(),
            preferencesRepository = TestPreferencesRepository(),
        )
        var openCount = 0
        composeRule.setContent {
            LauncherApp(
                viewModel = viewModel,
                onOpenDefaultLauncherSettings = {},
                onOpenNotifications = { openCount++ },
            )
        }

        composeRule.onRoot().performTouchInput {
            swipe(center, Offset(center.x, height * 0.2f))
        }
        composeRule.runOnIdle { assertEquals(0, openCount) }
        composeRule.onNodeWithTag("Home search").assertIsFocused()
        composeRule.onNodeWithContentDescription("Current time").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Close home search").assertIsDisplayed()
    }

    @Test
    fun upwardSwipeWithFavoritesSearchesAndRestoresHome() {
        val app = LaunchableApp("favorite", "Activity", "Favorite", Process.myUserHandle(), false)
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(listOf(app)),
            preferencesRepository = TestPreferencesRepository(listOf(app.key)),
        )
        var openCount = 0
        composeRule.setContent {
            LauncherApp(
                viewModel = viewModel,
                onOpenDefaultLauncherSettings = {},
                onOpenNotifications = { openCount++ },
            )
        }

        composeRule.onNodeWithContentDescription("Favorite Favorite").assertIsDisplayed()
        composeRule.onRoot().performTouchInput {
            swipe(center, Offset(center.x, height * 0.2f))
        }
        composeRule.runOnIdle { assertEquals(0, openCount) }
        composeRule.onNodeWithTag("Home search").assertIsFocused()
        composeRule.onNodeWithContentDescription("Close home search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Favorite Favorite").assertDoesNotExist()
        composeRule.onNodeWithTag("Home search").performTextInput("Fav")
        val result = composeRule.onNode(hasText("Favorite") and hasAnyAncestor(hasTestTag("Home search view")))
        composeRule.waitUntil(5_000) { result.isDisplayed() }
        result.assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.onNodeWithContentDescription("Close home search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close home search").performClick()
        composeRule.onNodeWithContentDescription("Voice assistant").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Favorite Favorite").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
    }

    @Test
    fun homeSearchFinishesShrinkingBeforeItsViewIsRemoved() {
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(),
            preferencesRepository = TestPreferencesRepository(),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        // Keep native IME animation outside the paused Compose clock check.
        composeRule.onNodeWithText("All apps").assertDoesNotExist()
        composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
        composeRule.onNodeWithText("All apps").performClick()
        composeRule.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        val pill = composeRule.onNodeWithTag("Home search")
        pill.assertIsNotFocused()
        val expandedWidth = pill.fetchSemanticsNode().size.width
        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithContentDescription("Close home search").performClick()
            composeRule.mainClock.advanceTimeBy(180)
            val closingWidth = pill.fetchSemanticsNode().size.width
            assertTrue("Pill must remain while shrinking", closingWidth in 1 until expandedWidth)
            composeRule.mainClock.advanceTimeBy(1_000)
            pill.assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Voice assistant").assertIsDisplayed()
        } finally { composeRule.mainClock.autoAdvance = true }
    }

    @Test
    fun blankBackgroundClosesOnlyEmptyHomeSearch() {
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(),
            preferencesRepository = TestPreferencesRepository(),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        fun openSearch() {
            composeRule.onRoot().performTouchInput {
                swipe(center, Offset(center.x, height * 0.2f))
            }
            composeRule.onNodeWithTag("Home search").assertIsFocused()
        }
        fun tapBackground() {
            composeRule.onNodeWithTag("Home search view").performTouchInput {
                down(Offset(width * 0.5f, height * 0.2f)); up()
            }
        }
        openSearch()
        fun keyboardVisible() = ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        composeRule.waitUntil(5_000) { keyboardVisible() }
        val background = composeRule.onNodeWithTag("Home search view")
        background.performTouchInput {
            down(Offset(center.x, height * 0.6f))
            moveTo(Offset(center.x, height * 0.4f), delayMillis = 200)
        }
        composeRule.onNodeWithTag("Home search").assertIsFocused()
        assertTrue("Empty search swipe must keep the keyboard visible", keyboardVisible())
        background.performTouchInput {
            moveTo(Offset(center.x, height * 0.2f), delayMillis = 200)
            up()
        }
        composeRule.onNodeWithTag("Home search").assertIsFocused()
        assertTrue("Releasing an empty search swipe must keep the keyboard visible", keyboardVisible())
        tapBackground()
        composeRule.onNodeWithContentDescription("Close home search").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Voice assistant").assertIsDisplayed()
        openSearch()
        composeRule.onNodeWithTag("Home search").performTextInput("zzqxv")
        tapBackground()
        composeRule.onNodeWithContentDescription("Close home search").assertIsDisplayed()
    }

    @Test
    fun homeAndBackDismissHomeSearchAndRestoreAssistant() {
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(),
            preferencesRepository = TestPreferencesRepository(),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        repeat(2) { attempt ->
            composeRule.onRoot().performTouchInput {
                swipe(center, Offset(center.x, height * 0.2f))
            }
            composeRule.onNodeWithContentDescription("Close home search").assertIsDisplayed()
            composeRule.runOnUiThread {
                if (attempt == 0) viewModel.onHomePressed()
                else composeRule.activity.onBackPressedDispatcher.onBackPressed()
            }
            composeRule.onNodeWithContentDescription("Close home search").assertDoesNotExist()
            composeRule.onNodeWithContentDescription("Voice assistant").assertIsDisplayed()
        }
    }

    @Test
    fun draggingFavoriteStaysOnHome() {
        val user = Process.myUserHandle()
        val first = LaunchableApp("first", "Activity", "First", user, false)
        val second = LaunchableApp("second", "Activity", "Second", user, false)
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(listOf(first, second)),
            preferencesRepository = TestPreferencesRepository(listOf(first.key, second.key)),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }

        val source = composeRule.onNodeWithContentDescription("Favorite Second")
        val sourceBounds = source.fetchSemanticsNode().boundsInRoot
        val target = composeRule.onNodeWithContentDescription("Favorite First")
            .fetchSemanticsNode().boundsInRoot.center - sourceBounds.topLeft
        source.performTouchInput {
            down(center)
            advanceEventTime(700)
            moveTo(target)
            up()
        }

        composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
    }

    @Test
    fun scrollingAllAppsKeepsHomeListOpen() {
        val user = Process.myUserHandle()
        val apps = (0..40).map { index ->
            LaunchableApp("app$index", "Activity$index", "App %02d".format(index), user, false)
        }
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(apps),
            preferencesRepository = TestPreferencesRepository(),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }

        composeRule.onNodeWithText("All apps").assertDoesNotExist()
        composeRule.onRoot().performTouchInput { swipe(center, Offset(center.x, height * 0.2f)) }
        composeRule.onNodeWithText("All apps").performClick()
        composeRule.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        composeRule.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToIndex(0)
        composeRule.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performTouchInput { swipeDown() }

        composeRule.onNodeWithText("App 00").assertIsDisplayed()
        composeRule.onNodeWithText("Search apps...").assertIsDisplayed()
    }
}

private class TestApplicationsRepository(
    private val apps: List<LaunchableApp> = emptyList(),
) : ApplicationsRepository {
    override suspend fun loadApps(): List<LaunchableApp> = apps

    override fun launchApp(app: LaunchableApp): Boolean = true
}

private class TestPreferencesRepository(
    favorites: List<String> = emptyList(),
    autoOpenKeyboard: Boolean = false,
    intentionalPilotAppKeys: Set<String> = emptySet(),
) : LauncherPreferencesRepository {
    private val state = MutableStateFlow(
        LauncherPreferences(
            showDate = true,
            theme = ThemePreference.LIGHT,
            favoriteAppKeys = favorites.toSet(),
            favoriteAppOrder = favorites,
            autoOpenKeyboard = autoOpenKeyboard,
            doubleTapToLock = false,
            showStatusBar = true,
            isIntentionalPilotEnabled = intentionalPilotAppKeys.isNotEmpty(),
            intentionalPilotAppKeys = intentionalPilotAppKeys,
        ),
    )
    override val preferences: Flow<LauncherPreferences> = state

    override suspend fun setShowDate(enabled: Boolean) = Unit
    override suspend fun setAutoOpenKeyboard(enabled: Boolean) = Unit
    override suspend fun setDoubleTapToLock(enabled: Boolean) = Unit
    override suspend fun setShowStatusBar(enabled: Boolean) = Unit
    override suspend fun setIntentionalPilotEnabled(enabled: Boolean) = Unit
    override suspend fun toggleIntentionalPilotApp(appKey: String) = Unit
    override suspend fun setTheme(theme: ThemePreference) = Unit
    override suspend fun toggleFavorite(appKey: String) = Unit
    override suspend fun removeFavorite(appKey: String) = Unit
    override suspend fun moveFavorite(fromKey: String, toKey: String) = Unit
}
