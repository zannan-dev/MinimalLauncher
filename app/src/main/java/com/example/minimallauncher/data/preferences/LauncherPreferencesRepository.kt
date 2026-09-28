package com.example.minimallauncher.data.preferences

import kotlinx.coroutines.flow.Flow

interface LauncherPreferencesRepository {
    val preferences: Flow<LauncherPreferences>

    suspend fun setShowDate(enabled: Boolean)
    suspend fun setAutoOpenKeyboard(enabled: Boolean)
    suspend fun setDoubleTapToLock(enabled: Boolean)
    suspend fun setShowStatusBar(enabled: Boolean)
    suspend fun setIntentionalPilotEnabled(enabled: Boolean)
    suspend fun setTheme(theme: ThemePreference)
    suspend fun toggleFavorite(appKey: String)
    suspend fun removeFavorite(appKey: String)
    suspend fun moveFavorite(fromKey: String, toKey: String)
    suspend fun toggleIntentionalPilotApp(appKey: String)
}
