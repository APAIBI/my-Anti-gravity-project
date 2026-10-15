package com.avatar.talkback.ui.state

import androidx.compose.runtime.Immutable

/**
 * High-level state of the TalkBack interactive loop.
 */
enum class AvatarInteractionState {
    IDLE,       // Avatar breathes, blinks (2-6s), subtle micro-movements
    LISTENING,  // Mic active, speech recognizer capturing user voice
    PROCESSING, // Silence detected; processing speech to text
    SPEAKING    // Streaming TTS voice + driving 60 FPS real-time facial mouth visemes
}

/**
 * The 4 Character Identities specified in the prompt.
 */
enum class AvatarCharacter(
    val displayName: String,
    val description: String,
    val personaTag: String
) {
    CAT(
        displayName = "Tabby Cat",
        description = "Groomed fur, green-gold eyes, whisker twitches",
        personaTag = "Whimsical & Soft Voice"
    ),
    HUSKY(
        displayName = "Siberian Husky",
        description = "Heterochromia eyes, double-coat fur, ear twitches",
        personaTag = "Energetic & Playful Voice"
    ),
    JAPANESE_TEEN(
        displayName = "Sakura",
        description = "Floral kimono, gentle bob hair, natural demeanor",
        personaTag = "Warm & Empathetic Voice"
    ),
    ADULT_FEMALE(
        displayName = "Elena",
        description = "Ana de Armas styling, refined facial structure",
        personaTag = "Sophisticated & Warm Voice"
    )
}

/**
 * Complete immutable screen state.
 */
@Immutable
data class AvatarUiState(
    val currentAvatar: AvatarCharacter = AvatarCharacter.CAT,
    val interactionState: AvatarInteractionState = AvatarInteractionState.IDLE,
    val isMicEnabled: Boolean = true,
    val isMicLocked: Boolean = false,
    val isToddlerLocked: Boolean = false,
    val audioInputLevel: Float = 0f,
    val mouthOpenAmount: Float = 0f, // 0.0f (closed) to 1.0f (open viseme)
    val lastRecognizedText: String = "",
    val errorMessage: String? = null
)
