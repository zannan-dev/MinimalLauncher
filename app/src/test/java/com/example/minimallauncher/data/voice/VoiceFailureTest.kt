package com.example.minimallauncher.data.voice

import android.speech.SpeechRecognizer
import org.junit.Assert.*
import org.junit.Test

class VoiceFailureTest {
    @Test fun clientFailureDoesNotClaimOfflineModelIsUnavailable() {
        val state = recognitionFailure(SpeechRecognizer.ERROR_CLIENT, "English")
        assertEquals("Listening stopped. Tap to try again", state.message)
        assertFalse(state.needsModel)
        assertFalse(state.busy)
    }
    @Test fun onlyMissingLanguageOffersModelDownload() {
        assertTrue(recognitionFailure(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, "English").needsModel)
        assertFalse(recognitionFailure(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, "English").needsModel)
        assertEquals("Speech isn’t supported for English", recognitionFailure(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, "English").message)
    }
    @Test fun silenceAndUnrecognizedSpeechHaveTheSameHonestFeedback() {
        assertEquals("Didn’t catch that. Try again", recognitionFailure(SpeechRecognizer.ERROR_NO_MATCH, "English").message)
        assertEquals("Didn’t catch that. Try again", recognitionFailure(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, "English").message)
    }
    @Test fun busyAndDisconnectedServicesExplainTheActualFailure() {
        assertEquals("Please wait a moment, then try again", recognitionFailure(SpeechRecognizer.ERROR_RECOGNIZER_BUSY, "English").message)
        assertEquals("Speech service disconnected. Try again", recognitionFailure(SpeechRecognizer.ERROR_SERVER_DISCONNECTED, "English").message)
    }
}
