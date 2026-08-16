package com.example.minimallauncher.domain

import java.text.Collator

/** Returns a stable, case-insensitive, locale-aware alphabetical ordering for the app drawer. */
fun sortApps(apps: Iterable<LaunchableApp>): List<LaunchableApp> {
    val collator = Collator.getInstance()
    return apps.sortedWith(
        compareBy<LaunchableApp, String>(collator) { app -> app.label }
            .thenBy { it.packageName }
            .thenBy { it.activityName },
    )
}

/** Filters a pre-sorted app list without changing its order. */
fun filterApps(apps: Iterable<LaunchableApp>, query: String): List<LaunchableApp> {
    val normalizedQuery = query.trim()
    return if (normalizedQuery.isEmpty()) {
        apps.toList()
    } else {
        apps.filter { app -> app.label.contains(normalizedQuery, ignoreCase = true) }
    }
}
