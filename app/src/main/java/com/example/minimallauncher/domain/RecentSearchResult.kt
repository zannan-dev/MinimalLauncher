package com.example.minimallauncher.domain

const val MAX_RECENT_SEARCH_RESULTS = 5

/** Only destinations opened by the user are saved; queries and phone numbers are not. */
sealed interface RecentSearchResult {
    val identity: String

    data class App(val appKey: String) : RecentSearchResult {
        override val identity get() = "app:$appKey"
    }

    data class Contact(val lookupUri: String, val title: String) : RecentSearchResult {
        override val identity get() = "contact:$lookupUri"
    }

    data class Setting(
        val key: String,
        val title: String,
        val action: String,
        val preferenceKey: String? = null,
    ) : RecentSearchResult {
        override val identity get() = "setting:$key"
    }
}

fun List<RecentSearchResult>.withRecentResult(result: RecentSearchResult): List<RecentSearchResult> =
    (listOf(result) + this).distinctBy { it.identity }.take(MAX_RECENT_SEARCH_RESULTS)

fun DeviceSearchResult.asRecentResult(): RecentSearchResult =
    if (key.startsWith("contact:") && uri != null) RecentSearchResult.Contact(uri, title)
    else RecentSearchResult.Setting(key, title, action, preferenceKey)
