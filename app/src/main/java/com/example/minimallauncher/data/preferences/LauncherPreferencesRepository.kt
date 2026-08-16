package com.example.minimallauncher.data.preferences

import kotlinx.coroutines.flow.Flow

interface LauncherPreferencesRepository {
    val preferences: Flow<LauncherPreferences>

    suspend fun setUse24HourClock(enabled: Boolean)
    suspend fun setShowDate(enabled: Boolean)
    suspend fun setAutoOpenKeyboard(enabled: Boolean)
    suspend fun setTheme(theme: ThemePreference)
    suspend fun toggleFavorite(appKey: String)
}
