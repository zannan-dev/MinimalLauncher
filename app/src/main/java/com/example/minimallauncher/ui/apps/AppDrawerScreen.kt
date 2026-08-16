package com.example.minimallauncher.ui.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.domain.filterApps

@Composable
fun AppDrawerScreen(
    apps: List<LaunchableApp>,
    favoriteKeys: Set<String>,
    isLoading: Boolean,
    failedToLoad: Boolean,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredApps by remember(apps, query) { mutableStateOf(filterApps(apps, query)) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Apps", style = MaterialTheme.typography.titleLarge)
        TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
            else -> LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                items(filteredApps, key = { app -> app.key }) { app ->
                    AppDrawerRow(
                        app = app,
                        isFavorite = app.key in favoriteKeys,
                        onLaunchApp = onLaunchApp,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDrawerRow(
    app: LaunchableApp,
    isFavorite: Boolean,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleFavorite: (LaunchableApp) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable { onLaunchApp(app) }
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 16.dp, end = 16.dp),
        )
        TextButton(onClick = { onToggleFavorite(app) }) {
            Text(if (isFavorite) "Remove" else "Add")
        }
    }
}
