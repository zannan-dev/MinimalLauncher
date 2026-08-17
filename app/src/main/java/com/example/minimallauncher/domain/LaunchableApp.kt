package com.example.minimallauncher.domain

import android.os.UserHandle

/**
 * Metadata needed to show and launch an installed application or shortcut. Keeping this model
 * UI-free makes filtering, sorting, and favorites straightforward to test.
 */
data class LaunchableApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val userHandle: UserHandle,
    val isPinnedShortcut: Boolean,
    val isSystemApp: Boolean = false,
) {
    val key = "${packageName}_${activityName}_${userHandle.hashCode()}_${isPinnedShortcut}"
}
