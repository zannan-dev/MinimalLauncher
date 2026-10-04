package com.example.minimallauncher.domain

import java.text.Collator
import java.text.Normalizer

/** Returns a stable, case-insensitive, locale-aware alphabetical ordering for the app list. */
fun sortApps(apps: Iterable<LaunchableApp>): List<LaunchableApp> {
    val collator = Collator.getInstance()
    return apps.sortedWith(
        compareBy<LaunchableApp, String>(collator) { app -> app.label }
            .thenBy { it.packageName }
            .thenBy { it.activityName },
    )
}

private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
private val separatorsRegex = Regex("[-_+,.`'\\s\\p{Z}]")

internal fun CharSequence.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(diacriticsRegex, "")
        .replace(separatorsRegex, "")

/** Filters a pre-sorted app list without changing its order, ignoring spaces, punctuation, and diacritics. */
fun filterApps(apps: Iterable<LaunchableApp>, query: String): List<LaunchableApp> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return if (apps is List) apps else apps.toList()
    
    val searchNormalized = query.normalizeForSearch()
    
    return apps.filter { app -> 
        if (app.label.contains(normalizedQuery, ignoreCase = true)) {
            true
        } else {
            searchNormalized.isNotEmpty() && app.searchableLabel.contains(searchNormalized, ignoreCase = true)
        }
    }
}
