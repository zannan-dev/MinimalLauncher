package com.example.minimallauncher.ui

import android.os.UserHandle
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
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val userHandle = mock(UserHandle::class.java)

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
        val camera = LaunchableApp("camera", "CameraActivity", "Camera", userHandle, false)
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
        val phone = LaunchableApp("phone", "PhoneActivity", "Phone", userHandle, false)
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
            showDate = true,
            theme = ThemePreference.SYSTEM,
            favoriteAppKeys = emptySet(),
            favoriteAppOrder = emptyList(),
            autoOpenKeyboard = true,
            doubleTapToLock = false,
            showStatusBar = true,
            isIntentionalPilotEnabled = false,
            intentionalPilotAppKeys = emptySet(),
        ),
    )
    override val preferences: Flow<LauncherPreferences> = state

    override suspend fun setShowDate(enabled: Boolean) {
        state.value = state.value.copy(showDate = enabled)
    }

    override suspend fun setAutoOpenKeyboard(enabled: Boolean) = Unit
    override suspend fun setDoubleTapToLock(enabled: Boolean) = Unit
    override suspend fun setShowStatusBar(enabled: Boolean) = Unit
    override suspend fun setIntentionalPilotEnabled(enabled: Boolean) = Unit
    override suspend fun toggleIntentionalPilotApp(appKey: String) = Unit

    override suspend fun setTheme(theme: ThemePreference) {
        state.value = state.value.copy(theme = theme)
    }

    override suspend fun toggleFavorite(appKey: String) {
        val current = state.value
        val updated = toggledFavorite(current.favoriteAppKeys, appKey)
        state.value = current.copy(
            favoriteAppKeys = updated,
            favoriteAppOrder = if (appKey in updated) current.favoriteAppOrder + appKey
                else current.favoriteAppOrder - appKey,
        )
    }
    override suspend fun removeFavorite(appKey: String) {
        state.value = state.value.copy(
            favoriteAppKeys = state.value.favoriteAppKeys - appKey,
            favoriteAppOrder = state.value.favoriteAppOrder - appKey,
        )
    }
    override suspend fun moveFavorite(fromKey: String, toKey: String) {
        state.value = state.value.copy(
            favoriteAppOrder = com.example.minimallauncher.domain.movedFavorite(
                state.value.favoriteAppOrder, fromKey, toKey,
            ),
        )
    }
}
