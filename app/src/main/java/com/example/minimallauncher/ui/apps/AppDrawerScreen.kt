package com.example.minimallauncher.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.domain.filterApps

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDrawerScreen(
    apps: List<LaunchableApp>,
    autoOpenKeyboard: Boolean,
    isLoading: Boolean,
    failedToLoad: Boolean,
    onBack: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredApps by remember(apps, query) { mutableStateOf(filterApps(apps, query)) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible
    var wasImeVisible by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var optionsAppKey by rememberSaveable { mutableStateOf<String?>(null) }

    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(Unit) {
        if (autoOpenKeyboard) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop && autoOpenKeyboard && !isImeVisible) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isAtTop) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            wasImeVisible = true
        } else if (wasImeVisible) {
            // focusManager.clearFocus() // Remove this line
            wasImeVisible = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search apps") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (query.isEmpty()) {
                        onBack()
                    } else {
                        filteredApps.firstOrNull()?.let { onLaunchApp(it) }
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(focusRequester),
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
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            ) {
                items(filteredApps, key = { app -> app.key }) { app ->
                    AppDrawerRow(
                        app = app,
                        showOptions = optionsAppKey == app.key,
                        onToggleOptions = { show ->
                            optionsAppKey = if (show) app.key else null
                        },
                        onLaunchApp = onLaunchApp,
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
    showOptions: Boolean,
    onToggleOptions: (Boolean) -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
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
