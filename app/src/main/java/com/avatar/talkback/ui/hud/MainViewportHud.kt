package com.avatar.talkback.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avatar.talkback.ui.state.AvatarInteractionState
import com.avatar.talkback.ui.state.AvatarUiEvent
import com.avatar.talkback.ui.state.AvatarUiState

/**
 * Main Viewport HUD Overlay.
 * Renders on top of the 3D Avatar Surface.
 * Contains:
 * 1. Top-Right: Discreet Toddler Lock engagement button.
 * 2. Center-Top: Subtle status badge (Listening, Processing, Speaking).
 * 3. Bottom-Center: Dual-action Micro Control Capsule (Mic On/Off + Mic Lock toggle).
 */
@Composable
fun MainViewportHud(
    uiState: AvatarUiState,
    onEvent: (AvatarUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // --- TOP BAR: Toddler Lock Button (Discreet Upper-Right Corner) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(AvatarUiEvent.EnableToddlerLock)
                }
                .padding(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = "Engage Toddler Lock",
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
            )
        }

        // --- TOP-CENTER: Ambient Interaction Status Pill ---
        InteractionStatusBadge(
            state = uiState.interactionState,
            isMicEnabled = uiState.isMicEnabled,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // --- BOTTOM BAR: Floating Capsule with Mic Toggle + Mic Lock ---
        MicrophoneControlCapsule(
            isMicEnabled = uiState.isMicEnabled,
            isMicLocked = uiState.isMicLocked,
            interactionState = uiState.interactionState,
            audioLevel = uiState.audioInputLevel,
            onToggleMic = { onEvent(AvatarUiEvent.ToggleMic) },
            onToggleMicLock = { onEvent(AvatarUiEvent.ToggleMicLock) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * Visual badge indicating current audio/processing loop status.
 */
@Composable
private fun InteractionStatusBadge(
    state: AvatarInteractionState,
    isMicEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val (badgeText, badgeColor) = when {
        !isMicEnabled -> "Mic Muted" to Color(0xFFE57373)
        state == AvatarInteractionState.LISTENING -> "Listening..." to Color(0xFF81C784)
        state == AvatarInteractionState.PROCESSING -> "Thinking..." to Color(0xFFFFB74D)
        state == AvatarInteractionState.SPEAKING -> "Speaking" to Color(0xFF64B5F6)
        else -> "" to Color.Transparent
    }

    AnimatedVisibility(
        visible = badgeText.isNotEmpty() && isMicEnabled,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(if (state == AvatarInteractionState.LISTENING) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = badgeText,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Discreet floating capsule at screen bottom containing:
 * - Large tactile Mic Toggle button
 * - Separate small Mic Lock switch
 */
@Composable
private fun MicrophoneControlCapsule(
    isMicEnabled: Boolean,
    isMicLocked: Boolean,
    interactionState: AvatarInteractionState,
    audioLevel: Float,
    onToggleMic: () -> Unit,
    onToggleMicLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val micBgColor by animateColorAsState(
        targetValue = when {
            !isMicEnabled -> Color(0xFFD32F2F).copy(alpha = 0.85f)
            interactionState == AvatarInteractionState.LISTENING -> Color(0xFF2E7D32).copy(alpha = 0.85f)
            else -> Color(0xFF212121).copy(alpha = 0.75f)
        },
        animationSpec = tween(300),
        label = "micBgColor"
    )

    Box(
        modifier = modifier
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.45f),
                        Color.Black.copy(alpha = 0.70f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Mic Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(micBgColor)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleMic()
                    }
            ) {
                Icon(
                    imageVector = if (isMicEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = if (isMicEnabled) "Mute Microphone" else "Unmute Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.18f))
            )

            // Dedicated Mic Lock Switch
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMicLocked) Color(0xFFFFB300).copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.08f)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleMicLock()
                    }
            ) {
                Icon(
                    imageVector = if (isMicLocked) Icons.Filled.Lock else Icons.Outlined.LockOpen,
                    contentDescription = if (isMicLocked) "Microphone Locked" else "Microphone Unlocked",
                    tint = if (isMicLocked) Color(0xFFFFCA28) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
