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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Renders high-fidelity 3D CGI character models (Tabby Cat, Siberian Husky, Sakura, Elena)
 * with cinematic studio lighting, real-time 3D spatial breathing, spontaneous eye blinking,
 * responsive head-tracking postures, interactive touch physics, and synchronized 60 FPS
 * audio-driven mouth visemes during TalkBack re-voicing.
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

    // 2. Variable Natural Blinking Controller (2-5.5s intervals)
    val blinkProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(Random.nextLong(2200, 5500))
            // Quick eye closure (80ms)
            blinkProgress.animateTo(1f, animationSpec = tween(80, easing = LinearEasing))
            // Gentle opening (120ms)
            blinkProgress.animateTo(0f, animationSpec = tween(120, easing = LinearEasing))
        }
    }

    // 3. Smooth Sinusoidal Breathing Cycle (3.2s period)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingSway by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingSway"
    )

    // 4. Subtle Micro-Drift (Head sway)
    val microDriftAngle by infiniteTransition.animateFloat(
        initialValue = -0.75f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "microDrift"
    )

    // 5. Posture & Head Tilt based on Interaction State
    val targetHeadTilt = when (interactionState) {
        AvatarInteractionState.LISTENING -> 2.2f  // Attentive head tilt
        AvatarInteractionState.PROCESSING -> -1.4f // Thoughtful posture
        AvatarInteractionState.SPEAKING -> (mouthOpenAmount * 2.0f) - 0.5f // Expressive speech cadence
        AvatarInteractionState.IDLE -> microDriftAngle
    }
    val animatedHeadTilt by animateFloatAsState(
        targetValue = targetHeadTilt,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "headTilt"
    )

    val targetScale = when (interactionState) {
        AvatarInteractionState.LISTENING -> 1.035f // Lean forward attentively
        AvatarInteractionState.SPEAKING -> 1.015f + (mouthOpenAmount * 0.015f)
        else -> 1.0f
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // Resolve Photorealistic 3D Model Asset
    val drawableRes = when (character) {
        AvatarCharacter.CAT -> R.drawable.avatar_cat
        AvatarCharacter.HUSKY -> R.drawable.avatar_husky
        AvatarCharacter.JAPANESE_TEEN -> R.drawable.avatar_sakura
        AvatarCharacter.ADULT_FEMALE -> R.drawable.avatar_elena
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
                        animationSpec = tween(100, easing = FastOutSlowInEasing)
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
        val viewportW = maxWidth
        val viewportH = maxHeight

        // --- 1. 3D CHARACTER LAYER (Hardware Accelerated Graphics Layer) ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = breathingSway + tapRecoil.value
                    scaleX = animatedScale
                    scaleY = animatedScale
                    rotationZ = animatedHeadTilt
                    cameraDistance = 18f * density
                }
        ) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "${character.displayName} 3D Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(0.dp))
            )

            // --- 2. CINEMATIC 3D VOLUMETRIC VIGNETTE & LIGHTING ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f),
                                Color(0xFF07090C)
                            )
                        )
                    )
            )

            // --- 3. DYNAMIC AMBIENT STATE GLOW ---
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

            // --- 4. REAL-TIME 60 FPS 3D MOUTH VISEME & BLINK CANVAS ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cw = size.width
                val ch = size.height

                // Render spontaneous eyelid blinks
                if (blinkProgress.value > 0.05f) {
                    drawEyelidBlinks(
                        character = character,
                        cw = cw,
                        ch = ch,
                        blink = blinkProgress.value
                    )
                }

                // Render dynamic speech mouth visemes
                if (mouthOpenAmount > 0.03f) {
                    drawSpeechVisemes(
                        character = character,
                        cw = cw,
                        ch = ch,
                        mouthOpen = mouthOpenAmount
                    )
                }
            }
        }
    }
}

/**
 * Renders real-time 3D mouth articulation mapped to each character's anatomical facial coordinates.
 */
private fun DrawScope.drawSpeechVisemes(
    character: AvatarCharacter,
    cw: Float,
    ch: Float,
    mouthOpen: Float
) {
    when (character) {
        AvatarCharacter.CAT -> {
            // Cat Mouth position: cx = 50%, cy = 56.5%
            val mx = cw * 0.50f
            val my = ch * 0.565f
            val maxOpenH = 34f * mouthOpen
            val mouthW = 38f + (mouthOpen * 6f)

            // Deep oral cavity
            val cavity = Path().apply {
                moveTo(mx - mouthW, my)
                quadraticBezierTo(mx, my + maxOpenH * 1.35f, mx + mouthW, my)
                quadraticBezierTo(mx, my - maxOpenH * 0.25f, mx - mouthW, my)
                close()
            }
            drawPath(cavity, Color(0xFF2C1010))

            // Tongue tip
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFF8A80), Color(0xFFD81B60)),
                    center = Offset(mx, my + maxOpenH * 0.7f),
                    radius = 16f
                ),
                topLeft = Offset(mx - 15f, my + maxOpenH * 0.35f),
                size = Size(30f, maxOpenH * 0.75f)
            )

            // Upper soft fangs
            drawPath(
                Path().apply {
                    moveTo(mx - 18f, my)
                    lineTo(mx - 14f, my + 6f)
                    lineTo(mx - 10f, my)
                    close()
                },
                Color.White.copy(alpha = 0.92f)
            )
            drawPath(
                Path().apply {
                    moveTo(mx + 10f, my)
                    lineTo(mx + 14f, my + 6f)
                    lineTo(mx + 18f, my)
                    close()
                },
                Color.White.copy(alpha = 0.92f)
            )

            // Feline lip contour
            drawPath(
                Path().apply {
                    moveTo(mx - mouthW - 3f, my - 1f)
                    quadraticBezierTo(mx, my + maxOpenH * 0.2f, mx + mouthW + 3f, my - 1f)
                },
                Color(0xFF3E2723),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
        }

        AvatarCharacter.HUSKY -> {
            // Husky Muzzle/Mouth position: cx = 50%, cy = 63.8%
            val mx = cw * 0.50f
            val my = ch * 0.638f
            val maxOpenH = 46f * mouthOpen
            val mouthW = 46f + (mouthOpen * 8f)

            // Dark canine oral cavity
            val cavity = Path().apply {
                moveTo(mx - mouthW, my)
                quadraticBezierTo(mx, my + maxOpenH * 1.45f, mx + mouthW, my)
                quadraticBezierTo(mx, my - maxOpenH * 0.2f, mx - mouthW, my)
                close()
            }
            drawPath(cavity, Color(0xFF220C0C))

            // Realistic canine tongue hanging comfortably
            val tongueH = maxOpenH * 1.15f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF8A80), Color(0xFFE57373), Color(0xFFC2185B)),
                    startY = my + 8f,
                    endY = my + tongueH + 16f
                ),
                topLeft = Offset(mx - 20f, my + 6f),
                size = Size(40f, tongueH + 8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )

            // Tongue central groove
            drawLine(
                color = Color(0xFFB71C1C).copy(alpha = 0.45f),
                start = Offset(mx, my + 10f),
                end = Offset(mx, my + tongueH + 2f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )

            // Canine black lip rim
            drawPath(
                cavity,
                Color(0xFF1E1E1E),
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )
        }

        AvatarCharacter.JAPANESE_TEEN -> {
            // Sakura Lips position: cx = 50%, cy = 49.6%
            val mx = cw * 0.50f
            val my = ch * 0.496f
            val maxOpenH = 26f * mouthOpen
            val mouthW = 28f + (mouthOpen * 5f)

            // Natural oral cavity
            val mouthPath = Path().apply {
                moveTo(mx - mouthW, my)
                quadraticBezierTo(mx, my + maxOpenH * 1.25f, mx + mouthW, my)
                quadraticBezierTo(mx, my - maxOpenH * 0.28f, mx - mouthW, my)
                close()
            }
            drawPath(mouthPath, Color(0xFF5B1724))

            // Pearlescent upper teeth flash
            drawRoundRect(
                color = Color(0xFFFAF9F6),
                topLeft = Offset(mx - 15f, my - 1f),
                size = Size(30f, (maxOpenH * 0.42f).coerceAtLeast(3f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Soft tongue
            drawOval(
                color = Color(0xFFE57373),
                topLeft = Offset(mx - 12f, my + maxOpenH * 0.45f),
                size = Size(24f, maxOpenH * 0.65f)
            )

            // Delicate Coral Pink Lip Contour
            val lipContour = Path().apply {
                moveTo(mx - mouthW - 2f, my)
                quadraticBezierTo(mx, my + maxOpenH * 1.35f, mx + mouthW + 2f, my)
            }
            drawPath(lipContour, Color(0xFFE91E63).copy(alpha = 0.75f), style = Stroke(width = 3.2f, cap = StrokeCap.Round))
        }

        AvatarCharacter.ADULT_FEMALE -> {
            // Elena Lips position: cx = 50%, cy = 50.8%
            val mx = cw * 0.50f
            val my = ch * 0.508f
            val maxOpenH = 30f * mouthOpen
            val mouthW = 34f + (mouthOpen * 6f)

            // Deep shaded interior
            val mouthPath = Path().apply {
                moveTo(mx - mouthW, my)
                quadraticBezierTo(mx, my + maxOpenH * 1.32f, mx + mouthW, my)
                quadraticBezierTo(mx, my - maxOpenH * 0.32f, mx - mouthW, my)
                close()
            }
            drawPath(mouthPath, Color(0xFF4A0E17))

            // White sculpted upper teeth
            drawRoundRect(
                color = Color(0xFFFCFCFC),
                topLeft = Offset(mx - 18f, my - 2f),
                size = Size(36f, (maxOpenH * 0.45f).coerceAtLeast(4f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Natural tongue
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEF9A9A), Color(0xFFC2185B)),
                    center = Offset(mx, my + maxOpenH * 0.7f),
                    radius = 18f
                ),
                topLeft = Offset(mx - 16f, my + maxOpenH * 0.40f),
                size = Size(32f, maxOpenH * 0.70f)
            )

            // Sculpted Nude-Rose Cupid's Bow & Lower Lip
            val upperLip = Path().apply {
                moveTo(mx - mouthW - 3f, my)
                quadraticBezierTo(mx - 12f, my - 6f, mx, my - 3f)
                quadraticBezierTo(mx + 12f, my - 6f, mx + mouthW + 3f, my)
            }
            val lowerLip = Path().apply {
                moveTo(mx - mouthW - 2f, my)
                quadraticBezierTo(mx, my + maxOpenH * 1.40f, mx + mouthW + 2f, my)
            }
            drawPath(upperLip, Color(0xFFAD1457).copy(alpha = 0.85f), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
            drawPath(lowerLip, Color(0xFFC2185B).copy(alpha = 0.85f), style = Stroke(width = 3.8f, cap = StrokeCap.Round))
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
