package com.example.minimallauncher.ui.home

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.domain.LaunchableApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    use24HourClock: Boolean,
    showDate: Boolean,
    favorites: List<LaunchableApp>,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (isActive) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    var upwardDrag by remember { mutableStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onOpenDrawer) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        upwardDrag += dragAmount
                        if (upwardDrag < -72f) {
                            onOpenDrawer()
                            upwardDrag = 0f
                        }
                    },
                    onDragEnd = { upwardDrag = 0f },
                )
            }
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text(
            text = now.format(
                DateTimeFormatter.ofPattern(if (use24HourClock) "HH:mm" else "h:mm", Locale.getDefault()),
            ),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.semantics { contentDescription = "Current time" },
        )
        if (showDate) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = now.format(DateTimeFormatter.ofPattern("EEEE\nd MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(32.dp))

        if (favorites.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text("Add favorites from the app drawer", textAlign = TextAlign.Center)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
                modifier = Modifier.weight(1f),
            ) {
                items(favorites, key = { app -> app.key }) { app ->
                    TextButton(
                        onClick = { onLaunchApp(app) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                    ) {
                        Text(
                            text = app.label,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        TextButton(onClick = onOpenDrawer, modifier = Modifier.height(48.dp)) {
            Text("All apps")
        }
        TextButton(onClick = onOpenSettings, modifier = Modifier.height(48.dp)) {
            Text("Settings")
        }
    }
}
