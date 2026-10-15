package com.avatar.talkback.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avatar.talkback.ui.avatar.AvatarViewportRenderer
import com.avatar.talkback.ui.hud.MainViewportHud
import com.avatar.talkback.ui.lock.ToddlerLockOverlay
import com.avatar.talkback.ui.state.AvatarCharacter
import com.avatar.talkback.ui.state.AvatarInteractionState
import com.avatar.talkback.ui.state.AvatarUiEvent
import com.avatar.talkback.ui.state.AvatarViewModel

/**
 * Root TalkBack Avatar Screen:
 * 1. Dominant character viewport (75-85% screen height) rendering the 4 distinct characters.
 * 2. Character Switcher Bar (Cat, Husky, Sakura, Elena) with active indicator.
 * 3. Live Speech Transcript Banner when speaking.
 * 4. Main HUD overlay (Discreet mic toggle + mic lock + toddler lock trigger).
 * 5. Toddler-proof touch interception with Slide-to-Unlock.
 */
@Composable
fun TalkBackScreen(
    viewModel: AvatarViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141923),
                        Color(0xFF0D1017),
                        Color(0xFF07090C)
                    )
                )
            )
    ) {
        // --- 1. DOMINANT CHARACTER VIEWPORT (75-85% Screen Height) ---
        AvatarViewportRenderer(
            character = uiState.currentAvatar,
            interactionState = uiState.interactionState,
            mouthOpenAmount = uiState.mouthOpenAmount,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
        )

        // --- 2. CHARACTER SWITCHER SELECTOR (Top below status bar) ---
        if (!uiState.isToddlerLocked) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 10.dp)
            ) {
                CharacterSelectorBar(
                    selected = uiState.currentAvatar,
                    onSelect = { viewModel.onEvent(AvatarUiEvent.SelectCharacter(it)) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Persona Tag Info
                Text(
                    text = uiState.currentAvatar.personaTag,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // --- 3. TRANSCRIPT DISPLAY BANNER (Displays what was said/revoiced) ---
        AnimatedVisibility(
            visible = uiState.lastRecognizedText.isNotEmpty() && !uiState.isToddlerLocked,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 105.dp, start = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.60f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "“${uiState.lastRecognizedText}”",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // --- 4. MAIN HUD CONTROLS (Mic Capsule + Toddler Lock Trigger) ---
        if (!uiState.isToddlerLocked) {
            MainViewportHud(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 5. TODDLER-PROOF TOUCH LOCK LAYER ---
        ToddlerLockOverlay(
            isLocked = uiState.isToddlerLocked,
            onUnlock = { viewModel.onEvent(AvatarUiEvent.DismissToddlerLock) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Top selector pill to switch between the 4 avatar characters.
 */
@Composable
private fun CharacterSelectorBar(
    selected: AvatarCharacter,
    onSelect: (AvatarCharacter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(4.dp)
    ) {
        AvatarCharacter.values().forEach { character ->
            val isSelected = character == selected
            val iconEmoji = when (character) {
                AvatarCharacter.CAT -> "🐱 Cat"
                AvatarCharacter.HUSKY -> "🐺 Husky"
                AvatarCharacter.JAPANESE_TEEN -> "🌸 Sakura"
                AvatarCharacter.ADULT_FEMALE -> "👑 Elena"
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.22f)
                        else Color.Transparent
                    )
                    .clickable { onSelect(character) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = iconEmoji,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
