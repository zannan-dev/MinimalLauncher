package com.example.minimallauncher

import android.content.Intent
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import com.example.minimallauncher.data.search.AndroidDeviceSearchRepository
import com.example.minimallauncher.data.search.createDeviceSearchIntent
import com.example.minimallauncher.data.search.SETTINGS_FRAGMENT_ARGUMENTS
import com.example.minimallauncher.data.search.SETTINGS_PREFERENCE_KEY
import com.example.minimallauncher.domain.DeviceSearchResult
import org.junit.Assert.*
import org.junit.Test

class SettingsNavigationTest {
    @Test fun specificSearchResultsIncludeNativeHighlightArguments() {
        val repository = AndroidDeviceSearchRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        mapOf("build number" to "build_number", "screen timeout" to "screen_timeout").forEach { (query, key) ->
            val result = repository.settings(query).first()
            val intent = createDeviceSearchIntent(result)
            assertEquals(key, intent.getStringExtra(SETTINGS_PREFERENCE_KEY))
            assertEquals(key, intent.getBundleExtra(SETTINGS_FRAGMENT_ARGUMENTS)?.getString(SETTINGS_PREFERENCE_KEY))
            assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
            assertNotNull(intent.resolveActivity(InstrumentationRegistry.getInstrumentation().targetContext.packageManager))
        }
    }

    @Test fun genericSettingsPageHasNoPreferenceHighlight() {
        val intent = createDeviceSearchIntent(DeviceSearchResult("display", "Display", "Phone settings", Settings.ACTION_DISPLAY_SETTINGS))
        assertFalse(intent.hasExtra(SETTINGS_PREFERENCE_KEY))
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    @Test fun contactCardKeepsItsUriWithoutSettingsExtras() {
        val uri = "content://com.android.contacts/contacts/lookup/test/123"
        val intent = createDeviceSearchIntent(DeviceSearchResult("contact", "Contact", "Contact", Intent.ACTION_VIEW, uri))
        assertEquals(uri, intent.data.toString())
        assertFalse(intent.hasExtra(SETTINGS_FRAGMENT_ARGUMENTS))
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
