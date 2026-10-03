package com.example.minimallauncher.ui.theme

import androidx.compose.animation.animateColorAsState
import com.example.minimallauncher.ui.motion.LauncherMotion
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.minimallauncher.data.preferences.ThemePreference

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A1C1B),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFCF9F4),
    onBackground = Color(0xFF1B1C19),
    surface = Color(0xFFFCF9F4),
    onSurface = Color(0xFF1B1C19),
    surfaceVariant = Color(0xFFE4E2DC),
    onSurfaceVariant = Color(0xFF474742),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE3E3DC),
    onPrimary = Color(0xFF30312E),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF474742),
    onSurfaceVariant = Color(0xFFC8C6C0),
)

@Composable
fun LauncherTheme(preference: ThemePreference, content: @Composable () -> Unit) {
    val darkTheme = when (preference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val target = if (darkTheme) DarkColors else LightColors
    val colors = target.copy(
        primary = animateColorAsState(target.primary, LauncherMotion.color(), label = "Primary color").value,
        onPrimary = animateColorAsState(target.onPrimary, LauncherMotion.color(), label = "Primary text").value,
        background = animateColorAsState(target.background, LauncherMotion.color(), label = "Background color").value,
        onBackground = animateColorAsState(target.onBackground, LauncherMotion.color(), label = "Background text").value,
        surface = animateColorAsState(target.surface, LauncherMotion.color(), label = "Surface color").value,
        onSurface = animateColorAsState(target.onSurface, LauncherMotion.color(), label = "Surface text").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, LauncherMotion.color(), label = "Secondary surface").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, LauncherMotion.color(), label = "Secondary text").value,
    )
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
