package com.example.minimallauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.key
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.minimallauncher.data.search.DeviceSearchRepository
import androidx.activity.compose.BackHandler
import android.app.Activity
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
import com.example.minimallauncher.ui.apps.HomeSearchScreen
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
    deviceSearch: DeviceSearchRepository? = null,
) {
    val context = LocalContext.current
    fun hasContactsPermission() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    var contactsAllowed by remember { mutableStateOf(hasContactsPermission()) }
    var contactsPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var contactsRevision by remember { mutableStateOf(0) }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsAllowed = granted
        contactsRevision++
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                contactsAllowed = hasContactsPermission()
                contactsRevision++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val requestContacts = {
        val activity = context as? Activity
        if (!contactsPermissionRequested || activity == null ||
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)) {
            contactsPermissionRequested = true
            contactsPermission.launch(Manifest.permission.READ_CONTACTS)
        } else {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")))
        }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val homeRequest by viewModel.homeRequests.collectAsState()
    var handledHomeRequest by rememberSaveable { mutableStateOf(0L) }
    val returningHome = homeRequest != handledHomeRequest
    var currentScreenName by rememberSaveable { mutableStateOf(LauncherScreen.HOME.name) }
    val currentScreen = LauncherScreen.valueOf(currentScreenName)
    val assistantExpansion = remember { mutableStateOf(false) }
    var homeSearchVisible by remember { mutableStateOf(false) }
    val searchAnimation = remember { Animatable(0f) }
    val searchScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val setSearchVisible: (Boolean) -> Unit = { visible ->
        homeSearchVisible = visible
        // Cancel at the gesture/click itself so another opening frame cannot slip
        // through before a composition effect processes the reversed target.
        searchJob?.cancel()
        searchJob = searchScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val destination = if (visible) 1f else 0f
            if (searchAnimation.value != destination) {
                searchAnimation.animateTo(destination, LauncherMotion.search(opening = visible), initialVelocity = 0f)
            }
        }
    }
    val searchProgress = searchAnimation.value
    val searchPresent = homeSearchVisible || searchProgress > 0f || searchAnimation.isRunning
    var homeAllApps by remember { mutableStateOf(false) }
    var searchSession by remember { mutableStateOf(0) }
    var searchKeyboardBottom by remember(androidx.compose.ui.platform.LocalConfiguration.current) { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var appPendingLaunch by remember { mutableStateOf<LaunchableApp?>(null) }
    var lastPendingApp by remember { mutableStateOf<LaunchableApp?>(null) }
    val openHome: () -> Unit = {
        setSearchVisible(false)
        appPendingLaunch = null
        currentScreenName = LauncherScreen.HOME.name
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
            setSearchVisible(false)
            appPendingLaunch = null
            currentScreenName = LauncherScreen.HOME.name
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            handledHomeRequest = homeRequest
        }
    }

    BackHandler(enabled = currentScreen != LauncherScreen.HOME, onBack = openHome)

    BackHandler(enabled = appPendingLaunch != null, onBack = openHome)
    BackHandler(enabled = homeSearchVisible && appPendingLaunch == null) {
        setSearchVisible(false)
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

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
            Color.Black
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
                        LauncherScreen.HOME -> Box(Modifier.fillMaxSize()) {
                            HomeScreen(
                                favoriteApps = state.favoriteApps,
                                showDate = state.preferences.showDate,
                                doubleTapToLock = state.preferences.doubleTapToLock,
                                onOpenSearch = { searchSession++; homeAllApps = false; setSearchVisible(true) },
                                onOpenNotifications = onOpenNotifications,
                                onOpenSettings = { currentScreenName = LauncherScreen.SETTINGS.name },
                                onSetDefaultLauncher = onOpenDefaultLauncherSettings,
                                onLaunchApp = handleAppLaunch,
                                onMoveFavorite = viewModel::moveFavorite,
                                onRemoveFavorite = viewModel::removeFavorite,
                                assistantExpansion = assistantExpansion,
                                searchVisible = searchPresent,
                                searchProgress = searchProgress,
                                gesturesEnabled = !returningHome && appPendingLaunch == null,
                                assistantContent = { expansionState ->
                                    com.example.minimallauncher.ui.assistant.HomeAssistant(
                                        apps = state.apps,
                                        deviceSearch = deviceSearch,
                                        onLaunchApp = handleAppLaunch,
                                        isActive = !searchPresent && !returningHome && currentScreen == LauncherScreen.HOME && appPendingLaunch == null,
                                        homeRequest = homeRequest,
                                        expansionState = expansionState,
                                    )
                                },
                            )
                            // Retain the overlay until its closing transition completes.
                            if (searchPresent) {
                                key(searchSession) { HomeSearchScreen(
                                    apps = state.apps, listState = rememberLazyListState(),
                                    autoOpenKeyboard = !homeAllApps || state.preferences.autoOpenKeyboard,
                                    focusOnOpen = !homeAllApps, isLoading = state.isLoadingApps,
                                    failedToLoad = state.appLoadError, favoriteKeys = state.preferences.favoriteAppKeys,
                                    onBack = { setSearchVisible(false) }, onLaunchApp = handleAppLaunch,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    isActive = homeSearchVisible && !returningHome && appPendingLaunch == null,
                                    homeRequest = homeRequest, deviceSearch = deviceSearch,
                                    contactsAllowed = contactsAllowed, contactsRevision = contactsRevision,
                                    onRequestContacts = requestContacts, animateFromHome = true,
                                    showAllApps = homeAllApps, onShowAllApps = { homeAllApps = true },
                                    homeSearchProgress = searchProgress,
                                    previousKeyboardBottom = searchKeyboardBottom,
                                    onKeyboardPosition = { searchKeyboardBottom = it },
                                ) }
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
