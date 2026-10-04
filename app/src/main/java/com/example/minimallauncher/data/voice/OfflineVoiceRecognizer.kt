package com.example.minimallauncher.data.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceState(val message: String = "Tap to speak", val busy: Boolean = false, val needsModel: Boolean = false, val listening: Boolean = false, val level: Float = 0f)

/** Main-thread owner. Never creates the network-capable default recognizer. */
class OfflineVoiceRecognizer(private val context: Context, private val onResult: (String) -> Unit) {
    private val mutableState = MutableStateFlow(VoiceState())
    val state = mutableState.asStateFlow()
    private var recognizer: SpeechRecognizer? = null
    private var acceptingResults = false
    private var destroyed = false
    private var generation = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var microphone: SilentSpeechAudioSource? = null
    private val timeout = Runnable {
        cancel()
        mutableState.value = VoiceState("Didn’t catch that. Try again")
    }
    private fun closeMicrophone() {
        handler.removeCallbacks(timeout)
        microphone?.close()
        microphone = null
    }
    private fun request() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().stripExtensions().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    fun start() {
        if (destroyed || mutableState.value.busy) return
        if (Build.VERSION.SDK_INT < 33 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            mutableState.value = VoiceState("Offline speech isn’t available on this device")
            return
        }
        // A fresh engine isolates callbacks from the previous completed utterance.
        cancel()
        try {
            val captureSession = generation + if (recognizer == null) 1 else 0
            microphone = SilentSpeechAudioSource(
                onLevel = { level -> handler.post {
                    if (acceptingResults && captureSession == generation && mutableState.value.listening) {
                        mutableState.value = mutableState.value.copy(level = level)
                    }
                } },
                onFailure = { handler.post {
                    if (acceptingResults && captureSession == generation) {
                        cancel()
                        mutableState.value = VoiceState("Microphone isn’t available. Try again")
                    }
                } },
            )
            val engine = recognizer ?: SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also {
                val session = ++generation
                recognizer = it
                it.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (acceptingResults && session == generation) {
                            mutableState.value = VoiceState("Listening…", true, listening = true)
                        }
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        if (acceptingResults && session == generation) {
                            mutableState.value = VoiceState("Finishing…", true)
                        }
                    }
                    override fun onError(error: Int) {
                        if (!acceptingResults || session != generation) return
                        closeMicrophone()
                        acceptingResults = false
                        android.util.Log.w("LauncherVoice", "Recognition ended with error code $error")
                        mutableState.value = recognitionFailure(error, Locale.getDefault().displayLanguage)
                    }
                    override fun onResults(results: Bundle?) {
                        if (!acceptingResults || session != generation) return
                        closeMicrophone()
                        acceptingResults = false
                        mutableState.value = VoiceState()
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!text.isNullOrBlank()) onResult(text)
                        else mutableState.value = VoiceState("Didn’t catch that. Try again")
                    }
                    override fun onSegmentResults(segmentResults: Bundle) { onResults(segmentResults) }
                    override fun onEndOfSegmentedSession() {
                        if (acceptingResults && session == generation) {
                            closeMicrophone()
                            acceptingResults = false
                            mutableState.value = VoiceState("Didn’t catch that. Try again")
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            acceptingResults = true
            mutableState.value = VoiceState("Getting ready…", true)
            engine.startListening(request().also { microphone?.configure(it) })
            microphone?.start()
            handler.postDelayed(timeout, 20_000)
        } catch (_: Exception) {
            cancel()
            acceptingResults = false
            mutableState.value = VoiceState("Couldn’t start listening. Try again")
        }
    }

    fun cancel() {
        generation++
        acceptingResults = false
        recognizer?.cancel()
        // Destroy each session so cancelled callbacks cannot reach a later session.
        recognizer?.destroy()
        recognizer = null
        closeMicrophone()
        mutableState.value = VoiceState()
    }

    fun downloadModel() {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                recognizer?.triggerModelDownload(request())
                mutableState.value = VoiceState("Speech download requested. Try again when it’s ready")
            } catch (_: Exception) {
                mutableState.value = VoiceState("Download offline speech in your phone’s speech settings")
            }
        }
    }

    fun close() { cancel(); destroyed = true }
}


/** Runtime failures do not imply that the installed offline model is missing. */
internal fun recognitionFailure(error: Int, language: String): VoiceState = when (error) {
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> VoiceState("Download speech for $language to use it offline", needsModel = true)
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> VoiceState("Speech isn’t supported for $language")
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceState("Didn’t catch that. Try again")
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceState("Microphone access is needed")
    SpeechRecognizer.ERROR_AUDIO -> VoiceState("Microphone was interrupted. Try again")
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> VoiceState("Please wait a moment, then try again")
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> VoiceState("Speech service disconnected. Try again")
    else -> VoiceState("Listening stopped. Tap to try again")
}
