package com.example.minimallauncher.ui.components

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.minimallauncher.domain.LaunchableApp

/** Loads an icon once per visible app row rather than retaining all installed icons in memory. */
@Composable
fun AppIcon(app: LaunchableApp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawable = remember(app.key) {
        runCatching {
            context.packageManager.getActivityIcon(ComponentName(app.packageName, app.activityName))
        }.getOrNull()
    }
    val icon = remember(drawable) { drawable?.toBitmap(width = 96, height = 96)?.asImageBitmap() }

    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
