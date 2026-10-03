package com.example.minimallauncher.ui.apps

import com.example.minimallauncher.data.search.DeviceSearchRepository
import com.example.minimallauncher.domain.DeviceSearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.minimallauncher.ui.motion.LauncherMotion
import com.example.minimallauncher.ui.motion.launcherPressFeedback
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.domain.filterApps

@Composable
fun AppDrawerScreen(
    apps: List<LaunchableApp>,
    listState: LazyListState,
    autoOpenKeyboard: Boolean,
    isLoading: Boolean,
    failedToLoad: Boolean,
    favoriteKeys: Set<String>,
    onBack: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    isActive: Boolean = true,
    isPageMoving: Boolean = false,
    homeRequest: Long = 0L,
    deviceSearch: DeviceSearchRepository? = null,
    contactsAllowed: Boolean = false,
    contactsRevision: Int = 0,
    onRequestContacts: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredApps = remember(apps, query) { filterApps(apps, query) }
    val context = LocalContext.current
    val settingsResults = remember(deviceSearch, query) { deviceSearch?.settings(query).orEmpty() }
    var contactQuery by remember { mutableStateOf("") }
    var contactResults by remember { mutableStateOf(emptyList<DeviceSearchResult>()) }
    var contactsLoading by remember { mutableStateOf(false) }
    var contactsFailed by remember { mutableStateOf(false) }
    val visibleContacts = if (contactsAllowed && contactQuery == query) contactResults else emptyList()
    LaunchedEffect(query, contactsAllowed, contactsRevision, isActive) {
        contactResults = emptyList()
        contactsFailed = false
        contactQuery = query
        contactsLoading = deviceSearch != null && contactsAllowed && query.isNotBlank() && isActive
        if (contactsLoading) {
            try {
                delay(120)
                contactResults = deviceSearch!!.contacts(query)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                contactsFailed = true
            }
            contactsLoading = false
        }
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var optionsAppKey by rememberSaveable { mutableStateOf<String?>(null) }

    var searchFocused by remember { mutableStateOf(false) }
    var focusOnReturnToTop by remember { mutableStateOf(false) }
    var focusOnEntry by remember { mutableStateOf(false) }
    var resumeSearchOnReturn by rememberSaveable { mutableStateOf(false) }
    var leftForSearchResult by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerActive by rememberUpdatedState(isActive)
    val latestHomeRequest by rememberUpdatedState(homeRequest)
    var launchHomeRequest by rememberSaveable { mutableStateOf(homeRequest) }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val isAtTop by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    val isListDragged by listState.interactionSource.collectIsDraggedAsState()
    val dismissSearch = {
        focusOnReturnToTop = false
        if (searchFocused) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
    val launchApp: (LaunchableApp) -> Unit = { app ->
        resumeSearchOnReturn = query.isNotBlank()
        launchHomeRequest = homeRequest
        leftForSearchResult = false
        dismissSearch()
        onLaunchApp(app)
    }

    val launchDeviceResult: (DeviceSearchResult) -> Unit = { result ->
        resumeSearchOnReturn = query.isNotBlank()
        launchHomeRequest = homeRequest
        leftForSearchResult = false
        dismissSearch()
        if (deviceSearch?.open(result) != true) {
            resumeSearchOnReturn = false
            focusOnEntry = query.isNotBlank()
            Toast.makeText(context, "Could not open ${result.title}", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (resumeSearchOnReturn) leftForSearchResult = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (resumeSearchOnReturn && leftForSearchResult) {
                        resumeSearchOnReturn = false
                        leftForSearchResult = false
                        if (drawerActive && query.isNotBlank() && launchHomeRequest == latestHomeRequest) {
                            focusOnEntry = true
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(homeRequest) {
        if (launchHomeRequest != homeRequest) {
            resumeSearchOnReturn = false
            leftForSearchResult = false
            focusOnEntry = false
            dismissSearch()
            launchHomeRequest = homeRequest
        }
    }

    // A completed drawer entry is independent of insets, filtering, and list movement.
    LaunchedEffect(isActive, autoOpenKeyboard) {
        focusOnReturnToTop = false
        focusOnEntry = isActive && autoOpenKeyboard
        if (!isActive) {
            resumeSearchOnReturn = false
            leftForSearchResult = false
            dismissSearch()
        }
    }
    LaunchedEffect(focusOnEntry, isPageMoving, windowFocused, isActive) {
        if (focusOnEntry && isActive && !isPageMoving && windowFocused) {
            listState.stopScroll()
            focusOnEntry = false
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    LaunchedEffect(isPageMoving) {
        if (isPageMoving) dismissSearch()
    }

    // Only a user gesture can dismiss search or arm automatic focus at the top.
    // Filtering, IME resizing, and restoring scroll position are not gestures.
    val searchScrollConnection = remember(listState, isActive, autoOpenKeyboard) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isActive && source == NestedScrollSource.UserInput && available.y != 0f) {
                    if (searchFocused) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                    if (autoOpenKeyboard && (!isAtTop || available.y < 0f)) {
                        focusOnReturnToTop = true
                    }
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(isActive, autoOpenKeyboard, focusOnReturnToTop, isAtTop,
        listState.isScrollInProgress, isListDragged) {
        if (isActive && autoOpenKeyboard && focusOnReturnToTop && isAtTop &&
            !listState.isScrollInProgress && !isListDragged && !isPageMoving) {
            focusOnReturnToTop = false
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    val searchInset by animateDpAsState(
        targetValue = if (searchFocused) 16.dp else 32.dp,
        animationSpec = LauncherMotion.settle(),
        label = "Search pill width",
    )
    val searchHeight by animateDpAsState(
        targetValue = if (searchFocused) 60.dp else 56.dp,
        animationSpec = LauncherMotion.settle(),
        label = "Search pill height",
    )
    // An opaque, theme-derived fill keeps text underneath from showing through the pill.
    val searchFill = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant, 0.65f)
    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        when {
            isLoading && apps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            failedToLoad && apps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Could not load installed apps")
            }
            filteredApps.isEmpty() && visibleContacts.isEmpty() && settingsResults.isEmpty() &&
                !contactsLoading && !contactsFailed && (deviceSearch == null || contactsAllowed || query.isBlank()) -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (deviceSearch == null) "No matching apps" else "No results")
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(searchScrollConnection),
                // Rows can pass beneath the floating controls; the last row can still scroll clear.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
            ) {
                if (deviceSearch != null && query.isNotBlank() && filteredApps.isNotEmpty()) {
                    item(key = "section:apps") { SearchSectionLabel("Apps") }
                }
                items(filteredApps, key = { app -> "app:${app.key}" }) { app ->
                    AppDrawerRow(
                        app = app,
                        isFavorite = app.key in favoriteKeys,
                        showOptions = optionsAppKey == app.key,
                        onToggleOptions = { show ->
                            optionsAppKey = if (show) app.key else null
                        },
                        onLaunchApp = launchApp,
                        onToggleFavorite = onToggleFavorite,
                        modifier = Modifier.animateItem(
                            fadeInSpec = LauncherMotion.fade(),
                            placementSpec = LauncherMotion.settle(),
                            fadeOutSpec = LauncherMotion.fade(),
                        ),
                    )
                }
                if (query.isNotBlank() && deviceSearch != null) {
                    if (visibleContacts.isNotEmpty() || !contactsAllowed || contactsLoading || contactsFailed) {
                        item(key = "section:contacts") { SearchSectionLabel("Contacts") }
                    }
                    items(visibleContacts, key = { it.key }) { result ->
                        DeviceResultRow(result, onClick = { launchDeviceResult(result) })
                    }
                    if (!contactsAllowed) {
                        item(key = "contacts:permission") {
                            DeviceResultRow(
                                DeviceSearchResult("permission", "Search contacts", "Allow contacts access", ""),
                                onClick = onRequestContacts,
                            )
                        }
                    } else if (contactsLoading) {
                        item(key = "contacts:loading") { Text("Searching contacts…", Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else if (contactsFailed) {
                        item(key = "contacts:error") { Text("Contacts unavailable", Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    if (settingsResults.isNotEmpty()) {
                        item(key = "section:settings") { SearchSectionLabel("Settings") }
                        items(settingsResults, key = { it.key }) { result ->
                            DeviceResultRow(result, onClick = { launchDeviceResult(result) })
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(140.dp)
                .background(Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to Color.Black.copy(alpha = 0.12f),
                    1f to Color.Black.copy(alpha = 0.5f),
                )),
        )
        TextField(
            value = query,
            onValueChange = {
                focusOnReturnToTop = false
                resumeSearchOnReturn = false
                leftForSearchResult = false
                query = it
                listState.requestScrollToItem(0)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            shape = RoundedCornerShape(percent = 50),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(if (deviceSearch == null) "Search apps..." else "Search…", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = searchFill,
                unfocusedContainerColor = searchFill,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (query.isEmpty()) {
                        dismissSearch()
                        onBack()
                    } else {
                        when {
                            filteredApps.isNotEmpty() -> launchApp(filteredApps.first())
                            visibleContacts.isNotEmpty() -> launchDeviceResult(visibleContacts.first())
                            settingsResults.isNotEmpty() -> launchDeviceResult(settingsResults.first())
                        }
                    }
                }
            ),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = searchInset, vertical = 16.dp).height(searchHeight)
                .onFocusChanged {
                    searchFocused = it.isFocused
                    if (it.isFocused) focusOnReturnToTop = false
                }
                .focusRequester(focusRequester),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDrawerRow(
    app: LaunchableApp,
    isFavorite: Boolean,
    showOptions: Boolean,
    onToggleOptions: (Boolean) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val interactions = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .launcherPressFeedback(interactions)
            .combinedClickable(
                interactionSource = interactions,
                indication = androidx.compose.material3.ripple(),
                onClick = {
                    if (showOptions) {
                        onToggleOptions(false)
                    } else {
                        onLaunchApp(app)
                    }
                },
                onLongClick = { onToggleOptions(true) }
            )
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 16.dp, end = 16.dp),
        )

        AnimatedVisibility(
            visible = showOptions,
            enter = fadeIn(LauncherMotion.fade()) + expandHorizontally(LauncherMotion.settle(), expandFrom = Alignment.End),
            exit = fadeOut(LauncherMotion.fade()) + shrinkHorizontally(LauncherMotion.settle(), shrinkTowards = Alignment.End),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    onToggleFavorite(app)
                    onToggleOptions(false)
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    )
                }
                IconButton(onClick = {
                    onToggleOptions(false)
                    try {
                        val launcherApps = context.getSystemService(android.content.Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                        val component = ComponentName(app.packageName, app.activityName)
                        launcherApps.startAppDetailsActivity(component, app.userHandle, null, null)
                    } catch (_: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", app.packageName, null)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info")
                }

                if (!app.isSystemApp) {
                    IconButton(onClick = {
                        onToggleOptions(false)
                        try {
                            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                            intent.data = Uri.fromParts("package", app.packageName, null)
                            intent.putExtra(Intent.EXTRA_USER, app.userHandle)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Uninstall")
                    }
                }

                IconButton(onClick = { onToggleOptions(false) }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }
}

@Composable
private fun SearchSectionLabel(label: String) {
    Text(label, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DeviceResultRow(result: DeviceSearchResult, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().heightIn(min = 64.dp).launcherPressFeedback(interaction)
        .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(result.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(result.detail, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
