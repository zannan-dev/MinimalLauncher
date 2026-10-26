package com.example.minimallauncher.data.preferences

import com.example.minimallauncher.domain.MAX_RECENT_SEARCH_RESULTS
import com.example.minimallauncher.domain.RecentSearchResult
import org.json.JSONArray
import org.json.JSONObject

internal fun encodeRecentSearchResults(results: List<RecentSearchResult>): String = JSONArray().apply {
    results.distinctBy { it.identity }.take(MAX_RECENT_SEARCH_RESULTS).forEach { result ->
        put(JSONObject().apply {
            when (result) {
                is RecentSearchResult.App -> {
                    put("type", "app")
                    put("appKey", result.appKey)
                }
                is RecentSearchResult.Contact -> {
                    put("type", "contact")
                    put("uri", result.lookupUri)
                    put("title", result.title)
                }
                is RecentSearchResult.Setting -> {
                    put("type", "setting")
                    put("key", result.key)
                    put("title", result.title)
                    put("action", result.action)
                    result.preferenceKey?.let { put("preferenceKey", it) }
                }
            }
        })
    }
}.toString()

internal fun decodeRecentSearchResults(saved: String?): List<RecentSearchResult> {
    if (saved == null) return emptyList()
    val array = try { JSONArray(saved) } catch (_: Exception) { return emptyList() }
    return (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        fun required(key: String): String = (item.get(key) as? String).orEmpty().also { require(it.isNotBlank()) }
        try {
            when (required("type")) {
                "app" -> RecentSearchResult.App(required("appKey"))
                "contact" -> {
                    val uri = required("uri")
                    require(uri.startsWith("content://com.android.contacts/contacts/lookup/"))
                    RecentSearchResult.Contact(uri, required("title"))
                }
                "setting" -> RecentSearchResult.Setting(
                    required("key"), required("title"), required("action"),
                    (item.opt("preferenceKey") as? String)?.takeIf { it.isNotBlank() },
                )
                else -> null
            }
        } catch (_: Exception) { null }
    }.distinctBy { it.identity }.take(MAX_RECENT_SEARCH_RESULTS)
}
