package com.example.minimallauncher

import androidx.test.platform.app.InstrumentationRegistry
import com.example.minimallauncher.data.search.AndroidDeviceSearchRepository
import com.example.minimallauncher.domain.RecentSearchResult
import com.example.minimallauncher.domain.asRecentResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RecentDestinationResolutionTest {
    private val repository = AndroidDeviceSearchRepository(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test fun savedSettingResolvesWithItsOriginalHighlight() = runBlocking {
        val original = repository.settings("build number").first { it.title == "Build number" }
        val resolved = repository.resolveRecent(original.asRecentResult())
        assertNotNull(resolved)
        assertEquals(original.key, resolved?.key)
        assertEquals(original.action, resolved?.action)
        assertEquals("build_number", resolved?.preferenceKey)
    }

    @Test fun missingContactAndUnavailableSettingAreOmitted() = runBlocking {
        assertNull(repository.resolveRecent(RecentSearchResult.Contact(
            "content://com.android.contacts/contacts/lookup/minimal_launcher_test_missing/9223372036854775807", "Missing",
        )))
        assertNull(repository.resolveRecent(RecentSearchResult.Setting("removed", "Removed", "android.settings.SETTINGS")))
    }

    @Test fun nonContactUrisAreRejectedBeforeReadingAnyProvider() = runBlocking {
        assertNull(repository.resolveRecent(RecentSearchResult.Contact("content://other.provider/item", "Invalid")))
        assertNull(repository.resolveRecent(RecentSearchResult.Contact("https://example.com", "Invalid")))
    }
}
