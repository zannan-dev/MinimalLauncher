package com.example.minimallauncher.data.voice

import android.annotation.SuppressLint
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import android.speech.RecognizerIntent
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.sqrt

internal interface SpeechAudioSource {
    fun configure(intent: Intent)
    fun start()
    fun close()
}

/** Capture raw PCM ourselves so the speech provider receives an external, silent audio source. */
internal class SilentSpeechAudioSource(private val onLevel: (Float) -> Unit, private val onFailure: () -> Unit) : SpeechAudioSource {
    private val pipes = ParcelFileDescriptor.createPipe()
    private val output = ParcelFileDescriptor.AutoCloseOutputStream(pipes[1])
    private val active = AtomicBoolean(false)
    private var started = false
    @SuppressLint("MissingPermission") // The caller obtains RECORD_AUDIO before constructing a session.
    private val recorder = try { AudioRecord.Builder()
        .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
        .setAudioFormat(AudioFormat.Builder().setSampleRate(16_000).setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
        .setBufferSizeInBytes(maxOf(4096, AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2))
        .build()
    } catch (failure: Exception) {
        output.close()
        pipes[0].close()
        throw failure
    }

    init {
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            output.close()
            pipes[0].close()
            error("Microphone could not initialize")
        }
    }

    override fun configure(intent: Intent) { configureSilentSpeechInput(intent, pipes[0]) }

    override fun start() {
        recorder.startRecording()
        check(recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING)
        active.set(true)
        started = true
        Thread({
            val pcm = ShortArray(512)
            val bytes = ByteArray(pcm.size * 2)
            var frames = 0
            try {
                while (active.get()) {
                    val count = recorder.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                    if (count <= 0) { if (active.get()) error("Microphone read failed"); break }
                    var power = 0.0
                    for (i in 0 until count) {
                        val sample = pcm[i].toInt()
                        bytes[i * 2] = sample.toByte()
                        bytes[i * 2 + 1] = (sample shr 8).toByte()
                        power += sample.toDouble() * sample
                    }
                    output.write(bytes, 0, count * 2)
                    // Draw updates stay bounded; audio itself is streamed without batching delay.
                    frames += count
                    if (frames >= 1600) {
                        frames = 0
                        val db = 20.0 * log10((sqrt(power / count) / 32768.0).coerceAtLeast(0.00001))
                        onLevel(((db + 55.0) / 35.0).toFloat().coerceIn(0f, 1f))
                    }
                }
            } catch (_: Exception) {
                if (active.getAndSet(false)) onFailure()
            } finally {
                active.set(false)
                runCatching { recorder.stop() }
                recorder.release()
                runCatching { output.close() }
            }
        }, "Silent voice input").apply { isDaemon = true; start() }
    }

    override fun close() {
        active.set(false)
        runCatching { recorder.stop() }
        runCatching { pipes[0].close() }
        runCatching { output.close() }
        if (!started) recorder.release()
    }
}

/** Shared by live capture and the real-device recorded-command regression test. */
internal fun configureSilentSpeechInput(intent: Intent, descriptor: ParcelFileDescriptor) {
    intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, descriptor)
    intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
    intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
    intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16_000)
    // Keep the session alive with our microphone stream, including silent intermediate segments.
    intent.putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
    intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_800)
    intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2_500)
}
