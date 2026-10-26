package com.example.minimallauncher.data.preferences

import com.example.minimallauncher.domain.RecentSearchResult
import kotlinx.coroutines.flow.Flow

interface LauncherPreferencesRepository {
    val preferences: Flow<LauncherPreferences>

    suspend fun recordRecentSearchResult(result: RecentSearchResult)
    suspend fun clearRecentSearchResults()

    suspend fun setShowDate(enabled: Boolean)
    suspend fun setAutoOpenKeyboard(enabled: Boolean)
    suspend fun setTheme(theme: ThemePreference)
    suspend fun toggleFavorite(appKey: String)
    suspend fun removeFavorite(appKey: String)
    suspend fun moveFavorite(fromKey: String, toKey: String)
}
