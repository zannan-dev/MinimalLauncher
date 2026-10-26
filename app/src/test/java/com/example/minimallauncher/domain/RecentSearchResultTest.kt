package com.example.minimallauncher.domain

import org.junit.Assert.*
import org.junit.Test

class RecentSearchResultTest {
    @Test fun `newest result is first and the oldest is dropped at five`() {
        val results = (1..7).fold(emptyList<RecentSearchResult>()) { history, index ->
            history.withRecentResult(RecentSearchResult.App("app$index"))
        }
        assertEquals((7 downTo 3).map { RecentSearchResult.App("app$it") }, results)
    }

    @Test fun `reopening a result moves it to top without duplicates and refreshes its name`() {
        val contact = RecentSearchResult.Contact("content://com.android.contacts/contacts/lookup/a/1", "Old name")
        val history = listOf(RecentSearchResult.App("app"), contact)
        val updated = contact.copy(title = "New name")
        assertEquals(listOf(updated, history.first()), history.withRecentResult(updated))
    }

    @Test fun `different destination types do not collide`() {
        val app = RecentSearchResult.App("same")
        val setting = RecentSearchResult.Setting("same", "Setting", "settings")
        assertEquals(listOf(setting, app), listOf(app).withRecentResult(setting))
    }

    @Test fun `contact history contains no phone number and settings keep their highlight`() {
        val uri = "content://com.android.contacts/contacts/lookup/a/1"
        val contact = DeviceSearchResult("contact:$uri", "Alex", "+1234567890", "view", uri)
        assertEquals(RecentSearchResult.Contact(uri, "Alex"), contact.asRecentResult())
        val setting = DeviceSearchResult("settings:build", "Build number", "About phone", "settings", preferenceKey = "build_number")
        assertEquals(RecentSearchResult.Setting("settings:build", "Build number", "settings", "build_number"), setting.asRecentResult())
    }
}
