package com.example.minimallauncher.ui

import com.example.minimallauncher.data.apps.ApplicationsRepository
import com.example.minimallauncher.data.preferences.LauncherPreferences
import com.example.minimallauncher.data.preferences.LauncherPreferencesRepository
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.domain.toggledFavorite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads apps and reflects favorite and theme updates`() = runTest(dispatcher) {
        val camera = LaunchableApp("camera", "CameraActivity", "Camera")
        val apps = FakeApplicationsRepository(listOf(camera))
        val preferences = FakePreferencesRepository()
        val viewModel = LauncherViewModel(apps, preferences)

        advanceUntilIdle()
        assertEquals(listOf(camera), viewModel.uiState.value.apps)

        viewModel.toggleFavorite(camera)
        viewModel.setTheme(ThemePreference.DARK)
        advanceUntilIdle()

        assertEquals(listOf(camera), viewModel.uiState.value.favoriteApps)
        assertEquals(ThemePreference.DARK, viewModel.uiState.value.preferences.theme)
    }

    @Test
    fun `launches requested app through repository`() = runTest(dispatcher) {
        val phone = LaunchableApp("phone", "PhoneActivity", "Phone")
        val apps = FakeApplicationsRepository(listOf(phone))
        val viewModel = LauncherViewModel(apps, FakePreferencesRepository())

        assertTrue(viewModel.launchApp(phone))
        assertEquals(phone, apps.launchedApp)
    }
}

private class FakeApplicationsRepository(
    private val installedApps: List<LaunchableApp>,
) : ApplicationsRepository {
    var launchedApp: LaunchableApp? = null

    override suspend fun loadApps(): List<LaunchableApp> = installedApps

    override fun launchApp(app: LaunchableApp): Boolean {
        launchedApp = app
        return true
    }
}

private class FakePreferencesRepository : LauncherPreferencesRepository {
    private val state = MutableStateFlow(
        LauncherPreferences(
            use24HourClock = false,
            showDate = true,
            theme = ThemePreference.SYSTEM,
            favoriteAppKeys = emptySet(),
        ),
    )
    override val preferences: Flow<LauncherPreferences> = state

    override suspend fun setUse24HourClock(enabled: Boolean) {
        state.value = state.value.copy(use24HourClock = enabled)
    }

    override suspend fun setShowDate(enabled: Boolean) {
        state.value = state.value.copy(showDate = enabled)
    }

    override suspend fun setTheme(theme: ThemePreference) {
        state.value = state.value.copy(theme = theme)
    }

    override suspend fun toggleFavorite(appKey: String) {
        state.value = state.value.copy(favoriteAppKeys = toggledFavorite(state.value.favoriteAppKeys, appKey))
    }
}
