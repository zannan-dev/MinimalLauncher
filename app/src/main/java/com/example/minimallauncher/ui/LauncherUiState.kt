package com.example.minimallauncher.ui

import com.example.minimallauncher.data.preferences.LauncherPreferences
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp
import com.example.minimallauncher.domain.MAX_HOME_FAVORITES

data class LauncherUiState(
    val apps: List<LaunchableApp> = emptyList(),
    val preferences: LauncherPreferences = LauncherPreferences(
        showDate = true,
        autoOpenKeyboard = true,
        theme = ThemePreference.SYSTEM,
        favoriteAppKeys = emptySet(),
        favoriteAppOrder = emptyList(),
    ),
    val isLoadingApps: Boolean = true,
    val appLoadError: Boolean = false,
) {
    val favoriteApps: List<LaunchableApp>
        get() {
            val appsByKey = apps.associateBy { it.key }
            return preferences.favoriteAppOrder.mapNotNull(appsByKey::get).take(MAX_HOME_FAVORITES)
        }
}
