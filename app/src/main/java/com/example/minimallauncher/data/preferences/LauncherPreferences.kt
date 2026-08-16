package com.example.minimallauncher.data.preferences

/** User-controlled presentation preferences kept locally on the device. */
data class LauncherPreferences(
    val use24HourClock: Boolean,
    val showDate: Boolean,
    val autoOpenKeyboard: Boolean,
    val theme: ThemePreference,
    val favoriteAppKeys: Set<String>,
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
