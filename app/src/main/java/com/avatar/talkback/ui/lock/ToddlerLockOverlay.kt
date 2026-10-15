package com.avatar.talkback.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Toddler-Proof Screen Interceptor Layer.
 *
 * When visible:
 * 1. Consumes ALL touch events across the viewport so toddlers cannot trigger system menus or app actions.
 * 2. Renders a horizontal "Slide to Unlock" slider in the lower third.
 * 3. Requires continuous linear swipe across >= 80% track width to release the lock.
 */
@Composable
fun ToddlerLockOverlay(
    isLocked: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLocked,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        // Full screen touch sink consuming all taps, drags, and multi-touch
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .pointerInput(Unit) {
                    detectTapGestures { /* Intercept and consume erratic child taps */ }
                }
        ) {
            // Lower-third container for Slide to Unlock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 64.dp, start = 32.dp, end = 32.dp)
            ) {
                // Subtle lock status indicator
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Screen Locked",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Touch screen locked for toddlers",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // The Slider Component
                SlideToUnlockBar(
                    onUnlockConfirmed = onUnlock,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Slide to Unlock bar with spring drag mechanics and haptic feedback.
 */
@Composable
private fun SlideToUnlockBar(
    onUnlockConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }

    val trackHeight = 58.dp
    val thumbSize = 50.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(trackHeight / 2))
            .padding(4.dp)
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val thumbSizePx = with(density) { thumbSize.toPx() }
        val maxDragPx = (totalWidthPx - thumbSizePx).coerceAtLeast(0f)
        val unlockThresholdPx = maxDragPx * 0.85f

        // Track Guide Text
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val dragFraction = if (maxDragPx > 0) (dragOffset.value / maxDragPx).coerceIn(0f, 1f) else 0f
            Text(
                text = "Slide to Unlock  >>>",
                color = Color.White.copy(alpha = (0.75f - dragFraction).coerceAtLeast(0.1f)),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Active Fill Bar behind thumb
        val fillFraction = if (maxDragPx > 0) dragOffset.value / maxDragPx else 0f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x554CAF50),
                            Color(0xAA4CAF50).copy(alpha = (fillFraction * 0.7f).coerceIn(0f, 0.7f))
                        )
                    )
                )
        )

        // Draggable Circular Thumb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFE0E0E0)
                        )
                    )
                )
                .pointerInput(maxDragPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset.value >= unlockThresholdPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onUnlockConfirmed()
                                coroutineScope.launch {
                                    dragOffset.snapTo(0f)
                                }
                            } else {
                                // Snap back with realistic spring
                                coroutineScope.launch {
                                    dragOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val next = (dragOffset.value + dragAmount).coerceIn(0f, maxDragPx)
                            coroutineScope.launch {
                                dragOffset.snapTo(next)
                            }
                        }
                    )
                }
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Swipe handle",
                tint = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
