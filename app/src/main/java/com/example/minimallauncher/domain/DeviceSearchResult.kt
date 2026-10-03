package com.example.minimallauncher.domain

/** A local search destination outside the installed app list. */
data class DeviceSearchResult(
    val key: String,
    val title: String,
    val detail: String,
    val action: String,
    val uri: String? = null,
    val preferenceKey: String? = null,
)

data class SettingsSearchTarget(
    val title: String,
    val action: String,
    val keywords: List<String>,
    val detail: String = "Phone settings",
    val preferenceKey: String? = null,
)

fun filterSettingsTargets(targets: List<SettingsSearchTarget>, query: String): List<SettingsSearchTarget> {
    val needle = query.trim().normalizeForSearch().lowercase(java.util.Locale.ROOT)
    if (needle.isEmpty()) return emptyList()
    return targets.filter { target ->
        (listOf(target.title) + target.keywords).any {
            it.normalizeForSearch().lowercase(java.util.Locale.ROOT).contains(needle)
        }
    }.sortedBy { target ->
        val title = target.title.normalizeForSearch().lowercase(java.util.Locale.ROOT)
        when {
            title == needle -> 0
            title.startsWith(needle) -> 1
            else -> 2
        }
    }
}
