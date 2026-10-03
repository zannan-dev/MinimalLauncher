package com.example.minimallauncher.ui.apps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import com.example.minimallauncher.ui.motion.LauncherMotion
import com.example.minimallauncher.ui.motion.launcherPressFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.domain.LaunchableApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionalPilotAppSelectionScreen(
    apps: List<LaunchableApp>,
    selectedAppKeys: Set<String>,
    onToggleApp: (LaunchableApp) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Selected apps will ask you to pause and consider why you want to open them.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(apps, key = { it.key }) { app ->
                    val interactions = remember(app.key) { MutableInteractionSource() }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .animateItem(placementSpec = LauncherMotion.settle())
                            .fillMaxWidth()
                            .launcherPressFeedback(interactions)
                            .toggleable(value = app.key in selectedAppKeys,
                                interactionSource = interactions, indication = androidx.compose.material3.ripple(),
                                role = Role.Checkbox, onValueChange = { onToggleApp(app) })
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(16.dp))
                        Checkbox(
                            checked = app.key in selectedAppKeys,
                            onCheckedChange = null
                        )
                    }
                }
            }
        }
    }
}
