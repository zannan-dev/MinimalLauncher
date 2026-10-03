package com.example.minimallauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.foundation.pager.PagerDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.example.minimallauncher.ui.motion.LauncherMotion
import com.example.minimallauncher.ui.theme.LauncherTheme

private enum class LauncherScreen { HOME, SETTINGS, INTENTIONAL_PILOT_APPS }

@Composable
fun LauncherApp(
    viewModel: LauncherViewModel,
    onOpenDefaultLauncherSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val homeRequest by viewModel.homeRequests.collectAsState()
    var handledHomeRequest by rememberSaveable { mutableStateOf(0L) }
    val returningHome = homeRequest != handledHomeRequest
    val appDrawerListState = rememberLazyListState()
    var currentScreenName by rememberSaveable { mutableStateOf(LauncherScreen.HOME.name) }
    val currentScreen = LauncherScreen.valueOf(currentScreenName)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var appPendingLaunch by remember { mutableStateOf<LaunchableApp?>(null) }
    var lastPendingApp by remember { mutableStateOf<LaunchableApp?>(null) }
    val openHome: () -> Unit = {
        appPendingLaunch = null
        currentScreenName = LauncherScreen.HOME.name
        scope.launch { pagerState.animateScrollToPage(0, animationSpec = LauncherMotion.settle()) }
        Unit
    }

    val handleAppLaunch: (LaunchableApp) -> Unit = { app ->
        if (state.preferences.isIntentionalPilotEnabled && app.key in state.preferences.intentionalPilotAppKeys) {
            lastPendingApp = app
            appPendingLaunch = app
        } else {
            viewModel.launchApp(app)
        }
    }

    LaunchedEffect(homeRequest) {
        if (returningHome) {
            appPendingLaunch = null
            currentScreenName = LauncherScreen.HOME.name
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            // Keep the drawer inactive until the pager has actually settled on Home.
            // A scheduled remeasure alone can briefly reactivate bottom search on resume.
            pagerState.scrollToPage(0)
            handledHomeRequest = homeRequest
        }
    }

    BackHandler(enabled = currentScreen != LauncherScreen.HOME || pagerState.currentPage != 0 || pagerState.currentPageOffsetFraction != 0f, onBack = openHome)

    BackHandler(enabled = appPendingLaunch != null, onBack = openHome)

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
                androidx.compose.animation.AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (androidx.compose.animation.fadeIn(LauncherMotion.fade()) +
                            androidx.compose.animation.slideInHorizontally(LauncherMotion.settle()) { direction * it / 12 }) togetherWith
                            (androidx.compose.animation.fadeOut(LauncherMotion.fade()) +
                                androidx.compose.animation.slideOutHorizontally(LauncherMotion.settle()) { -direction * it / 18 })
                    },
                    label = "Screen Transition"
                ) { screen ->
                    when (screen) {
                        LauncherScreen.HOME -> HorizontalPager(
                            state = pagerState,
                            flingBehavior = PagerDefaults.flingBehavior(pagerState, snapAnimationSpec = LauncherMotion.settle()),
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            userScrollEnabled = appPendingLaunch == null,
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
                                    homeRequest = homeRequest,
                                    isActive = !returningHome && currentScreen == LauncherScreen.HOME && appPendingLaunch == null &&
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
                AnimatedVisibility(
                    visible = appPendingLaunch != null,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(LauncherMotion.fade()) + scaleIn(LauncherMotion.settle(), initialScale = 0.98f),
                    exit = fadeOut(LauncherMotion.fade()) + scaleOut(LauncherMotion.settle(), targetScale = 0.98f),
                ) {
                    lastPendingApp?.let { app ->
                        IntentionalPilotScreen(
                            app = app,
                            onLaunchApp = {
                                if (appPendingLaunch != null) {
                                    viewModel.launchApp(app)
                                    appPendingLaunch = null
                                }
                            },
                            onCancel = openHome,
                        )
                    }
                }
            }
        }
    }
}
