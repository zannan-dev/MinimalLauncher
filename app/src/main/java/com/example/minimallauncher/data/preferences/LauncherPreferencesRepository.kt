package com.example.minimallauncher.data.preferences

import kotlinx.coroutines.flow.Flow

interface LauncherPreferencesRepository {
    val preferences: Flow<LauncherPreferences>

    suspend fun setUse24HourClock(enabled: Boolean)
    suspend fun setShowDate(enabled: Boolean)
    suspend fun setAutoOpenKeyboard(enabled: Boolean)
    suspend fun setDoubleTapToLock(enabled: Boolean)
    suspend fun setShowStatusBar(enabled: Boolean)
    suspend fun setIntentionalPilotEnabled(enabled: Boolean)
    suspend fun setTheme(theme: ThemePreference)
    suspend fun toggleFavorite(appKey: String)
    suspend fun toggleIntentionalPilotApp(appKey: String)
    suspend fun setIntentionalPilotDelaySeconds(seconds: Int)
    suspend fun setFlowZoneEnabled(enabled: Boolean)
    suspend fun setFlowZoneFocusMinutes(minutes: Int)
    suspend fun setFlowZoneBreakMinutes(minutes: Int)
    suspend fun setFlowZoneLongBreakMinutes(minutes: Int)
}
