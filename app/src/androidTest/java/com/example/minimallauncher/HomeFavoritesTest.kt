package com.example.minimallauncher

import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.example.minimallauncher.ui.home.HomeScreen
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.ui.home.HomeFavorites
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeFavoritesTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val user = Process.myUserHandle()
    private val camera = LaunchableApp("camera", "CameraActivity", "Camera", user, false)
    private val phone = LaunchableApp("phone", "PhoneActivity", "Phone", user, false)

    @Test
    fun longPressAndDragReordersFavorites() {
        var apps by mutableStateOf(listOf(camera, phone))
        composeRule.setContent {
            HomeFavorites(
                apps = apps,
                onLaunchApp = {},
                onMoveFavorite = { from, to ->
                    apps = apps.toMutableList().apply {
                        remove(from)
                        add(indexOf(to), from)
                    }
                },
                onRemoveFavorite = {},
            )
        }

        val source = composeRule.onNodeWithContentDescription("Favorite Phone")
        val sourceBounds = source.fetchSemanticsNode().boundsInRoot
        val target = composeRule.onNodeWithContentDescription("Favorite Camera")
            .fetchSemanticsNode().boundsInRoot.center - sourceBounds.topLeft
        source.performTouchInput {
            down(center)
            advanceEventTime(700)
            moveTo(target)
            up()
        }

        composeRule.runOnIdle { assertEquals(listOf(phone, camera), apps) }
    }

    @Test
    fun draggingFavoriteToTrashRemovesIt() {
        var apps by mutableStateOf(listOf(camera))
        composeRule.setContent {
            HomeFavorites(
                apps = apps,
                onLaunchApp = {},
                onMoveFavorite = { _, _ -> },
                onRemoveFavorite = { apps = apps - it },
            )
        }

        val source = composeRule.onNodeWithContentDescription("Favorite Camera")
        source.performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(0f, 1f))
        }
        val target = composeRule.onNodeWithContentDescription("Remove favorite").fetchSemanticsNode().boundsInRoot.center
        composeRule.onRoot().performTouchInput { moveTo(target); up() }

        composeRule.runOnIdle { assertEquals(emptyList<LaunchableApp>(), apps) }
    }
    @Test fun homeFavoriteCanBeRemovedByDragging() {
        var apps by mutableStateOf(listOf(camera))
        composeRule.setContent {
            HomeScreen(favoriteApps = apps, showDate = false,
                onOpenSearch = {}, onOpenSettings = {}, onSetDefaultLauncher = {}, onLaunchApp = {},
                onMoveFavorite = { _, _ -> }, onRemoveFavorite = { apps = apps - it })
        }
        composeRule.onNodeWithContentDescription("Favorite Camera").performTouchInput {
            down(center); advanceEventTime(700); moveBy(Offset(0f, 1f))
        }
        val target = composeRule.onNodeWithContentDescription("Remove favorite").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot.center
        composeRule.onRoot().performTouchInput { moveTo(target) }
        composeRule.onNodeWithText("Release to remove").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { up() }
        composeRule.runOnIdle { assertEquals(emptyList<LaunchableApp>(), apps) }
        composeRule.onNodeWithContentDescription("Remove favorite").assertDoesNotExist()
    }

    @Test fun cancellingHomeDragKeepsFavorite() {
        composeRule.setContent {
            HomeScreen(favoriteApps = listOf(camera), showDate = false,
                onOpenSearch = {}, onOpenSettings = {}, onSetDefaultLauncher = {}, onLaunchApp = {},
                onMoveFavorite = { _, _ -> }, onRemoveFavorite = { error("Cancelled drag removed a favorite") })
        }
        composeRule.onNodeWithContentDescription("Favorite Camera").performTouchInput {
            down(center); advanceEventTime(700); moveBy(Offset(0f, 1f))
        }
        composeRule.onNodeWithContentDescription("Remove favorite").assertIsDisplayed()
        composeRule.onRoot().performTouchInput { cancel() }
        composeRule.onNodeWithContentDescription("Favorite Camera").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove favorite").assertDoesNotExist()
    }

}
