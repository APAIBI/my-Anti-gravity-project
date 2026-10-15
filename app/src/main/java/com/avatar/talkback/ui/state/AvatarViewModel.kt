package com.avatar.talkback.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    data class OnSpeechDetected(val audioLevel: Float) : AvatarUiEvent
    data class OnSpeechFinished(val rawAudioChunk: ByteArray) : AvatarUiEvent
    data class OnSynthesisProgress(val isSpeaking: Boolean) : AvatarUiEvent
}

sealed interface AvatarUiEffect {
    data class ShowToast(val message: String) : AvatarUiEffect
    object TriggerHapticTap : AvatarUiEffect
    object TriggerHapticHeavy : AvatarUiEffect
}

class AvatarViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AvatarUiState())
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AvatarUiEffect>()
    val uiEffect: SharedFlow<AvatarUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: AvatarUiEvent) {
        when (event) {
            is AvatarUiEvent.ToggleMic -> handleToggleMic()
            is AvatarUiEvent.ToggleMicLock -> handleToggleMicLock()
            is AvatarUiEvent.EnableToddlerLock -> handleEnableToddlerLock()
            is AvatarUiEvent.DismissToddlerLock -> handleDismissToddlerLock()
            is AvatarUiEvent.SelectCharacter -> handleSelectCharacter(event.character)
            is AvatarUiEvent.OnSpeechDetected -> handleSpeechDetected(event.audioLevel)
            is AvatarUiEvent.OnSpeechFinished -> handleSpeechFinished(event.rawAudioChunk)
            is AvatarUiEvent.OnSynthesisProgress -> handleSynthesisProgress(event.isSpeaking)
        }
    }

    private fun handleToggleMic() {
        if (_uiState.value.isMicLocked) {
            viewModelScope.launch {
                _uiEffect.emit(AvatarUiEffect.ShowToast("Microphone is locked. Unlock to change."))
                _uiEffect.emit(AvatarUiEffect.TriggerHapticTap)
            }
            return
        }

        _uiState.update { current ->
            val newMicState = !current.isMicEnabled
            current.copy(
                isMicEnabled = newMicState,
                interactionState = if (newMicState) AvatarInteractionState.IDLE else AvatarInteractionState.IDLE,
                audioInputLevel = 0f
            )
        }
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
        if (_uiState.value.interactionState == AvatarInteractionState.SPEAKING) return
        _uiState.update { it.copy(currentAvatar = character) }
    }

    private fun handleSpeechDetected(level: Float) {
        if (!_uiState.value.isMicEnabled) return
        _uiState.update {
            it.copy(
                interactionState = AvatarInteractionState.LISTENING,
                audioInputLevel = level
            )
        }
    }

    private fun handleSpeechFinished(rawAudioChunk: ByteArray) {
        if (!_uiState.value.isMicEnabled) return
        _uiState.update {
            it.copy(
                interactionState = AvatarInteractionState.PROCESSING,
                audioInputLevel = 0f
            )
        }
        // In real pipeline: AudioPipelineManager handles STT -> TTS -> triggers OnSynthesisProgress
    }

    private fun handleSynthesisProgress(isSpeaking: Boolean) {
        _uiState.update {
            it.copy(
                interactionState = if (isSpeaking) AvatarInteractionState.SPEAKING else AvatarInteractionState.IDLE
            )
        }
    }
}
