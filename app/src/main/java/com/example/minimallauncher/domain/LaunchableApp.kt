package com.example.minimallauncher.domain

/**
 * Metadata needed to show and launch an installed application. Keeping this model
 * Android-free makes filtering, sorting, and favorites straightforward to test.
 */
data class LaunchableApp(
    val packageName: String,
    val activityName: String,
    val label: String,
) {
    val key: String
        get() = "$packageName/$activityName"
}
