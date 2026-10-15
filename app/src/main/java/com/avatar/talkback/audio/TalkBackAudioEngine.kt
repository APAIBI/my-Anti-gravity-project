package com.avatar.talkback.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.avatar.talkback.ui.state.AvatarCharacter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

/**
 * Native Android TalkBack Engine:
 * 1. Captures live microphone speech using native SpeechRecognizer.
 * 2. Transcribes user speech in real-time.
 * 3. Re-voices speech via TextToSpeech with persona-specific pitch/speed profiles.
 * 4. Synthesizes 60 FPS mouth viseme animation values (0.0f to 1.0f) during speech.
 */
class TalkBackAudioEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _mouthOpen = MutableStateFlow(0f)
    val mouthOpen: StateFlow<Float> = _mouthOpen.asStateFlow()

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private var lipSyncAnimationJob: Job? = null
    private var currentCharacter = AvatarCharacter.CAT
    private var isMicEnabled = true

    init {
        // Initialize Android native TTS engine
        textToSpeech = TextToSpeech(context, this)
        initSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.getDefault()
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    startLipSyncAnimation()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopLipSyncAnimation()
                    // Resume listening automatically after speaking
                    if (isMicEnabled) {
                        scope.launch(Dispatchers.Main) {
                            delay(400)
                            startListening()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopLipSyncAnimation()
                    if (isMicEnabled) {
                        scope.launch(Dispatchers.Main) { startListening() }
                    }
                }
            })
            isTtsInitialized = true
            if (isMicEnabled) {
                startListening()
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    // Auto-retry listening if mic remains enabled and not speaking
                    if (isMicEnabled && !_isSpeaking.value) {
                        scope.launch(Dispatchers.Main) {
                            delay(600)
                            startListening()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()

                    if (!spokenText.isNullOrBlank()) {
                        _lastTranscript.value = spokenText
                        revoice(spokenText)
                    } else if (isMicEnabled) {
                        scope.launch(Dispatchers.Main) {
                            delay(300)
                            startListening()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun setCharacter(character: AvatarCharacter) {
        currentCharacter = character
    }

    fun setMicEnabled(enabled: Boolean) {
        isMicEnabled = enabled
        if (!enabled) {
            stopListening()
            textToSpeech?.stop()
            _isSpeaking.value = false
            stopLipSyncAnimation()
        } else {
            if (!_isSpeaking.value) {
                startListening()
            }
        }
    }

    fun startListening() {
        if (!isMicEnabled || _isSpeaking.value) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Re-voices transcribed speech using persona-tailored vocal characteristics.
     */
    fun revoice(text: String) {
        if (!isTtsInitialized) return

        // Configure Persona Voice Pitch & Rate
        when (currentCharacter) {
            AvatarCharacter.CAT -> {
                // High-pitched, whimsical, expressive
                textToSpeech?.setPitch(1.65f)
                textToSpeech?.setSpeechRate(1.15f)
            }
            AvatarCharacter.HUSKY -> {
                // Energetic, friendly, resonant lower-mid range
                textToSpeech?.setPitch(0.72f)
                textToSpeech?.setSpeechRate(1.08f)
            }
            AvatarCharacter.JAPANESE_TEEN -> {
                // Gentle, warm, conversational female voice (Sakura style)
                textToSpeech?.setPitch(1.28f)
                textToSpeech?.setSpeechRate(0.98f)
            }
            AvatarCharacter.ADULT_FEMALE -> {
                // Sophisticated, natural, warm vocal timbre (Ana de Armas style)
                textToSpeech?.setPitch(0.95f)
                textToSpeech?.setSpeechRate(0.92f)
            }
        }

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TALKBACK_UTTERANCE")
    }

    /**
     * Drives real-time mouth movement at 60 FPS in sync with the speech playback.
     */
    private fun startLipSyncAnimation() {
        lipSyncAnimationJob?.cancel()
        lipSyncAnimationJob = scope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive && _isSpeaking.value) {
                step++
                // Multi-frequency wave simulating syllabic mouth opening
                val wave1 = (sin(step * 0.45) + 1.0) / 2.0
                val wave2 = (sin(step * 0.18) + 1.0) / 2.0
                val combined = ((wave1 * 0.7 + wave2 * 0.3) * 0.85).toFloat()

                _mouthOpen.value = combined.coerceIn(0.05f, 1.0f)
                delay(20) // ~50-60 FPS
            }
            _mouthOpen.value = 0f
        }
    }

    private fun stopLipSyncAnimation() {
        lipSyncAnimationJob?.cancel()
        _mouthOpen.value = 0f
    }

    fun release() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        stopLipSyncAnimation()
    }
}
