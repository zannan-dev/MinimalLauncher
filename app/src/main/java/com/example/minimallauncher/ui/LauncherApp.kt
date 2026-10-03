package com.example.minimallauncher.ui

import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
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

private enum class LauncherScreen { HOME, SETTINGS, INTENTIONAL_PILOT_APPS }

@Composable
fun LauncherApp(
    viewModel: LauncherViewModel,
    onOpenDefaultLauncherSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appDrawerListState = rememberLazyListState()
    var currentScreenName by rememberSaveable { mutableStateOf(LauncherScreen.HOME.name) }
    val currentScreen = LauncherScreen.valueOf(currentScreenName)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val openHome: () -> Unit = {
        currentScreenName = LauncherScreen.HOME.name
        scope.launch { pagerState.animateScrollToPage(0) }
        Unit
    }
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

    BackHandler(enabled = currentScreen != LauncherScreen.HOME || pagerState.currentPage != 0 || pagerState.currentPageOffsetFraction != 0f, onBack = openHome)

    val view = LocalView.current
    LaunchedEffect(view, state.preferences.showStatusBar) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        if (state.preferences.showStatusBar) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    LauncherTheme(preference = state.preferences.theme) {
        val background = if (currentScreen == LauncherScreen.HOME && appPendingLaunch == null) {
            lerp(Color.Black, MaterialTheme.colorScheme.surface,
                (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f))
        } else {
            MaterialTheme.colorScheme.surface
        }
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowInsetsControllerCompat(window, view).isAppearanceLightNavigationBars =
                    background.luminance() > 0.5f
            }
        }
        Surface(modifier = Modifier.fillMaxSize(), color = background) {
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
                    androidx.compose.animation.AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(250)) togetherWith
                                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(250))
                        },
                        label = "Screen Transition"
                    ) { screen ->
                        when (screen) {
                            LauncherScreen.HOME -> HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1,
                            ) { page ->
                                if (page == 0) HomeScreen(
                                    favoriteApps = state.favoriteApps,
                                    showDate = state.preferences.showDate,
                                    doubleTapToLock = state.preferences.doubleTapToLock,
                                    onOpenNotifications = onOpenNotifications,
                                    onOpenSettings = { currentScreenName = LauncherScreen.SETTINGS.name },
                                    onSetDefaultLauncher = onOpenDefaultLauncherSettings,
                                    onLaunchApp = handleAppLaunch,
                                    onMoveFavorite = viewModel::moveFavorite,
                                    onRemoveFavorite = viewModel::removeFavorite,
                                )
                                else Surface(modifier = Modifier.fillMaxSize()) {
                                    AppDrawerScreen(
                                        apps = state.apps,
                                        listState = appDrawerListState,
                                        autoOpenKeyboard = state.preferences.autoOpenKeyboard,
                                        isActive = currentScreen == LauncherScreen.HOME &&
                                            pagerState.settledPage == 1,
                                        isPageMoving = pagerState.currentPageOffsetFraction != 0f,
                                        isLoading = state.isLoadingApps,
                                        failedToLoad = state.appLoadError,
                                        favoriteKeys = state.preferences.favoriteAppKeys,
                                        onBack = openHome,
                                        onLaunchApp = handleAppLaunch,
                                        onToggleFavorite = viewModel::toggleFavorite,
                                    )
                                }
                            }
                            LauncherScreen.SETTINGS -> SettingsScreen(
                                showDate = state.preferences.showDate,
                                autoOpenKeyboard = state.preferences.autoOpenKeyboard,
                                doubleTapToLock = state.preferences.doubleTapToLock,
                                showStatusBar = state.preferences.showStatusBar,
                                isIntentionalPilotEnabled = state.preferences.isIntentionalPilotEnabled,
                                theme = state.preferences.theme,
                                onShowDateChanged = viewModel::setShowDate,
                                onAutoOpenKeyboardChanged = viewModel::setAutoOpenKeyboard,
                                onDoubleTapToLockChanged = viewModel::setDoubleTapToLock,
                                onShowStatusBarChanged = viewModel::setShowStatusBar,
                                onIntentionalPilotEnabledChanged = viewModel::setIntentionalPilotEnabled,
                                onSelectIntentionalPilotApps = { currentScreenName = LauncherScreen.INTENTIONAL_PILOT_APPS.name },
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
}
