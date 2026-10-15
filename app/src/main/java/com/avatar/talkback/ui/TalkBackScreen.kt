package com.avatar.talkback.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.avatar.talkback.ui.hud.MainViewportHud
import com.avatar.talkback.ui.lock.ToddlerLockOverlay
import com.avatar.talkback.ui.state.AvatarUiEvent
import com.avatar.talkback.ui.state.AvatarViewModel

/**
 * Root Screen composing:
 * 1. 3D Avatar Viewport (occupying 75-85% viewport in portrait)
 * 2. Main Viewport HUD overlay (Mic toggle, Mic lock, Toddler lock trigger)
 * 3. Full-screen Toddler Lock Interceptor with Slide to Unlock
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
            .background(Color(0xFF0F1115))
    ) {
        // --- 1. 3D Avatar Viewport Layer ---
        // Native Sceneview / Filament 3D Viewport or Unity/Unreal SurfaceView
        Avatar3DViewport(
            modelPath = uiState.currentAvatar.modelAssetPath,
            interactionState = uiState.interactionState,
            modifier = Modifier.fillMaxSize()
        )

        // --- 2. Main HUD (Ignored/Hidden when Toddler Lock is engaged) ---
        if (!uiState.isToddlerLocked) {
            MainViewportHud(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- 3. Toddler Lock Interception Layer ---
        ToddlerLockOverlay(
            isLocked = uiState.isToddlerLocked,
            onUnlock = { viewModel.onEvent(AvatarUiEvent.DismissToddlerLock) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Placeholder for the 3D Avatar Rendering Node (Sceneview / Filament).
 * In production, wraps io.github.sceneview.SceneView or SurfaceView.
 */
@Composable
private fun Avatar3DViewport(
    modelPath: String,
    interactionState: com.avatar.talkback.ui.state.AvatarInteractionState,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Native 3D scene rendering occurs here
        // The camera is calibrated to frame the avatar character from waist/chest-up,
        // occupying 75-85% screen height in standard 9:16 mobile portrait ratio.
    }
}
