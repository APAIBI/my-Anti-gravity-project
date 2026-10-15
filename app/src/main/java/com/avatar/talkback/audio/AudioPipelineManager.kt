package com.avatar.talkback.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import com.avatar.talkback.lipsync.LipSyncEngine
import com.avatar.talkback.lipsync.VisemeCue
import com.avatar.talkback.ui.state.AvatarCharacter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages the real-time audio pipeline:
 * 1. Hardware Mic capture with hardware NoiseSuppressor & EchoCanceler
 * 2. Voice Activity Detection (VAD)
 * 3. STT Transcription
 * 4. Avatar Persona TTS Synthesis with aligned Viseme cues
 * 5. Synchronized AudioTrack playback
 */
class AudioPipelineManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val lipSyncEngine: LipSyncEngine,
    private val onStateChange: (Boolean, Float) -> Unit // isListening, rmsAudioLevel
) {
    private var recordingJob: Job? = null
    private var isCapturing = false

    // Audio recording configuration
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    @SuppressLint("MissingPermission")
    fun startListening(character: AvatarCharacter) {
        if (isCapturing) return
        isCapturing = true

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                val audioSessionId = audioRecord?.audioSessionId ?: 0
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply { setEnabled(true) }
                }
                if (AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply { setEnabled(true) }
                }

                audioRecord?.startRecording()
                val audioBuffer = ShortArray(bufferSize / 2)

                while (isActive && isCapturing) {
                    val readSamples = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                    if (readSamples > 0) {
                        // Calculate RMS level for UI audio level indicator
                        var sum = 0.0
                        for (i in 0 until readSamples) {
                            sum += audioBuffer[i] * audioBuffer[i]
                        }
                        val rms = Math.sqrt(sum / readSamples).toFloat() / 32767f
                        onStateChange(true, rms)

                        // VAD (Voice Activity Detection) logic triggers STT on silence
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cleanupAudioRecord()
            }
        }
    }

    fun stopListening() {
        isCapturing = false
        recordingJob?.cancel()
        cleanupAudioRecord()
    }

    private fun cleanupAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            noiseSuppressor?.release()
            noiseSuppressor = null
            echoCanceler?.release()
            echoCanceler = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Re-voices the transcribed text using persona-specific TTS models and routes visemes to LipSyncEngine.
     */
    suspend fun revoiceSpeech(
        transcript: String,
        character: AvatarCharacter,
        onPlaybackStarted: () -> Unit,
        onPlaybackFinished: () -> Unit
    ) = withContext(Dispatchers.IO) {
        // 1. Synthesize audio + visemes from selected persona TTS provider (e.g., ElevenLabs / Azure Speech SDK)
        // 2. Stream audio to AudioTrack
        // 3. lipSyncEngine.playVisemes(cues) { audioTrack.playbackHeadPositionMs }
        // 4. Notify UI on completion
    }
}
