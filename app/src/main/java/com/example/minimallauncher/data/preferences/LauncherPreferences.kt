package com.example.minimallauncher.data.preferences

/** User-controlled presentation preferences kept locally on the device. */
data class LauncherPreferences(
    val use24HourClock: Boolean,
    val showDate: Boolean,
    val autoOpenKeyboard: Boolean,
    val doubleTapToLock: Boolean,
    val showStatusBar: Boolean,
    val theme: ThemePreference,
    val favoriteAppKeys: Set<String>,
    val isIntentionalPilotEnabled: Boolean,
    val intentionalPilotAppKeys: Set<String>,
    val intentionalPilotDelaySeconds: Int,
)

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStorage(value: String?): ThemePreference =
            entries.firstOrNull { theme -> theme.name == value } ?: SYSTEM
    }
}
