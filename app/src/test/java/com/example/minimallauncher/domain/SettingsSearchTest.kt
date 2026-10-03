package com.example.minimallauncher.domain

import com.example.minimallauncher.data.search.settingsTargets
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSearchTest {
    private val targets = listOf(
        SettingsSearchTarget("Wi-Fi", "wifi", listOf("internet", "network")),
        SettingsSearchTarget("Display", "display", listOf("brightness", "screen", "dark mode")),
        SettingsSearchTarget("Network settings", "network", listOf("wifi")),
    )

    @Test fun blankQueryDoesNotFillTheDrawerWithSettings() {
        assertEquals(emptyList<SettingsSearchTarget>(), filterSettingsTargets(targets, "  "))
    }
    @Test fun matchesKeywordsAndIgnoresCaseAndSeparators() {
        assertEquals(listOf(targets[1]), filterSettingsTargets(targets, "DARK-MODE"))
        assertEquals(listOf(targets[0]), filterSettingsTargets(targets, "internet"))
    }
    @Test fun exactTitleComesBeforeKeywordMatches() {
        assertEquals(listOf(targets[0], targets[2]), filterSettingsTargets(targets, "wifi"))
    }
    @Test fun ignoresDiacriticsAndRejectsUnknownQueries() {
        assertEquals(listOf(targets[1]), filterSettingsTargets(targets, "dísplay"))
        assertEquals(emptyList<SettingsSearchTarget>(), filterSettingsTargets(targets, "unrelated"))
    }
    @Test fun productionCatalogFindsSpecificSettingsAndTheirParentPages() {
        val cases = mapOf(
            "build number" to ("Build number" to "android.settings.DEVICE_INFO_SETTINGS"),
            "SCREEN TIMEOUT" to ("Screen timeout" to "android.settings.DISPLAY_SETTINGS"),
            "screen time out" to ("Screen timeout" to "android.settings.DISPLAY_SETTINGS"),
            "screen off" to ("Screen timeout" to "android.settings.DISPLAY_SETTINGS"),
            "USB debugging" to ("USB debugging" to "android.settings.APPLICATION_DEVELOPMENT_SETTINGS"),
            "font size" to ("Font size" to "android.settings.DISPLAY_SETTINGS"),
            "android version" to ("Android version" to "android.settings.DEVICE_INFO_SETTINGS"),
        )
        cases.forEach { (query, expected) ->
            val result = filterSettingsTargets(settingsTargets, query).first()
            assertEquals(query, expected.first, result.title)
            assertEquals(query, expected.second, result.action)
            assertTrue("Specific settings should name their containing page", result.detail != "Phone settings")
        }
    }

    @Test fun sharedParentPageStillHasDistinctResultKeys() {
        val displayResults = filterSettingsTargets(settingsTargets, "font")
        val keys = displayResults.map { "${it.action}:${it.title}" }
        assertTrue(displayResults.size > 1)
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test fun specificOptionsCarryNativePreferenceKeys() {
        val cases = mapOf("build number" to "build_number", "screen timeout" to "screen_timeout",
            "android version" to "firmware_version", "dark theme" to "dark_ui_mode",
            "USB debugging" to "enable_adb")
        cases.forEach { (query, key) ->
            assertEquals(query, key, filterSettingsTargets(settingsTargets, query).first().preferenceKey)
        }
    }

}
