package com.example.minimallauncher.domain

import android.os.UserHandle
import org.mockito.Mockito.mock
import org.junit.Assert.*
import org.junit.Test

class VoiceNameMatchingTest {
    private val user = mock(UserHandle::class.java)
    private fun app(name: String) = LaunchableApp(name, "Activity", name, user, false)

    @Test fun spokenSpacingStillMatchesExactInstalledNames() {
        assertEquals(100, voiceNameScore("blink it", "Blinkit"))
        assertEquals(100, voiceNameScore("AI cam alert", "AiCam Alert"))
        assertEquals(100, voiceNameScore("ai-cam alert", "AiCam Alert"))
        assertEquals(100, voiceNameScore("cafe", "Café"))
    }
    @Test fun smallSpeechSpellingErrorsBecomeSuggestions() {
        for ((spoken, name) in listOf("blanket" to "Blinkit", "I cam alert" to "AiCam Alert",
            "shoaib" to "Shuhaib", "shuaib" to "Shuhaib", "uma" to "Umma", "ummah" to "Umma")) {
            assertTrue("$spoken should suggest $name", voiceNameScore(spoken, name) in 1..89)
        }
    }
    @Test fun unrelatedAndVeryShortNamesDoNotProduceGuesses() {
        assertEquals(0, voiceNameScore("weather", "Blinkit"))
        assertEquals(0, voiceNameScore("xy", "Umma"))
        assertEquals(0, voiceNameScore("", "Camera"))
    }
    @Test fun exactAppRanksAboveNearSpellingAndRetainsDuplicateProfiles() {
        val exact = app("Blinkit")
        val duplicate = exact.copy(activityName = "WorkActivity")
        assertEquals(listOf(exact, duplicate), matchVoiceApps(listOf(app("Blinkist"), exact, duplicate), "blink it"))
    }
    @Test fun politeAppCommandsAndTrailingAppWordKeepTheName() {
        assertEquals(VoiceCommand.OpenApp("blink it"), interpretVoiceCommand("Can you open Blink It please"))
        assertEquals(VoiceCommand.OpenApp("ai cam alert"), interpretVoiceCommand("Could you please open the AI Cam Alert app"))
    }
}
