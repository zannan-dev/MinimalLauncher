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
class OfflineVoiceRecognizer internal constructor(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val audioSourceFactory: ((Float) -> Unit, () -> Unit) -> SpeechAudioSource,
) {
    constructor(context: Context, onResult: (String) -> Unit) : this(context, onResult, ::SilentSpeechAudioSource)
    private val mutableState = MutableStateFlow(VoiceState())
    val state = mutableState.asStateFlow()
    private var listeningStartedAt = 0L
    private var retrying = false
    private var reconnectAttempts = 0
    private var serviceReadyAfter = 0L
    private var nameHints: List<String> = emptyList()
    private var recognizer: SpeechRecognizer? = null
    private var acceptingResults = false
    private var destroyed = false
    private var generation = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var microphone: SpeechAudioSource? = null
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
        if (Build.VERSION.SDK_INT >= 33) {
            putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, ArrayList(nameHints))
        }
    }

    private fun retryEmptySession(): Boolean {
        if (!canRetryEmptySpeech(android.os.SystemClock.elapsedRealtime() - listeningStartedAt)) return false
        restartSession(100)
        return true
    }

    private fun restartSession(delayMillis: Long) {
        val startedAt = listeningStartedAt
        val hints = nameHints
        releaseSession()
        listeningStartedAt = startedAt
        mutableState.value = VoiceState("Listening…", true, listening = true)
        queueSession(hints, delayMillis)
    }

    private fun queueSession(hints: List<String>, delayMillis: Long) {
        retrying = true
        val retryGeneration = generation
        handler.postDelayed({
            if (retrying && !destroyed && generation == retryGeneration) {
                retrying = false
                beginSession(hints, fresh = false)
            }
        }, delayMillis)
    }

    fun start(hints: List<String> = emptyList()) = beginSession(hints, fresh = true)

    private fun beginSession(hints: List<String>, fresh: Boolean) {
        nameHints = hints.filter { it.isNotBlank() }.distinct()
        if (destroyed || (fresh && mutableState.value.busy)) return
        if (Build.VERSION.SDK_INT < 33 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            mutableState.value = VoiceState("Offline speech isn’t available on this device")
            return
        }
        // A fresh engine isolates callbacks while an internal retry keeps the listening UI steady.
        releaseSession()
        if (fresh) {
            listeningStartedAt = android.os.SystemClock.elapsedRealtime()
            reconnectAttempts = 0
        }
        val remainingReleaseDelay = serviceReadyAfter - android.os.SystemClock.elapsedRealtime()
        if (remainingReleaseDelay > 0) {
            if (fresh || !mutableState.value.listening) mutableState.value = VoiceState("Getting ready…", true)
            queueSession(nameHints, remainingReleaseDelay)
            return
        }
        try {
            val captureSession = generation + if (recognizer == null) 1 else 0
            microphone = audioSourceFactory(
                { level -> handler.post {
                    if (acceptingResults && captureSession == generation && mutableState.value.listening) {
                        mutableState.value = mutableState.value.copy(level = level)
                    }
                } },
                { handler.post {
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
                            mutableState.value = mutableState.value.copy(message = "Listening…", busy = true, listening = true)
                        }
                    }
                    override fun onBeginningOfSpeech() {
                        if (acceptingResults && session == generation) {
                            mutableState.value = mutableState.value.copy(message = "Listening…", busy = true, listening = true)
                        }
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    // This is a segment boundary, often just a pause. The PCM session is still listening.
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (!acceptingResults || session != generation) return
                        if (shouldReconnectSpeech(error, reconnectAttempts, android.os.SystemClock.elapsedRealtime() - listeningStartedAt)) {
                            reconnectAttempts++
                            restartSession(400)
                            return
                        }
                        if ((error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) && retryEmptySession()) return
                        releaseSession()
                        android.util.Log.w("LauncherVoice", "Recognition ended with error code $error")
                        mutableState.value = recognitionFailure(error, Locale.getDefault().displayLanguage)
                    }
                    override fun onResults(results: Bundle?) {
                        if (!acceptingResults || session != generation) return
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (text.isNullOrBlank() && retryEmptySession()) return
                        releaseSession()
                        mutableState.value = VoiceState()
                        if (!text.isNullOrBlank()) onResult(text)
                        else mutableState.value = VoiceState("Didn’t catch that. Try again")
                    }
                    override fun onSegmentResults(segmentResults: Bundle) {
                        // Pixel can emit an empty intermediate segment while the audio stream is still live.
                        // It is not the end of the utterance: keep listening for the next segment.
                        if (segmentResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.any { it.isNotBlank() } == true) {
                            onResults(segmentResults)
                        } else if (acceptingResults && session == generation) {
                            mutableState.value = mutableState.value.copy(message = "Listening…", busy = true, listening = true)
                        }
                    }
                    override fun onEndOfSegmentedSession() {
                        if (acceptingResults && session == generation) {
                            if (retryEmptySession()) return
                            releaseSession()
                            mutableState.value = VoiceState("Didn’t catch that. Try again")
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            acceptingResults = true
            mutableState.value = if (fresh) VoiceState("Getting ready…", true) else VoiceState("Listening…", true, listening = true)
            engine.startListening(request().also { microphone?.configure(it) })
            microphone?.start()
            handler.postDelayed(timeout, (20_000L - (android.os.SystemClock.elapsedRealtime() - listeningStartedAt)).coerceAtLeast(1L))
        } catch (_: Exception) {
            cancel()
            acceptingResults = false
            mutableState.value = VoiceState("Couldn’t start listening. Try again")
        }
    }

    /** Invalidate callbacks before stopping PCM or unbinding the service. */
    private fun releaseSession() {
        retrying = false
        generation++
        acceptingResults = false
        val engine = recognizer
        recognizer = null
        if (engine != null) serviceReadyAfter = android.os.SystemClock.elapsedRealtime() + 400L
        closeMicrophone()
        runCatching { engine?.cancel() }
        runCatching { engine?.destroy() }
    }

    fun cancel() {
        releaseSession()
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
    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> VoiceState("Listening was interrupted. Tap to speak again")
    else -> VoiceState("Listening stopped. Tap to try again")
}

/** Only empty early sessions are retried; meaningful results and other failures finish immediately. */
internal fun canRetryEmptySpeech(elapsedMillis: Long): Boolean = elapsedMillis in 0 until 8_000L

/** Rebind once on a transient disconnect while leaving room within the original session deadline. */
internal fun shouldReconnectSpeech(error: Int, attempts: Int, elapsedMillis: Long): Boolean =
    error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED && attempts == 0 && elapsedMillis in 0 until 19_000L
