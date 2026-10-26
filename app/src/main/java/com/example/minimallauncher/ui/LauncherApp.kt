package com.example.minimallauncher.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
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
import com.example.minimallauncher.ui.home.HomeScreen
import com.example.minimallauncher.ui.settings.SettingsScreen
import com.example.minimallauncher.ui.motion.LauncherMotion
import com.example.minimallauncher.ui.theme.LauncherTheme

private enum class LauncherScreen { HOME, SETTINGS }

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
    val currentScreen = LauncherScreen.entries.firstOrNull { it.name == currentScreenName } ?: LauncherScreen.HOME
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
    val openHome: () -> Unit = {
        setSearchVisible(false)
        currentScreenName = LauncherScreen.HOME.name
    }


    LaunchedEffect(homeRequest) {
        if (returningHome) {
            setSearchVisible(false)
            currentScreenName = LauncherScreen.HOME.name
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            handledHomeRequest = homeRequest
        }
    }

    BackHandler(enabled = currentScreen != LauncherScreen.HOME, onBack = openHome)

    BackHandler(enabled = homeSearchVisible) {
        setSearchVisible(false)
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    val view = LocalView.current
    LaunchedEffect(view) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    LauncherTheme(preference = state.preferences.theme) {
        val background = MaterialTheme.colorScheme.surface
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowInsetsControllerCompat(window, view)
                val isLight = background.luminance() > 0.5f
                controller.isAppearanceLightNavigationBars = isLight
                controller.isAppearanceLightStatusBars = isLight
            }
        }
        Surface(modifier = Modifier.fillMaxSize(), color = background) {
            val paddingModifier = Modifier.navigationBarsPadding().displayCutoutPadding()
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
                                onOpenSearch = { searchSession++; homeAllApps = false; setSearchVisible(true) },
                                onOpenNotifications = onOpenNotifications,
                                onOpenSettings = { currentScreenName = LauncherScreen.SETTINGS.name },
                                onSetDefaultLauncher = onOpenDefaultLauncherSettings,
                                onLaunchApp = viewModel::launchApp,
                                onMoveFavorite = viewModel::moveFavorite,
                                onRemoveFavorite = viewModel::removeFavorite,
                                searchVisible = searchPresent,
                                searchProgress = searchProgress,
                                gesturesEnabled = !returningHome,
                            )
                            // Retain the overlay until its closing transition completes.
                            if (searchPresent) {
                                key(searchSession) { HomeSearchScreen(
                                    apps = state.apps, listState = rememberLazyListState(),
                                    autoOpenKeyboard = !homeAllApps || state.preferences.autoOpenKeyboard,
                                    focusOnOpen = !homeAllApps, isLoading = state.isLoadingApps,
                                    failedToLoad = state.appLoadError, favoriteKeys = state.preferences.favoriteAppKeys,
                                    onBack = { setSearchVisible(false) }, onLaunchApp = viewModel::launchApp,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    isActive = homeSearchVisible && !returningHome,
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
                            theme = state.preferences.theme,
                            onBack = openHome,
                            onThemeChanged = viewModel::setTheme,
                            onOpenDefaultLauncherSettings = onOpenDefaultLauncherSettings,
                        )
                    }
                }
            }
        }
    }
}
