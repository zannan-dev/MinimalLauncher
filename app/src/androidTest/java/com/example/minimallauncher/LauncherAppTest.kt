package com.example.minimallauncher

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.minimallauncher.data.apps.ApplicationsRepository
import com.example.minimallauncher.data.preferences.LauncherPreferences
import com.example.minimallauncher.data.preferences.LauncherPreferencesRepository
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.ui.LauncherApp
import com.example.minimallauncher.ui.LauncherViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class LauncherAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun homeScreenOpensTheAppDrawer() {
        val viewModel = LauncherViewModel(
            applicationsRepository = TestApplicationsRepository(),
            preferencesRepository = TestPreferencesRepository(),
        )

        composeRule.setContent {
            LauncherApp(viewModel = viewModel, onOpenDefaultLauncherSettings = {})
        }

        composeRule.onNodeWithText("All apps").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Apps").assertIsDisplayed()
    }
}

private class TestApplicationsRepository : ApplicationsRepository {
    override suspend fun loadApps(): List<LaunchableApp> = emptyList()

    override fun launchApp(app: LaunchableApp): Boolean = true
}

private class TestPreferencesRepository : LauncherPreferencesRepository {
    private val state = MutableStateFlow(
        LauncherPreferences(
            use24HourClock = false,
            showDate = true,
            theme = ThemePreference.LIGHT,
            favoriteAppKeys = emptySet(),
        ),
    )
    override val preferences: Flow<LauncherPreferences> = state

    override suspend fun setUse24HourClock(enabled: Boolean) = Unit
    override suspend fun setShowDate(enabled: Boolean) = Unit
    override suspend fun setTheme(theme: ThemePreference) = Unit
    override suspend fun toggleFavorite(appKey: String) = Unit
}
