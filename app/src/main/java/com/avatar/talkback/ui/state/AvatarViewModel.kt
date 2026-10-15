package com.avatar.talkback.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avatar.talkback.audio.TalkBackAudioEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AvatarUiEvent {
    object ToggleMic : AvatarUiEvent
    object ToggleMicLock : AvatarUiEvent
    object EnableToddlerLock : AvatarUiEvent
    object DismissToddlerLock : AvatarUiEvent
    data class SelectCharacter(val character: AvatarCharacter) : AvatarUiEvent
    data class TestSpeak(val text: String) : AvatarUiEvent
}

sealed interface AvatarUiEffect {
    data class ShowToast(val message: String) : AvatarUiEffect
    object TriggerHapticTap : AvatarUiEffect
    object TriggerHapticHeavy : AvatarUiEffect
}

class AvatarViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AvatarUiState())
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AvatarUiEffect>()
    val uiEffect: SharedFlow<AvatarUiEffect> = _uiEffect.asSharedFlow()

    private val audioEngine = TalkBackAudioEngine(application, viewModelScope)

    init {
        // Collect listening status
        viewModelScope.launch {
            audioEngine.isListening.collect { listening ->
                if (!_uiState.value.isMicEnabled) return@collect
                if (!audioEngine.isSpeaking.value) {
                    _uiState.update {
                        it.copy(
                            interactionState = if (listening) AvatarInteractionState.LISTENING else AvatarInteractionState.IDLE
                        )
                    }
                }
            }
        }

        // Collect speaking status
        viewModelScope.launch {
            audioEngine.isSpeaking.collect { speaking ->
                _uiState.update {
                    it.copy(
                        interactionState = if (speaking) AvatarInteractionState.SPEAKING else AvatarInteractionState.IDLE
                    )
                }
            }
        }

        // Collect real-time 60 FPS mouth viseme animation values
        viewModelScope.launch {
            audioEngine.mouthOpen.collect { mouthOpenVal ->
                _uiState.update { it.copy(mouthOpenAmount = mouthOpenVal) }
            }
        }

        // Collect recognized transcript
        viewModelScope.launch {
            audioEngine.lastTranscript.collect { text ->
                if (text.isNotBlank()) {
                    _uiState.update { it.copy(lastRecognizedText = text) }
                }
            }
        }
    }

    fun onEvent(event: AvatarUiEvent) {
        when (event) {
            is AvatarUiEvent.ToggleMic -> handleToggleMic()
            is AvatarUiEvent.ToggleMicLock -> handleToggleMicLock()
            is AvatarUiEvent.EnableToddlerLock -> handleEnableToddlerLock()
            is AvatarUiEvent.DismissToddlerLock -> handleDismissToddlerLock()
            is AvatarUiEvent.SelectCharacter -> handleSelectCharacter(event.character)
            is AvatarUiEvent.TestSpeak -> audioEngine.revoice(event.text)
        }
    }

    private fun handleToggleMic() {
        if (_uiState.value.isMicLocked) {
            viewModelScope.launch {
                _uiEffect.emit(AvatarUiEffect.ShowToast("Microphone is locked. Unlock small switch to toggle."))
                _uiEffect.emit(AvatarUiEffect.TriggerHapticTap)
            }
            return
        }

        val newMicState = !_uiState.value.isMicEnabled
        _uiState.update { it.copy(isMicEnabled = newMicState) }
        audioEngine.setMicEnabled(newMicState)
        viewModelScope.launch { _uiEffect.emit(AvatarUiEffect.TriggerHapticTap) }
    }

    private fun handleToggleMicLock() {
        _uiState.update { it.copy(isMicLocked = !it.isMicLocked) }
        viewModelScope.launch { _uiEffect.emit(AvatarUiEffect.TriggerHapticTap) }
    }

    private fun handleEnableToddlerLock() {
        _uiState.update { it.copy(isToddlerLocked = true) }
        viewModelScope.launch { _uiEffect.emit(AvatarUiEffect.TriggerHapticHeavy) }
    }

    private fun handleDismissToddlerLock() {
        _uiState.update { it.copy(isToddlerLocked = false) }
        viewModelScope.launch { _uiEffect.emit(AvatarUiEffect.TriggerHapticHeavy) }
    }

    private fun handleSelectCharacter(character: AvatarCharacter) {
        _uiState.update { it.copy(currentAvatar = character) }
        audioEngine.setCharacter(character)

        // Character greeting in tailored persona voice
        val greeting = when (character) {
            AvatarCharacter.CAT -> "Meow! Speak to me, and I'll talk back!"
            AvatarCharacter.HUSKY -> "Woof! Speak to me, I'm listening!"
            AvatarCharacter.JAPANESE_TEEN -> "Hello! I am Sakura. Let's talk!"
            AvatarCharacter.ADULT_FEMALE -> "Hello! Say something, and I'll repeat it back."
        }
        audioEngine.revoice(greeting)
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
