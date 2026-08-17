package com.example.minimallauncher.ui

import com.example.minimallauncher.data.preferences.LauncherPreferences
import com.example.minimallauncher.data.preferences.ThemePreference
import com.example.minimallauncher.domain.LaunchableApp

enum class FlowZonePhase {
    FOCUS, BREAK, LONG_BREAK
}

data class FlowZoneState(
    val phase: FlowZonePhase = FlowZonePhase.FOCUS,
    val remainingSeconds: Long = 25 * 60,
    val totalSeconds: Long = 25 * 60,
    val completedCycles: Int = 0,
    val isRunning: Boolean = false,
    val message: String? = null,
)

data class LauncherUiState(
    val apps: List<LaunchableApp> = emptyList(),
    val preferences: LauncherPreferences = LauncherPreferences(
        use24HourClock = false,
        showDate = true,
        autoOpenKeyboard = true,
        doubleTapToLock = true,
        showStatusBar = false,
        theme = ThemePreference.SYSTEM,
        favoriteAppKeys = emptySet(),
        isIntentionalPilotEnabled = false,
        intentionalPilotAppKeys = emptySet(),
        intentionalPilotDelaySeconds = 5,
        isFlowZoneEnabled = false,
        flowZoneFocusMinutes = 25,
        flowZoneBreakMinutes = 5,
        flowZoneLongBreakMinutes = 15,
    ),
    val flowZoneState: FlowZoneState = FlowZoneState(),
    val isLoadingApps: Boolean = true,
    val appLoadError: Boolean = false,
) {
    val favoriteApps: List<LaunchableApp>
        get() = apps.filter { app -> app.key in preferences.favoriteAppKeys }
}
