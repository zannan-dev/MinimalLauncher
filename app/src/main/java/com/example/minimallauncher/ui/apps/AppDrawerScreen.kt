package com.example.minimallauncher.ui.apps

import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.clickable
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
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredApps = remember(apps, query) { filterApps(apps, query) }
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
        leftForSearchResult = false
        dismissSearch()
        onLaunchApp(app)
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
                        if (drawerActive && query.isNotBlank()) focusOnEntry = true
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
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
            textStyle = MaterialTheme.typography.titleLarge,
            placeholder = { Text("Search apps...", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
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
                        filteredApps.firstOrNull()?.let(launchApp)
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                .onFocusChanged {
                    searchFocused = it.isFocused
                    if (it.isFocused) focusOnReturnToTop = false
                }
                .focusRequester(focusRequester),
        )
        when {
            isLoading && apps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            failedToLoad && apps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Could not load installed apps")
            }
            filteredApps.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matching apps")
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).nestedScroll(searchScrollConnection).padding(top = 8.dp)
            ) {
                items(filteredApps, key = { app -> app.key }) { app ->
                    AppDrawerRow(
                        app = app,
                        isFavorite = app.key in favoriteKeys,
                        showOptions = optionsAppKey == app.key,
                        onToggleOptions = { show ->
                            optionsAppKey = if (show) app.key else null
                        },
                        onLaunchApp = launchApp,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }
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
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(
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

        if (showOptions) {
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
