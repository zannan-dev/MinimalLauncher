package com.example.minimallauncher

import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Test

class LauncherAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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
    fun leftSwipeOpensAppsAndRestoresScrollPosition() {
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

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.onNode(hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).performScrollToIndex(30)
        composeRule.onNodeWithText("App 30").assertIsDisplayed()

        composeRule.onRoot().performTouchInput { swipeRight() }
        composeRule.onNodeWithContentDescription("Current time").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("App 30").assertIsDisplayed()
    }

    @Test
    fun drawerTracksHeldSwipeAndCanReverseBeforeRelease() {
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(),
            preferencesRepository = TestPreferencesRepository(),
        )
        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {}, onOpenNotifications = {})
        }
        val clock = composeRule.onNodeWithContentDescription("Current time")
        val initialX = clock.fetchSemanticsNode().positionInRoot.x
        composeRule.onRoot().performTouchInput {
            down(Offset(width * 0.8f, height * 0.35f))
            moveTo(Offset(width * 0.7f, height * 0.35f), delayMillis = 100)
            moveTo(Offset(width * 0.4f, height * 0.35f), delayMillis = 400)
        }
        val pager = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
        fun pageOffset() = pager.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value()
        val viewportWidth = composeRule.onRoot().fetchSemanticsNode().size.width
        val heldX = pageOffset()
        assertTrue("Drawer should be partially revealed", heldX > viewportWidth * 0.2f && heldX < viewportWidth * 0.6f)
        composeRule.mainClock.advanceTimeBy(500)
        assertEquals(heldX, pageOffset(), 1f)
        composeRule.onRoot().performTouchInput {
            moveTo(Offset(width * 0.8f, height * 0.35f), delayMillis = 500)
            advanceEventTime(300)
            up()
        }
        clock.assertIsDisplayed()
        assertEquals(initialX, clock.fetchSemanticsNode().positionInRoot.x, 1f)

        composeRule.onRoot().performTouchInput { swipeLeft() }
        val drawerX = pageOffset()
        composeRule.onRoot().performTouchInput {
            down(Offset(width * 0.2f, height * 0.35f))
            moveTo(Offset(width * 0.3f, height * 0.35f), delayMillis = 100)
            moveTo(Offset(width * 0.6f, height * 0.35f), delayMillis = 400)
        }
        val heldDrawerX = pageOffset()
        assertTrue("Drawer should move with the return swipe", heldDrawerX < drawerX - 50f)
        composeRule.mainClock.advanceTimeBy(500)
        assertEquals(heldDrawerX, pageOffset(), 1f)
        composeRule.onRoot().performTouchInput {
            moveTo(Offset(width * 0.9f, height * 0.35f), delayMillis = 500)
            advanceEventTime(300)
            up()
        }
        clock.assertIsDisplayed()
    }

    @Test
    fun downwardSwipeOnHomeOpensNotifications() {
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
            swipe(center, Offset(center.x, bottomCenter.y * 0.75f))
        }
        composeRule.runOnIdle { assertEquals(1, openCount) }
    }

    @Test
    fun downwardSwipeWithFavoritesOpensNotifications() {
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
            swipe(center, Offset(center.x, bottomCenter.y * 0.75f))
        }
        composeRule.runOnIdle { assertEquals(1, openCount) }
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
    fun downwardSwipeAtTopOfDrawerKeepsDrawerOpen() {
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

        composeRule.onRoot().performTouchInput { swipeLeft() }
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
) : LauncherPreferencesRepository {
    private val state = MutableStateFlow(
        LauncherPreferences(
            showDate = true,
            theme = ThemePreference.LIGHT,
            favoriteAppKeys = favorites.toSet(),
            favoriteAppOrder = favorites,
            autoOpenKeyboard = false,
            doubleTapToLock = false,
            showStatusBar = true,
            isIntentionalPilotEnabled = false,
            intentionalPilotAppKeys = emptySet(),
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
