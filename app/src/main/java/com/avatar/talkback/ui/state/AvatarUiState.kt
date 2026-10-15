package com.avatar.talkback.ui.state

import androidx.compose.runtime.Immutable

/**
 * High-level state of the TalkBack interactive loop.
 */
enum class AvatarInteractionState {
    IDLE,       // Avatar breathes, blinks (2-6s), subtle saccades; waiting for speech
    LISTENING,  // Mic active, speech detected by VAD, buffering audio input
    PROCESSING, // Silence detected; STT transcription + TTS synthesis in progress
    SPEAKING    // Streaming TTS audio playback + driving 60fps facial visemes
}

/**
 * Character identities with specific visual and vocal characteristics.
 */
enum class AvatarCharacter(
    val displayName: String,
    val description: String,
    val voiceId: String,
    val modelAssetPath: String
) {
    CAT(
        displayName = "Domestic Cat",
        description = "Hyper-realistic tabby with fur cards & eye dilation",
        voiceId = "cat_whimsical_soft",
        modelAssetPath = "models/cat_avatar.glb"
    ),
    HUSKY(
        displayName = "Siberian Husky",
        description = "Realistic double-coat fur, ear twitches, and panting",
        voiceId = "husky_energetic_playful",
        modelAssetPath = "models/husky_avatar.glb"
    ),
    JAPANESE_TEEN(
        displayName = "Sakura (Japanese Teen)",
        description = "Kimono attire, delicate SSS skin shading, gentle demeanor",
        voiceId = "sakura_warm_gentle",
        modelAssetPath = "models/sakura_avatar.glb"
    ),
    ADULT_FEMALE(
        displayName = "Elena (Adult Female)",
        description = "Cinematic studio presence, refined features, warm timbre",
        voiceId = "elena_refined_warm",
        modelAssetPath = "models/elena_avatar.glb"
    )
}

/**
 * Immutable UI State representing the complete screen and system state.
 */
@Immutable
data class AvatarUiState(
    val currentAvatar: AvatarCharacter = AvatarCharacter.CAT,
    val interactionState: AvatarInteractionState = AvatarInteractionState.IDLE,
    val isMicEnabled: Boolean = true,
    val isMicLocked: Boolean = false,
    val isToddlerLocked: Boolean = false,
    val audioInputLevel: Float = 0f, // 0.0f to 1.0f for audio activity indicator
    val speechTranscript: String = "",
    val errorMessage: String? = null
)
