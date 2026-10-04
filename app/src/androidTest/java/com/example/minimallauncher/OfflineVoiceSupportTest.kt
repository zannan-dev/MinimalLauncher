package com.example.minimallauncher

import android.content.Intent
import android.os.Build
import android.speech.*
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Check the installed service/model without requesting microphone access or recording. */
class OfflineVoiceSupportTest {
    @Test fun pixelOnDeviceRecognitionSupport() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val done = CountDownLatch(1)
        var engine: SpeechRecognizer? = null
        instrumentation.runOnMainSync {
            assertTrue("No on-device recognition service", SpeechRecognizer.isOnDeviceRecognitionAvailable(context))
            engine = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            engine!!.checkRecognitionSupport(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().stripExtensions().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }, context.mainExecutor, object : RecognitionSupportCallback {
                override fun onSupportResult(support: RecognitionSupport) {
                    println("Offline speech: locale=${Locale.getDefault().stripExtensions().toLanguageTag()}, installed=${support.installedOnDeviceLanguages}, pending=${support.pendingOnDeviceLanguages}")
                    done.countDown()
                }
                override fun onError(error: Int) { println("Offline speech support check error=$error"); done.countDown() }
            })
        }
        try { assertTrue("Support check timed out", done.await(10, TimeUnit.SECONDS)) }
        finally { instrumentation.runOnMainSync { engine?.destroy() } }
    }

    @Test fun externalMicrophoneInputSuppressesSpeechServicePlayback() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED)
        fun playbackEvents(): Set<String> {
            val descriptor = instrumentation.uiAutomation.executeShellCommand("dumpsys audio")
            val dump = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
            return dump.substringAfter("### Playback activity").substringBefore("## RecordingActivityMonitor")
                .lineSequence().filter { it.contains("new player") && it.contains("package:com.google.android.as ") }.toSet()
        }
        val before = playbackEvents()
        val ready = CountDownLatch(1)
        val finished = CountDownLatch(1)
        var engine: SpeechRecognizer? = null
        var input: com.example.minimallauncher.data.voice.SilentSpeechAudioSource? = null
        instrumentation.runOnMainSync {
            input = com.example.minimallauncher.data.voice.SilentSpeechAudioSource({}, { finished.countDown() })
            engine = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            engine!!.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) { ready.countDown() }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { finished.countDown() }
                override fun onResults(results: android.os.Bundle?) { finished.countDown() }
                override fun onSegmentResults(segmentResults: android.os.Bundle) { finished.countDown() }
                override fun onEndOfSegmentedSession() { finished.countDown() }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            engine!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().stripExtensions().toLanguageTag())
                input!!.configure(this)
            })
            input!!.start()
        }
        try {
            assertTrue("Microphone session did not start", ready.await(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { engine?.stopListening() }
            assertTrue("Recognition session did not finish", finished.await(15, TimeUnit.SECONDS))
            // Give asynchronous end prompts time to reach Android's playback event log.
            android.os.SystemClock.sleep(1_000)
        } finally {
            instrumentation.runOnMainSync { engine?.cancel(); engine?.destroy(); input?.close() }
        }
        val newPlayers = playbackEvents() - before
        assertTrue("Speech service played an audio prompt: $newPlayers", newPlayers.isEmpty())
    }


    @Test fun recordedCommandCompletesWithoutProviderSound() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fun events(): Set<String> {
            val descriptor = instrumentation.uiAutomation.executeShellCommand("dumpsys audio")
            return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
                .substringAfter("### Playback activity").substringBefore("## RecordingActivityMonitor")
                .lineSequence().filter { it.contains("new player") && it.contains("package:com.google.android.as ") }.toSet()
        }
        val before = events()
        val pipes = android.os.ParcelFileDescriptor.createPipe()
        val finished = CountDownLatch(1)
        var text = ""
        var error = -1
        var engine: SpeechRecognizer? = null
        instrumentation.runOnMainSync {
            engine = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            engine!!.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(code: Int) { error = code; finished.countDown() }
                override fun onResults(results: android.os.Bundle?) {
                    text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    finished.countDown()
                }
                override fun onSegmentResults(segmentResults: android.os.Bundle) { onResults(segmentResults) }
                override fun onEndOfSegmentedSession() { finished.countDown() }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            engine!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, arrayListOf("Camera", "Blinkit", "AiCam Alert", "Shuhaib", "Umma"))
                com.example.minimallauncher.data.voice.configureSilentSpeechInput(this, pipes[0])
            })
        }
        val writer = Thread {
            runCatching {
                android.os.ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use { out ->
                    instrumentation.context.assets.open("open_camera_16khz.pcm").use { input ->
                        val buffer = ByteArray(640)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            out.write(buffer, 0, count)
                            android.os.SystemClock.sleep(20)
                        }
                    }
                }
            }
        }.apply { isDaemon = true; start() }
        try {
            assertTrue("Command did not finish", finished.await(20, TimeUnit.SECONDS))
            assertTrue("Expected open camera, got '$text', error=$error", text.lowercase().contains("open camera"))
        } finally {
            instrumentation.runOnMainSync { engine?.cancel(); engine?.destroy() }
            pipes[0].close()
            pipes[1].close()
            writer.join(2_000)
        }
        android.os.SystemClock.sleep(1_000)
        assertTrue("Speech service played a command-completion prompt", (events() - before).isEmpty())
    }

}
