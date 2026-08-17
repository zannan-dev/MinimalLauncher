package com.example.minimallauncher.ui

import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minimallauncher.ui.apps.AppDrawerScreen
import com.example.minimallauncher.ui.apps.IntentionalPilotAppSelectionScreen
import com.example.minimallauncher.ui.apps.IntentionalPilotScreen
import com.example.minimallauncher.ui.home.HomeScreen
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.ui.settings.SettingsScreen
import com.example.minimallauncher.ui.theme.LauncherTheme

private enum class LauncherScreen { HOME, APPS, SETTINGS, INTENTIONAL_PILOT_APPS }

@Composable
fun LauncherApp(
    viewModel: LauncherViewModel,
    onOpenDefaultLauncherSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentScreenName by rememberSaveable { mutableStateOf(LauncherScreen.HOME.name) }
    val currentScreen = LauncherScreen.valueOf(currentScreenName)
    val openHome = { currentScreenName = LauncherScreen.HOME.name }
    var appPendingLaunch by androidx.compose.runtime.remember { mutableStateOf<LaunchableApp?>(null) }

    val handleAppLaunch: (LaunchableApp) -> Unit = { app ->
        if (state.preferences.isIntentionalPilotEnabled && app.key in state.preferences.intentionalPilotAppKeys) {
            appPendingLaunch = app
        } else {
            viewModel.launchApp(app)
        }
    }

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.homeEvents.collect {
            openHome()
        }
    }

    BackHandler(enabled = currentScreen != LauncherScreen.HOME, onBack = openHome)

    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowInsetsControllerCompat(window, view)
        if (state.preferences.showStatusBar) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    LauncherTheme(preference = state.preferences.theme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val paddingModifier = if (state.preferences.showStatusBar) {
                Modifier.systemBarsPadding()
            } else {
                Modifier.navigationBarsPadding().displayCutoutPadding()
            }
            Box(modifier = Modifier.fillMaxSize().then(paddingModifier)) {
                val pendingApp = appPendingLaunch
                if (pendingApp != null) {
                    IntentionalPilotScreen(
                        app = pendingApp,
                        delaySeconds = state.preferences.intentionalPilotDelaySeconds,
                        onLaunchApp = {
                            viewModel.launchApp(pendingApp)
                            appPendingLaunch = null
                        },
                        onCancel = {
                            appPendingLaunch = null
                            openHome()
                        }
                    )
                } else {
                    when (currentScreen) {
                        LauncherScreen.HOME -> HomeScreen(
                            use24HourClock = state.preferences.use24HourClock,
                            showDate = state.preferences.showDate,
                            doubleTapToLock = state.preferences.doubleTapToLock,
                            isFlowZoneEnabled = state.preferences.isFlowZoneEnabled,
                            flowZoneState = state.flowZoneState,
                            onToggleFlowZoneTimer = viewModel::toggleFlowZoneTimer,
                            onSkipFlowZonePhase = viewModel::skipFlowZonePhase,
                            onResetFlowZone = viewModel::resetFlowZone,
                            onOpenDrawer = { currentScreenName = LauncherScreen.APPS.name },
                            onOpenSettings = { currentScreenName = LauncherScreen.SETTINGS.name },
                            onLaunchApp = handleAppLaunch,
                        )
                    LauncherScreen.APPS -> AppDrawerScreen(
                        apps = state.apps,
                        autoOpenKeyboard = state.preferences.autoOpenKeyboard,
                        isLoading = state.isLoadingApps,
                        failedToLoad = state.appLoadError,
                        onBack = openHome,
                        onLaunchApp = handleAppLaunch,
                    )
                    LauncherScreen.SETTINGS -> SettingsScreen(
                        use24HourClock = state.preferences.use24HourClock,
                        showDate = state.preferences.showDate,
                        autoOpenKeyboard = state.preferences.autoOpenKeyboard,
                        doubleTapToLock = state.preferences.doubleTapToLock,
                        showStatusBar = state.preferences.showStatusBar,
                        isIntentionalPilotEnabled = state.preferences.isIntentionalPilotEnabled,
                        intentionalPilotDelaySeconds = state.preferences.intentionalPilotDelaySeconds,
                        isFlowZoneEnabled = state.preferences.isFlowZoneEnabled,
                        flowZoneFocusMinutes = state.preferences.flowZoneFocusMinutes,
                        flowZoneBreakMinutes = state.preferences.flowZoneBreakMinutes,
                        flowZoneLongBreakMinutes = state.preferences.flowZoneLongBreakMinutes,
                        theme = state.preferences.theme,
                        onUse24HourClockChanged = viewModel::setUse24HourClock,
                        onShowDateChanged = viewModel::setShowDate,
                        onAutoOpenKeyboardChanged = viewModel::setAutoOpenKeyboard,
                        onDoubleTapToLockChanged = viewModel::setDoubleTapToLock,
                        onShowStatusBarChanged = viewModel::setShowStatusBar,
                        onIntentionalPilotEnabledChanged = viewModel::setIntentionalPilotEnabled,
                        onIntentionalPilotDelayChanged = viewModel::setIntentionalPilotDelaySeconds,
                        onSelectIntentionalPilotApps = { currentScreenName = LauncherScreen.INTENTIONAL_PILOT_APPS.name },
                        onFlowZoneEnabledChanged = viewModel::setFlowZoneEnabled,
                        onFlowZoneFocusMinutesChanged = viewModel::setFlowZoneFocusMinutes,
                        onFlowZoneBreakMinutesChanged = viewModel::setFlowZoneBreakMinutes,
                        onFlowZoneLongBreakMinutesChanged = viewModel::setFlowZoneLongBreakMinutes,
                        onThemeChanged = viewModel::setTheme,
                        onOpenDefaultLauncherSettings = onOpenDefaultLauncherSettings,
                    )
                    LauncherScreen.INTENTIONAL_PILOT_APPS -> IntentionalPilotAppSelectionScreen(
                        apps = state.apps,
                        selectedAppKeys = state.preferences.intentionalPilotAppKeys,
                        onToggleApp = viewModel::toggleIntentionalPilotApp,
                        onBack = { currentScreenName = LauncherScreen.SETTINGS.name },
                    )
                }
                }
            }
        }
    }
}
