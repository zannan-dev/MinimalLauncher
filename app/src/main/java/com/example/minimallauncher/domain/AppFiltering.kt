package com.example.minimallauncher.domain

/** Returns a stable, case-insensitive alphabetical ordering for the app drawer. */
fun sortApps(apps: Iterable<LaunchableApp>): List<LaunchableApp> =
    apps.sortedWith(
        compareBy<LaunchableApp, String>(String.CASE_INSENSITIVE_ORDER) { app -> app.label }
            .thenBy { it.packageName }
            .thenBy { it.activityName },
    )

/** Filters a pre-sorted app list without changing its order. */
fun filterApps(apps: Iterable<LaunchableApp>, query: String): List<LaunchableApp> {
    val normalizedQuery = query.trim()
    return if (normalizedQuery.isEmpty()) {
        apps.toList()
    } else {
        apps.filter { app -> app.label.contains(normalizedQuery, ignoreCase = true) }
    }
}
