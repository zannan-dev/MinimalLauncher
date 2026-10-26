package com.example.minimallauncher.data.preferences

import com.example.minimallauncher.domain.RecentSearchResult

/** User-controlled presentation preferences kept locally on the device. */
data class LauncherPreferences(
    val showDate: Boolean,
    val autoOpenKeyboard: Boolean,
    val theme: ThemePreference,
    val favoriteAppKeys: Set<String>,
    val favoriteAppOrder: List<String>,
    val recentSearchResults: List<RecentSearchResult> = emptyList(),
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
