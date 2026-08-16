package com.example.minimallauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minimallauncher.ui.apps.AppDrawerScreen
import com.example.minimallauncher.ui.home.HomeScreen
import com.example.minimallauncher.ui.settings.SettingsScreen
import com.example.minimallauncher.ui.theme.LauncherTheme

private enum class LauncherScreen { HOME, APPS, SETTINGS }

@Composable
fun LauncherApp(
    viewModel: LauncherViewModel,
    onOpenDefaultLauncherSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreenName by rememberSaveable { mutableStateOf(LauncherScreen.HOME.name) }
    val currentScreen = LauncherScreen.valueOf(currentScreenName)
    val openHome = { currentScreenName = LauncherScreen.HOME.name }

    BackHandler(enabled = currentScreen != LauncherScreen.HOME, onBack = openHome)

    LauncherTheme(preference = state.preferences.theme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                when (currentScreen) {
                    LauncherScreen.HOME -> HomeScreen(
                        use24HourClock = state.preferences.use24HourClock,
                        showDate = state.preferences.showDate,
                        doubleTapToLock = state.preferences.doubleTapToLock,
                        onOpenDrawer = { currentScreenName = LauncherScreen.APPS.name },
                        onOpenSettings = { currentScreenName = LauncherScreen.SETTINGS.name },
                        onLaunchApp = viewModel::launchApp,
                    )
                    LauncherScreen.APPS -> AppDrawerScreen(
                        apps = state.apps,
                        autoOpenKeyboard = state.preferences.autoOpenKeyboard,
                        isLoading = state.isLoadingApps,
                        failedToLoad = state.appLoadError,
                        onBack = openHome,
                        onLaunchApp = viewModel::launchApp,
                    )
                    LauncherScreen.SETTINGS -> SettingsScreen(
                        use24HourClock = state.preferences.use24HourClock,
                        showDate = state.preferences.showDate,
                        autoOpenKeyboard = state.preferences.autoOpenKeyboard,
                        doubleTapToLock = state.preferences.doubleTapToLock,
                        theme = state.preferences.theme,
                        onUse24HourClockChanged = viewModel::setUse24HourClock,
                        onShowDateChanged = viewModel::setShowDate,
                        onAutoOpenKeyboardChanged = viewModel::setAutoOpenKeyboard,
                        onDoubleTapToLockChanged = viewModel::setDoubleTapToLock,
                        onThemeChanged = viewModel::setTheme,
                        onOpenDefaultLauncherSettings = onOpenDefaultLauncherSettings,
                    )
                }
            }
        }
    }
}
