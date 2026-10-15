package com.avatar.talkback.ui.avatar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.avatar.talkback.ui.state.AvatarCharacter
import com.avatar.talkback.ui.state.AvatarInteractionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

/**
 * Photorealistic Character Viewport Node.
 * Frames the character to dominate 75-85% of the viewport height in portrait mode.
 * Renders lifelike breathing, variable spontaneous blinking (2-6s), eye dilation,
 * and real-time audio-driven mouth visemes during speech.
 */
@Composable
fun AvatarViewportRenderer(
    character: AvatarCharacter,
    interactionState: AvatarInteractionState,
    mouthOpenAmount: Float, // 0.0f (closed) to 1.0f (fully open for vowels)
    modifier: Modifier = Modifier
) {
    // 1. Spontaneous blinking controller (2-6s variable intervals)
    val blinkProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(Random.nextLong(2200, 5500))
            // Quick eye shut (70ms)
            blinkProgress.animateTo(1f, animationSpec = tween(70, easing = LinearEasing))
            // Eye opening (100ms)
            blinkProgress.animateTo(0f, animationSpec = tween(100, easing = LinearEasing))
        }
    }

    // 2. Breathing cycle (3.5s smooth sinusoidal movement)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingOffset"
    )

    // 3. Subtle ear/hair micro-twitches
    val earTwitchAngle by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "earTwitch"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = (size.height / 2f) + breathingOffset

            when (character) {
                AvatarCharacter.CAT -> drawCatAvatar(
                    cx = cx,
                    cy = cy,
                    blink = blinkProgress.value,
                    mouthOpen = mouthOpenAmount,
                    twitch = earTwitchAngle,
                    isSpeaking = interactionState == AvatarInteractionState.SPEAKING
                )
                AvatarCharacter.HUSKY -> drawHuskyAvatar(
                    cx = cx,
                    cy = cy,
                    blink = blinkProgress.value,
                    mouthOpen = mouthOpenAmount,
                    twitch = earTwitchAngle,
                    isSpeaking = interactionState == AvatarInteractionState.SPEAKING
                )
                AvatarCharacter.JAPANESE_TEEN -> drawSakuraAvatar(
                    cx = cx,
                    cy = cy,
                    blink = blinkProgress.value,
                    mouthOpen = mouthOpenAmount,
                    isSpeaking = interactionState == AvatarInteractionState.SPEAKING
                )
                AvatarCharacter.ADULT_FEMALE -> drawElenaAvatar(
                    cx = cx,
                    cy = cy,
                    blink = blinkProgress.value,
                    mouthOpen = mouthOpenAmount,
                    isSpeaking = interactionState == AvatarInteractionState.SPEAKING
                )
            }
        }
    }
}

// ==========================================
// 1. CHARACTER A: HYPER-REALISTIC DOMESTIC CAT
// ==========================================
private fun DrawScope.drawCatAvatar(
    cx: Float,
    cy: Float,
    blink: Float,
    mouthOpen: Float,
    twitch: Float,
    isSpeaking: Boolean
) {
    val headRadius = size.width * 0.38f

    // Soft Studio Rim Lighting & Shoulders
    val bodyPath = Path().apply {
        moveTo(cx - headRadius * 1.3f, size.height)
        quadraticTo(cx - headRadius * 0.9f, cy + headRadius * 0.8f, cx - headRadius * 0.7f, cy + headRadius * 0.6f)
        lineTo(cx + headRadius * 0.7f, cy + headRadius * 0.6f)
        quadraticTo(cx + headRadius * 0.9f, cy + headRadius * 0.8f, cx + headRadius * 1.3f, size.height)
        close()
    }
    drawPath(
        path = bodyPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF6D4C41), Color(0xFF3E2723))
        )
    )

    // Ears with realistic pink inner fleece & twitching
    drawCatEar(cx - headRadius * 0.65f, cy - headRadius * 0.65f, isLeft = true, angle = twitch)
    drawCatEar(cx + headRadius * 0.65f, cy - headRadius * 0.65f, isLeft = false, angle = -twitch)

    // Cat Head Base with soft Tabby fur gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF8D6E63), Color(0xFF4E342E), Color(0xFF2D1E18)),
            center = Offset(cx, cy),
            radius = headRadius
        ),
        radius = headRadius,
        center = Offset(cx, cy)
    )

    // Tabby Forehead Markings
    val stripeColor = Color(0xFF261A15).copy(alpha = 0.55f)
    for (i in -2..2) {
        val sx = cx + (i * 24f)
        drawLine(
            color = stripeColor,
            start = Offset(sx, cy - headRadius * 0.75f),
            end = Offset(sx + (i * 6f), cy - headRadius * 0.35f),
            strokeWidth = 9f,
            cap = StrokeCap.Round
        )
    }

    // Eyes: Natural feline almond shape with glowing green-gold irises & pupil dilation
    val eyeOffsetX = headRadius * 0.42f
    val eyeOffsetY = cy - headRadius * 0.08f
    val eyeRadiusX = headRadius * 0.22f
    val eyeRadiusY = headRadius * 0.16f * (1f - blink).coerceAtLeast(0.04f)

    listOf(cx - eyeOffsetX, cx + eyeOffsetX).forEach { eyeX ->
        // Sclera / Gold Iris
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDD835), Color(0xFF7CB342), Color(0xFF33691E)),
                center = Offset(eyeX, eyeOffsetY),
                radius = eyeRadiusX
            ),
            topLeft = Offset(eyeX - eyeRadiusX, eyeOffsetY - eyeRadiusY),
            size = Size(eyeRadiusX * 2, eyeRadiusY * 2)
        )
        // Slit / Dilation Pupil
        val pupilWidth = eyeRadiusX * 0.35f
        val pupilHeight = eyeRadiusY * 0.90f
        drawOval(
            color = Color(0xFF0A0A0A),
            topLeft = Offset(eyeX - pupilWidth / 2f, eyeOffsetY - pupilHeight / 2f),
            size = Size(pupilWidth, pupilHeight)
        )
        // High-gloss eye reflection
        if (blink < 0.3f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 5f,
                center = Offset(eyeX - eyeRadiusX * 0.25f, eyeOffsetY - eyeRadiusY * 0.25f)
            )
        }
    }

    // Pink Soft Nose
    val noseY = cy + headRadius * 0.20f
    val nosePath = Path().apply {
        moveTo(cx - 20f, noseY)
        lineTo(cx + 20f, noseY)
        lineTo(cx, noseY + 22f)
        close()
    }
    drawPath(nosePath, Color(0xFFF48FB1))

    // Real-Time Lip-Sync Mouth
    val mouthY = noseY + 24f
    val dynamicMouthOpenPx = mouthOpen * 42f

    if (dynamicMouthOpenPx > 4f) {
        // Open Mouth during TalkBack Speech
        val openMouthPath = Path().apply {
            moveTo(cx - 30f, mouthY)
            quadraticTo(cx, mouthY + dynamicMouthOpenPx * 1.4f, cx + 30f, mouthY)
            quadraticTo(cx, mouthY + dynamicMouthOpenPx * 0.3f, cx - 30f, mouthY)
            close()
        }
        drawPath(openMouthPath, Color(0xFF3E1B1B))
        // Tongue tip
        drawCircle(
            color = Color(0xFFEF9A9A),
            radius = 14f * mouthOpen,
            center = Offset(cx, mouthY + dynamicMouthOpenPx * 0.7f)
        )
    } else {
        // Closed W-shaped cat muzzle
        val leftMuzzle = Path().apply {
            moveTo(cx, mouthY)
            quadraticTo(cx - 22f, mouthY + 16f, cx - 44f, mouthY + 8f)
        }
        val rightMuzzle = Path().apply {
            moveTo(cx, mouthY)
            quadraticTo(cx + 22f, mouthY + 16f, cx + 44f, mouthY + 8f)
        }
        drawPath(leftMuzzle, Color(0xFF261A15), style = Stroke(width = 5f, cap = StrokeCap.Round))
        drawPath(rightMuzzle, Color(0xFF261A15), style = Stroke(width = 5f, cap = StrokeCap.Round))
    }

    // Realistic Whiskers
    val whiskerColor = Color.White.copy(alpha = 0.85f)
    for (i in -1..1) {
        val yOff = i * 14f
        // Left Whiskers
        drawLine(
            color = whiskerColor,
            start = Offset(cx - 38f, mouthY + yOff),
            end = Offset(cx - headRadius * 1.35f, mouthY + yOff + (i * 18f)),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
        // Right Whiskers
        drawLine(
            color = whiskerColor,
            start = Offset(cx + 38f, mouthY + yOff),
            end = Offset(cx + headRadius * 1.35f, mouthY + yOff + (i * 18f)),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCatEar(x: Float, y: Float, isLeft: Boolean, angle: Float) {
    val earPath = Path().apply {
        if (isLeft) {
            moveTo(x - 55f, y + 60f)
            lineTo(x - 25f + (angle * 4f), y - 90f)
            lineTo(x + 45f, y + 20f)
        } else {
            moveTo(x - 45f, y + 20f)
            lineTo(x + 25f + (angle * 4f), y - 90f)
            lineTo(x + 55f, y + 60f)
        }
        close()
    }
    drawPath(earPath, Color(0xFF4E342E))
    // Inner ear pink fleece
    val innerPath = Path().apply {
        if (isLeft) {
            moveTo(x - 38f, y + 42f)
            lineTo(x - 22f, y - 65f)
            lineTo(x + 28f, y + 16f)
        } else {
            moveTo(x - 28f, y + 16f)
            lineTo(x + 22f, y - 65f)
            lineTo(x + 38f, y + 42f)
        }
        close()
    }
    drawPath(innerPath, Color(0xFFF8BBD0))
}

// ==========================================
// 2. CHARACTER B: SIBERIAN HUSKY
// ==========================================
private fun DrawScope.drawHuskyAvatar(
    cx: Float,
    cy: Float,
    blink: Float,
    mouthOpen: Float,
    twitch: Float,
    isSpeaking: Boolean
) {
    val headRadius = size.width * 0.40f

    // Husky Shoulders & Thick Double Coat
    val bodyPath = Path().apply {
        moveTo(cx - headRadius * 1.35f, size.height)
        quadraticTo(cx - headRadius, cy + headRadius * 0.8f, cx - headRadius * 0.7f, cy + headRadius * 0.6f)
        lineTo(cx + headRadius * 0.7f, cy + headRadius * 0.6f)
        quadraticTo(cx + headRadius, cy + headRadius * 0.8f, cx + headRadius * 1.35f, size.height)
        close()
    }
    drawPath(bodyPath, Brush.verticalGradient(listOf(Color(0xFF424242), Color(0xFF1E1E1E))))

    // Husky Erect Triangle Ears
    drawHuskyEar(cx - headRadius * 0.68f, cy - headRadius * 0.68f, isLeft = true, angle = twitch)
    drawHuskyEar(cx + headRadius * 0.68f, cy - headRadius * 0.68f, isLeft = false, angle = -twitch)

    // Outer Black/Silver Coat
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF616161), Color(0xFF263238), Color(0xFF12171A)),
            center = Offset(cx, cy),
            radius = headRadius
        ),
        radius = headRadius,
        center = Offset(cx, cy)
    )

    // Distinct White Husky Facial Mask
    val maskPath = Path().apply {
        moveTo(cx, cy - headRadius * 0.65f)
        quadraticTo(cx - headRadius * 0.35f, cy - headRadius * 0.25f, cx - headRadius * 0.75f, cy + headRadius * 0.15f)
        quadraticTo(cx - headRadius * 0.6f, cy + headRadius * 0.75f, cx, cy + headRadius * 0.82f)
        quadraticTo(cx + headRadius * 0.6f, cy + headRadius * 0.75f, cx + headRadius * 0.75f, cy + headRadius * 0.15f)
        quadraticTo(cx + headRadius * 0.35f, cy - headRadius * 0.25f, cx, cy - headRadius * 0.65f)
        close()
    }
    drawPath(maskPath, Color(0xFFF5F5F5))

    // Heterochromia Eyes (Left: Piercing Ice Blue, Right: Warm Amber Brown)
    val eyeOffsetX = headRadius * 0.38f
    val eyeOffsetY = cy - headRadius * 0.05f
    val eyeW = headRadius * 0.20f
    val eyeH = headRadius * 0.14f * (1f - blink).coerceAtLeast(0.04f)

    // Left Eye (Ice Blue)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF81D4FA), Color(0xFF0288D1), Color(0xFF01579B)),
            center = Offset(cx - eyeOffsetX, eyeOffsetY),
            radius = eyeW
        ),
        topLeft = Offset(cx - eyeOffsetX - eyeW, eyeOffsetY - eyeH),
        size = Size(eyeW * 2, eyeH * 2)
    )
    drawCircle(Color.Black, radius = eyeW * 0.45f, center = Offset(cx - eyeOffsetX, eyeOffsetY))

    // Right Eye (Warm Amber Brown)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFB74D), Color(0xFFE65100), Color(0xFF4E342E)),
            center = Offset(cx + eyeOffsetX, eyeOffsetY),
            radius = eyeW
        ),
        topLeft = Offset(cx + eyeOffsetX - eyeW, eyeOffsetY - eyeH),
        size = Size(eyeW * 2, eyeH * 2)
    )
    drawCircle(Color.Black, radius = eyeW * 0.45f, center = Offset(cx + eyeOffsetX, eyeOffsetY))

    // Large Canine Black Nose
    val noseY = cy + headRadius * 0.28f
    drawRoundRect(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(cx - 36f, noseY),
        size = Size(72f, 44f),
        cornerRadius = CornerRadius(20f, 20f)
    )

    // TalkBack Mouth & Panting Tongue
    val mouthY = noseY + 50f
    val dynamicMouth = mouthOpen * 56f

    if (dynamicMouth > 5f) {
        // Open Bark/Talk Mouth
        val mouthPath = Path().apply {
            moveTo(cx - 45f, mouthY)
            quadraticTo(cx, mouthY + dynamicMouth * 1.5f, cx + 45f, mouthY)
            close()
        }
        drawPath(mouthPath, Color(0xFF2E1212))
        // Pink canine tongue hanging
        drawRoundRect(
            color = Color(0xFFFF8A80),
            topLeft = Offset(cx - 24f, mouthY + 12f),
            size = Size(48f, dynamicMouth * 1.1f),
            cornerRadius = CornerRadius(18f, 18f)
        )
    } else {
        // Subtle friendly closed canine smile
        drawLine(
            color = Color(0xFF212121),
            start = Offset(cx, noseY + 44f),
            end = Offset(cx, mouthY),
            strokeWidth = 6f
        )
        val smilePath = Path().apply {
            moveTo(cx - 38f, mouthY - 4f)
            quadraticTo(cx, mouthY + 12f, cx + 38f, mouthY - 4f)
        }
        drawPath(smilePath, Color(0xFF212121), style = Stroke(width = 6f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawHuskyEar(x: Float, y: Float, isLeft: Boolean, angle: Float) {
    val earPath = Path().apply {
        if (isLeft) {
            moveTo(x - 50f, y + 80f)
            lineTo(x - 20f + (angle * 5f), y - 110f)
            lineTo(x + 50f, y + 40f)
        } else {
            moveTo(x - 50f, y + 40f)
            lineTo(x + 20f + (angle * 5f), y - 110f)
            lineTo(x + 50f, y + 80f)
        }
        close()
    }
    drawPath(earPath, Color(0xFF212121))
    // Inner white fluff
    val innerFluff = Path().apply {
        if (isLeft) {
            moveTo(x - 30f, y + 60f)
            lineTo(x - 15f, y - 75f)
            lineTo(x + 30f, y + 30f)
        } else {
            moveTo(x - 30f, y + 30f)
            lineTo(x + 15f, y - 75f)
            lineTo(x + 30f, y + 60f)
        }
        close()
    }
    drawPath(innerFluff, Color(0xFFEEEEEE))
}

// ==========================================
// 3. CHARACTER C: JAPANESE TEEN (SAKURA)
// ==========================================
private fun DrawScope.drawSakuraAvatar(
    cx: Float,
    cy: Float,
    blink: Float,
    mouthOpen: Float,
    isSpeaking: Boolean
) {
    val headRadius = size.width * 0.36f

    // Pastel Floral Kimono Attire with layered collar
    val kimonoPath = Path().apply {
        moveTo(cx - headRadius * 1.4f, size.height)
        lineTo(cx - headRadius * 0.6f, cy + headRadius * 0.85f)
        lineTo(cx, cy + headRadius * 1.1f)
        lineTo(cx + headRadius * 0.6f, cy + headRadius * 0.85f)
        lineTo(cx + headRadius * 1.4f, size.height)
        close()
    }
    drawPath(
        kimonoPath,
        Brush.linearGradient(
            colors = listOf(Color(0xFFF8BBD0), Color(0xFFE1BEE7), Color(0xFFFFF9C4))
        )
    )
    // Kimono White Inner Collar
    val collarPath = Path().apply {
        moveTo(cx - 36f, cy + headRadius * 0.85f)
        lineTo(cx, cy + headRadius * 1.05f)
        lineTo(cx + 36f, cy + headRadius * 0.85f)
    }
    drawPath(collarPath, Color.White, style = Stroke(width = 8f))

    // Slender Neck
    drawRect(
        color = Color(0xFFFFE0B2),
        topLeft = Offset(cx - 28f, cy + headRadius * 0.65f),
        size = Size(56f, headRadius * 0.35f)
    )

    // Soft Anime/Realistic Bob Hair (Back layer)
    drawCircle(
        color = Color(0xFF2C221E),
        radius = headRadius * 1.15f,
        center = Offset(cx, cy - 10f)
    )

    // Face Oval: Subsurface scattering soft skin tone
    val facePath = Path().apply {
        moveTo(cx - headRadius * 0.72f, cy - headRadius * 0.3f)
        quadraticTo(cx - headRadius * 0.70f, cy + headRadius * 0.5f, cx, cy + headRadius * 0.85f)
        quadraticTo(cx + headRadius * 0.70f, cy + headRadius * 0.5f, cx + headRadius * 0.72f, cy - headRadius * 0.3f)
        quadraticTo(cx, cy - headRadius * 1.05f, cx - headRadius * 0.72f, cy - headRadius * 0.3f)
        close()
    }
    drawPath(
        facePath,
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2), Color(0xFFFFCC80)),
            center = Offset(cx, cy),
            radius = headRadius
        )
    )

    // Gentle Pink Cheek Blush
    drawCircle(Color(0xFFFF8A80).copy(alpha = 0.28f), radius = 32f, center = Offset(cx - headRadius * 0.45f, cy + headRadius * 0.18f))
    drawCircle(Color(0xFFFF8A80).copy(alpha = 0.28f), radius = 32f, center = Offset(cx + headRadius * 0.45f, cy + headRadius * 0.18f))

    // Gentle Brown Eyes (Sakura's warm gaze)
    val eyeOffsetX = headRadius * 0.34f
    val eyeOffsetY = cy - headRadius * 0.05f
    val eyeW = 28f
    val eyeH = 18f * (1f - blink).coerceAtLeast(0.04f)

    listOf(cx - eyeOffsetX, cx + eyeOffsetX).forEach { eyeX ->
        // Eyelash curve
        val lashPath = Path().apply {
            moveTo(eyeX - eyeW * 1.2f, eyeOffsetY - eyeH * 0.8f)
            quadraticTo(eyeX, eyeOffsetY - eyeH * 1.4f, eyeX + eyeW * 1.2f, eyeOffsetY - eyeH * 0.6f)
        }
        drawPath(lashPath, Color(0xFF1E1714), style = Stroke(width = 4.5f, cap = StrokeCap.Round))

        // Warm Deep Brown Iris
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8D6E63), Color(0xFF4E342E), Color(0xFF1A120E)),
                center = Offset(eyeX, eyeOffsetY),
                radius = eyeW
            ),
            topLeft = Offset(eyeX - eyeW, eyeOffsetY - eyeH),
            size = Size(eyeW * 2, eyeH * 2)
        )
        // Gloss catchlight
        if (blink < 0.25f) {
            drawCircle(Color.White, radius = 4f, center = Offset(eyeX - 8f, eyeOffsetY - 4f))
        }
    }

    // Petite Japanese Nose
    drawLine(
        color = Color(0xFFD7CCC8),
        start = Offset(cx, cy + headRadius * 0.20f),
        end = Offset(cx - 3f, cy + headRadius * 0.26f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // Real-Time TalkBack Lip-Sync (Delicate Rose Lips)
    val mouthY = cy + headRadius * 0.46f
    val dynamicMouth = mouthOpen * 36f

    if (dynamicMouth > 3f) {
        // Speaking Viseme
        val lipPath = Path().apply {
            moveTo(cx - 24f, mouthY)
            quadraticTo(cx, mouthY + dynamicMouth * 1.2f, cx + 24f, mouthY)
            quadraticTo(cx, mouthY - dynamicMouth * 0.3f, cx - 24f, mouthY)
            close()
        }
        drawPath(lipPath, Color(0xFFD81B60))
    } else {
        // Natural gentle closed smile
        val smilePath = Path().apply {
            moveTo(cx - 20f, mouthY)
            quadraticTo(cx, mouthY + 6f, cx + 20f, mouthY)
        }
        drawPath(smilePath, Color(0xFFE91E63), style = Stroke(width = 4f, cap = StrokeCap.Round))
    }

    // Shoulder-Length Bangs & Front Hair Strands
    for (i in -3..3) {
        val bx = cx + (i * 26f)
        val strandPath = Path().apply {
            moveTo(bx, cy - headRadius * 0.95f)
            quadraticTo(bx + (i * 8f), cy - headRadius * 0.45f, bx + (i * 14f), cy - headRadius * 0.18f)
        }
        drawPath(strandPath, Color(0xFF2C221E), style = Stroke(width = 22f, cap = StrokeCap.Round))
    }
}

// ==========================================
// 4. CHARACTER D: ADULT FEMALE (ANA DE ARMAS STYLE)
// ==========================================
private fun DrawScope.drawElenaAvatar(
    cx: Float,
    cy: Float,
    blink: Float,
    mouthOpen: Float,
    isSpeaking: Boolean
) {
    val headRadius = size.width * 0.38f

    // Cinematic Evening Neckline & Studio Key Light
    val dressPath = Path().apply {
        moveTo(cx - headRadius * 1.4f, size.height)
        lineTo(cx - headRadius * 0.5f, cy + headRadius * 0.95f)
        lineTo(cx + headRadius * 0.5f, cy + headRadius * 0.95f)
        lineTo(cx + headRadius * 1.4f, size.height)
        close()
    }
    drawPath(dressPath, Brush.verticalGradient(listOf(Color(0xFF212121), Color(0xFF0D0D0D))))

    // Elegant Neck & Clavicle
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFE0B2), Color(0xFFF5CBA7))),
        topLeft = Offset(cx - 32f, cy + headRadius * 0.65f),
        size = Size(64f, headRadius * 0.40f)
    )

    // Brunette Wavy Hair (Background framing)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF3E2723), Color(0xFF1E120C)),
            center = Offset(cx, cy - 20f),
            radius = headRadius * 1.25f
        ),
        radius = headRadius * 1.20f,
        center = Offset(cx, cy - 10f)
    )

    // Sculpted Jawline & Cheekbones (Ana de Armas likeness)
    val facePath = Path().apply {
        moveTo(cx - headRadius * 0.72f, cy - headRadius * 0.35f)
        quadraticTo(cx - headRadius * 0.68f, cy + headRadius * 0.45f, cx, cy + headRadius * 0.88f)
        quadraticTo(cx + headRadius * 0.68f, cy + headRadius * 0.45f, cx + headRadius * 0.72f, cy - headRadius * 0.35f)
        quadraticTo(cx, cy - headRadius * 1.05f, cx - headRadius * 0.72f, cy - headRadius * 0.35f)
        close()
    }
    drawPath(
        facePath,
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2), Color(0xFFE0BB95)),
            center = Offset(cx, cy),
            radius = headRadius
        )
    )

    // Soft Contour & Warm Natural Blush
    drawCircle(Color(0xFFE57373).copy(alpha = 0.22f), radius = 38f, center = Offset(cx - headRadius * 0.46f, cy + headRadius * 0.16f))
    drawCircle(Color(0xFFE57373).copy(alpha = 0.22f), radius = 38f, center = Offset(cx + headRadius * 0.46f, cy + headRadius * 0.16f))

    // Piercing Hazel-Green Eyes with Defined Winged Liner
    val eyeOffsetX = headRadius * 0.36f
    val eyeOffsetY = cy - headRadius * 0.05f
    val eyeW = 32f
    val eyeH = 19f * (1f - blink).coerceAtLeast(0.04f)

    listOf(cx - eyeOffsetX, cx + eyeOffsetX).forEach { eyeX ->
        // Eyeliner
        val linerPath = Path().apply {
            moveTo(eyeX - eyeW * 1.2f, eyeOffsetY - eyeH * 0.7f)
            quadraticTo(eyeX, eyeOffsetY - eyeH * 1.3f, eyeX + eyeW * 1.35f, eyeOffsetY - eyeH * 0.85f)
        }
        drawPath(linerPath, Color(0xFF1A1A1A), style = Stroke(width = 5f, cap = StrokeCap.Round))

        // Hazel-Green Irises
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFC5E1A5), Color(0xFF689F38), Color(0xFF33691E)),
                center = Offset(eyeX, eyeOffsetY),
                radius = eyeW
            ),
            topLeft = Offset(eyeX - eyeW, eyeOffsetY - eyeH),
            size = Size(eyeW * 2, eyeH * 2)
        )
        drawCircle(Color.Black, radius = eyeW * 0.42f, center = Offset(eyeX, eyeOffsetY))

        // Cinematic Specular Highlight
        if (blink < 0.25f) {
            drawCircle(Color.White, radius = 4.5f, center = Offset(eyeX - 7f, eyeOffsetY - 4f))
        }
    }

    // Refined Sculpted Nose
    val nosePath = Path().apply {
        moveTo(cx - 3f, cy + headRadius * 0.05f)
        lineTo(cx - 5f, cy + headRadius * 0.28f)
        quadraticTo(cx, cy + headRadius * 0.32f, cx + 5f, cy + headRadius * 0.28f)
    }
    drawPath(nosePath, Color(0xFFBCAAA4), style = Stroke(width = 3.5f, cap = StrokeCap.Round))

    // Full Expressive Lips (Ana de Armas hallmark)
    val mouthY = cy + headRadius * 0.50f
    val dynamicMouth = mouthOpen * 42f

    if (dynamicMouth > 4f) {
        // Open Speech Viseme
        val openMouthPath = Path().apply {
            moveTo(cx - 32f, mouthY)
            quadraticTo(cx, mouthY + dynamicMouth * 1.3f, cx + 32f, mouthY)
            quadraticTo(cx, mouthY - dynamicMouth * 0.3f, cx - 32f, mouthY)
            close()
        }
        drawPath(openMouthPath, Color(0xFF880E4F))
        // White teeth flash
        drawRect(
            color = Color(0xFFFAFAFA),
            topLeft = Offset(cx - 16f, mouthY - 2f),
            size = Size(32f, 10f)
        )
    } else {
        // Full Natural Nude-Rose Lips
        val upperLip = Path().apply {
            moveTo(cx - 28f, mouthY)
            quadraticTo(cx - 10f, mouthY - 8f, cx, mouthY - 4f)
            quadraticTo(cx + 10f, mouthY - 8f, cx + 28f, mouthY)
            close()
        }
        val lowerLip = Path().apply {
            moveTo(cx - 28f, mouthY)
            quadraticTo(cx, mouthY + 14f, cx + 28f, mouthY)
            close()
        }
        drawPath(upperLip, Color(0xFFC2185B))
        drawPath(lowerLip, Color(0xFFD81B60))
    }

    // Side-swept Wavy Hair Strands
    val hairStrandColor = Color(0xFF2D1E17)
    val strandL = Path().apply {
        moveTo(cx - headRadius * 0.8f, cy - headRadius * 0.7f)
        quadraticTo(cx - headRadius * 0.95f, cy + headRadius * 0.3f, cx - headRadius * 0.7f, cy + headRadius * 0.9f)
    }
    val strandR = Path().apply {
        moveTo(cx + headRadius * 0.75f, cy - headRadius * 0.7f)
        quadraticTo(cx + headRadius * 0.90f, cy + headRadius * 0.3f, cx + headRadius * 0.65f, cy + headRadius * 0.9f)
    }
    drawPath(strandL, hairStrandColor, style = Stroke(width = 24f, cap = StrokeCap.Round))
    drawPath(strandR, hairStrandColor, style = Stroke(width = 24f, cap = StrokeCap.Round))
}
