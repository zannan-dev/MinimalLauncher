package com.example.minimallauncher.data.voice

import android.speech.SpeechRecognizer
import org.junit.Assert.*
import org.junit.Test

class VoiceFailureTest {
    @Test fun transientDisconnectIsRetriedOnceWithinTheOriginalDeadline() {
        assertTrue(shouldReconnectSpeech(SpeechRecognizer.ERROR_SERVER_DISCONNECTED, 0, 100))
        assertTrue(shouldReconnectSpeech(SpeechRecognizer.ERROR_SERVER_DISCONNECTED, 0, 18_999))
        assertFalse(shouldReconnectSpeech(SpeechRecognizer.ERROR_SERVER_DISCONNECTED, 1, 100))
        assertFalse(shouldReconnectSpeech(SpeechRecognizer.ERROR_SERVER_DISCONNECTED, 0, 19_000))
        assertFalse(shouldReconnectSpeech(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, 0, 100))
    }
    @Test fun earlyEmptySessionsHaveABoundedGracePeriod() {
        assertTrue(canRetryEmptySpeech(500))
        assertTrue(canRetryEmptySpeech(7_999))
        assertFalse(canRetryEmptySpeech(8_000))
        assertFalse(canRetryEmptySpeech(20_000))
        assertFalse(canRetryEmptySpeech(-1))
    }
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
        assertEquals("Listening was interrupted. Tap to speak again", recognitionFailure(SpeechRecognizer.ERROR_SERVER_DISCONNECTED, "English").message)
    }
}
