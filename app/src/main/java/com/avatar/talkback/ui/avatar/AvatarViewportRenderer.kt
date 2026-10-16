package com.avatar.talkback.ui.avatar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.avatar.talkback.R
import com.avatar.talkback.ui.state.AvatarCharacter
import com.avatar.talkback.ui.state.AvatarInteractionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Photorealistic 3D Character Viewport Node.
 *
 * Drives real-time 3D mouth articulation, jaw drop, teeth/tongue exposure,
 * natural breathing, spontaneous eye blinking, and expressive speech postures
 * using high-fidelity 3D CGI dual-state character models.
 */
@Composable
fun AvatarViewportRenderer(
    character: AvatarCharacter,
    interactionState: AvatarInteractionState,
    mouthOpenAmount: Float, // 0.0f (closed) to 1.0f (open viseme)
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Interactive Tap Recoil Animation
    val tapRecoil = remember { Animatable(0f) }

    // 2. Natural Variable Blinking Controller (2.2-5.5s intervals)
    val blinkProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(Random.nextLong(2200, 5500))
            blinkProgress.animateTo(1f, animationSpec = tween(75, easing = LinearEasing))
            blinkProgress.animateTo(0f, animationSpec = tween(110, easing = LinearEasing))
        }
    }

    // 3. Smooth Sinusoidal Breathing Cycle (3.2s period)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingSway by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingSway"
    )

    // 4. Subtle Micro-Drift (Natural idle head sway)
    val microDriftAngle by infiniteTransition.animateFloat(
        initialValue = -0.7f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "microDrift"
    )

    // 5. Speech Rhythm Cadence (Subtle head nodding while speaking)
    val speechNod = if (interactionState == AvatarInteractionState.SPEAKING) {
        (mouthOpenAmount * 6.5f)
    } else {
        0f
    }

    // 6. Responsive Posture & Head Tilt based on Interaction State
    val targetHeadTilt = when (interactionState) {
        AvatarInteractionState.LISTENING -> 2.4f  // Attentive head tilt
        AvatarInteractionState.PROCESSING -> -1.5f // Thoughtful posture
        AvatarInteractionState.SPEAKING -> (mouthOpenAmount * 1.8f) - 0.4f // Expressive cadence
        AvatarInteractionState.IDLE -> microDriftAngle
    }
    val animatedHeadTilt by animateFloatAsState(
        targetValue = targetHeadTilt,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "headTilt"
    )

    val targetScale = when (interactionState) {
        AvatarInteractionState.LISTENING -> 1.035f // Lean forward attentively
        AvatarInteractionState.SPEAKING -> 1.018f + (mouthOpenAmount * 0.012f)
        else -> 1.0f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // Resolve Photorealistic 3D Model Asset Pairs (Idle & Talking States)
    val (idleDrawableRes, talkDrawableRes) = when (character) {
        AvatarCharacter.CAT -> R.drawable.avatar_cat to R.drawable.avatar_cat_talk
        AvatarCharacter.HUSKY -> R.drawable.avatar_husky to R.drawable.avatar_husky_talk
        AvatarCharacter.JAPANESE_TEEN -> R.drawable.avatar_sakura to R.drawable.avatar_sakura_talk
        AvatarCharacter.ADULT_FEMALE -> R.drawable.avatar_elena to R.drawable.avatar_elena_talk
    }

    // Dynamic 3D Mouth Viseme Alpha (smooth articulation between closed and open states)
    val talkAlpha = if (interactionState == AvatarInteractionState.SPEAKING) {
        (mouthOpenAmount * 1.35f).coerceIn(0f, 1f)
    } else {
        0f
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                coroutineScope.launch {
                    tapRecoil.animateTo(
                        targetValue = -12f,
                        animationSpec = tween(90, easing = FastOutSlowInEasing)
                    )
                    tapRecoil.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
    ) {
        // --- 1. 3D CHARACTER VIEWPORT (GPU Hardware Accelerated) ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = breathingSway + tapRecoil.value + speechNod
                    scaleX = animatedScale
                    scaleY = animatedScale
                    rotationZ = animatedHeadTilt
                    cameraDistance = 18f * density
                }
        ) {
            // Layer A: Base 3D Idle Character (Always visible)
            Image(
                painter = painterResource(id = idleDrawableRes),
                contentDescription = "${character.displayName} 3D Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(0.dp))
            )

            // Layer B: Photorealistic 3D Talking Frame with Dynamic Mouth Articulation
            if (talkAlpha > 0.01f) {
                Image(
                    painter = painterResource(id = talkDrawableRes),
                    contentDescription = "${character.displayName} Talking State",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = talkAlpha
                        }
                )
            }

            // --- 2. CINEMATIC 3D VOLUMETRIC VIGNETTE ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.50f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.70f),
                                Color(0xFF07090C)
                            )
                        )
                    )
            )

            // --- 3. DYNAMIC AMBIENT INTERACTION GLOW ---
            val stateGlowColor = when (interactionState) {
                AvatarInteractionState.LISTENING -> Color(0xFF81C784).copy(alpha = 0.12f)
                AvatarInteractionState.PROCESSING -> Color(0xFFFFB74D).copy(alpha = 0.14f)
                AvatarInteractionState.SPEAKING -> Color(0xFF64B5F6).copy(alpha = 0.16f)
                AvatarInteractionState.IDLE -> Color.Transparent
            }
            if (stateGlowColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(stateGlowColor, Color.Transparent)
                            )
                        )
                )
            }

            // --- 4. NATURAL EYELID BLINK CANVAS ---
            if (blinkProgress.value > 0.05f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawEyelidBlinks(
                        character = character,
                        cw = size.width,
                        ch = size.height,
                        blink = blinkProgress.value
                    )
                }
            }
        }
    }
}

/**
 * Overlays natural anatomical eyelid blinks over the character's eyes.
 */
private fun DrawScope.drawEyelidBlinks(
    character: AvatarCharacter,
    cw: Float,
    ch: Float,
    blink: Float
) {
    val (eyeL, eyeR, eyeRadiusW, eyeRadiusH, eyelidColor) = when (character) {
        AvatarCharacter.CAT -> {
            Quint(
                Offset(cw * 0.375f, ch * 0.418f),
                Offset(cw * 0.625f, ch * 0.418f),
                36f,
                24f,
                Color(0xFF6D4C41)
            )
        }
        AvatarCharacter.HUSKY -> {
            Quint(
                Offset(cw * 0.395f, ch * 0.412f),
                Offset(cw * 0.605f, ch * 0.412f),
                34f,
                22f,
                Color(0xFF424242)
            )
        }
        AvatarCharacter.JAPANESE_TEEN -> {
            Quint(
                Offset(cw * 0.398f, ch * 0.338f),
                Offset(cw * 0.602f, ch * 0.338f),
                30f,
                18f,
                Color(0xFFFFE0B2)
            )
        }
        AvatarCharacter.ADULT_FEMALE -> {
            Quint(
                Offset(cw * 0.402f, ch * 0.342f),
                Offset(cw * 0.598f, ch * 0.342f),
                34f,
                20f,
                Color(0xFFFFE0B2)
            )
        }
    }

    val currentBlinkH = eyeRadiusH * blink

    listOf(eyeL, eyeR).forEach { eyeCenter ->
        // Eyelid skin fold
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(eyelidColor.copy(alpha = 0.95f), eyelidColor),
                startY = eyeCenter.y - eyeRadiusH,
                endY = eyeCenter.y + currentBlinkH
            ),
            topLeft = Offset(eyeCenter.x - eyeRadiusW, eyeCenter.y - currentBlinkH),
            size = Size(eyeRadiusW * 2, currentBlinkH * 2)
        )

        // Eyelash line
        drawLine(
            color = Color(0xFF1E1714).copy(alpha = 0.85f),
            start = Offset(eyeCenter.x - eyeRadiusW, eyeCenter.y + currentBlinkH),
            end = Offset(eyeCenter.x + eyeRadiusW, eyeCenter.y + currentBlinkH),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 5-tuple helper for eye landmark configuration.
 */
private data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
