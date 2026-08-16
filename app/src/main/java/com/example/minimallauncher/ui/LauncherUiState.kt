package com.example.minimallauncher.ui

import com.example.minimallauncher.data.preferences.LauncherPreferences
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp

data class LauncherUiState(
    val apps: List<LaunchableApp> = emptyList(),
    val preferences: LauncherPreferences = LauncherPreferences(
        use24HourClock = false,
        showDate = true,
        theme = ThemePreference.SYSTEM,
        favoriteAppKeys = emptySet(),
    ),
    val isLoadingApps: Boolean = true,
    val appLoadError: Boolean = false,
) {
    val favoriteApps: List<LaunchableApp>
        get() = apps.filter { app -> app.key in preferences.favoriteAppKeys }
}
