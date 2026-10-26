package com.example.minimallauncher.data.preferences

import com.example.minimallauncher.domain.RecentSearchResult
import org.junit.Assert.*
import org.junit.Test

class RecentSearchStorageTest {
    @Test fun allDestinationTypesRoundTripWithoutLosingOrder() {
        val results = listOf(
            RecentSearchResult.Contact("content://com.android.contacts/contacts/lookup/a/1", "Alex"),
            RecentSearchResult.App("package_activity_profile_shortcut"),
            RecentSearchResult.Setting("settings:build", "Build number", "android.settings.DEVICE_INFO_SETTINGS", "build_number"),
        )
        assertEquals(results, decodeRecentSearchResults(encodeRecentSearchResults(results)))
    }

    @Test fun emptyOrMalformedStorageDoesNotBreakPreferences() {
        listOf(null, "", "not json", "{}", "[null, 3, {}, {\"type\":\"unknown\"}]").forEach {
            assertEquals(emptyList<RecentSearchResult>(), decodeRecentSearchResults(it))
        }
    }

    @Test fun malformedEntriesAreSkippedAndValidEntriesSurvive() {
        val saved = """[{"type":"app","appKey":"one"}, {"type":"app","appKey":5},
            {"type":"contact","uri":"https://example.com","title":"Invalid"},
            {"type":"setting","title":"Missing action"}, {"type":"app","appKey":"two"}]"""
        assertEquals(listOf(RecentSearchResult.App("one"), RecentSearchResult.App("two")), decodeRecentSearchResults(saved))
    }

    @Test fun duplicateAndOversizedHistoryIsNormalized() {
        val saved = """[{"type":"app","appKey":"one"}, {"type":"app","appKey":"one"},
            {"type":"app","appKey":"two"}, {"type":"app","appKey":"three"},
            {"type":"app","appKey":"four"}, {"type":"app","appKey":"five"},
            {"type":"app","appKey":"six"}]"""
        val restored = decodeRecentSearchResults(saved)
        assertEquals(5, restored.size)
        assertEquals(RecentSearchResult.App("one"), restored.first())
        assertEquals(RecentSearchResult.App("five"), restored.last())
    }
}
