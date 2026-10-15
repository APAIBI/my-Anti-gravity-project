package com.avatar.talkback.lipsync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Standard Oculus / ARKit Viseme blendshapes for facial animation.
 */
enum class Viseme(val blendshapeName: String) {
    SIL("viseme_sil"), // Silence / Rest
    PP("viseme_PP"),   // p, b, m
    FF("viseme_FF"),   // f, v
    TH("viseme_TH"),   // th
    DD("viseme_DD"),   // t, d
    KK("viseme_kk"),   // k, g
    CH("viseme_CH"),   // ch, j, sh
    SS("viseme_SS"),   // s, z
    NN("viseme_nn"),   // n, l
    RR("viseme_RR"),   // r
    AA("viseme_aa"),   // a (father)
    E("viseme_E"),     // e (bed)
    I("viseme_I"),     // i (tip)
    O("viseme_O"),     // o (go)
    U("viseme_U")      // u (too)
}

data class VisemeCue(
    val timestampMs: Long,
    val durationMs: Long,
    val viseme: Viseme,
    val targetWeight: Float = 1.0f
)

/**
 * High-performance Lip-Sync & Micro-Behavior Animator.
 * Dispatches interpolated blendshape weights to the 3D Engine at 60 FPS.
 */
class LipSyncEngine(
    private val scope: CoroutineScope,
    private val onUpdateBlendshapes: (Map<String, Float>) -> Unit
) {
    private var animationJob: Job? = null
    private var idleBehaviorJob: Job? = null

    // Current smooth blendshape weights map
    private val currentWeights = mutableMapOf<String, Float>()
    private val targetWeights = mutableMapOf<String, Float>()

    // Idle animation state variables
    private var eyeBlinkWeight = 0f
    private var breathingCycle = 0f

    fun startIdleBehaviors() {
        idleBehaviorJob?.cancel()
        idleBehaviorJob = scope.launch(Dispatchers.Default) {
            var nextBlinkTime = System.currentTimeMillis() + Random.nextLong(2000, 6000)

            while (isActive) {
                val now = System.currentTimeMillis()

                // Spontaneous natural blinking (duration ~180ms)
                if (now >= nextBlinkTime) {
                    executeNaturalBlink()
                    nextBlinkTime = System.currentTimeMillis() + Random.nextLong(2000, 6000)
                }

                // Subtle breathing oscillation (3-4 second cycle)
                breathingCycle = ((System.currentTimeMillis() % 3500) / 3500f) * 2f * Math.PI.toFloat()
                val chestExpansion = (Math.sin(breathingCycle.toDouble()).toFloat() * 0.05f) + 0.05f

                // Post idle facial updates
                synchronized(currentWeights) {
                    targetWeights["eyeBlinkLeft"] = eyeBlinkWeight
                    targetWeights["eyeBlinkRight"] = eyeBlinkWeight
                    targetWeights["chestExpansion"] = chestExpansion
                }

                delay(16) // ~60fps idle loop
            }
        }
    }

    private suspend fun executeNaturalBlink() {
        // Quick blink closing (60ms)
        val closingSteps = 4
        for (i in 1..closingSteps) {
            eyeBlinkWeight = (i.toFloat() / closingSteps)
            delay(15)
        }
        // Quick opening (100ms)
        val openingSteps = 6
        for (i in openingSteps downTo 0) {
            eyeBlinkWeight = (i.toFloat() / openingSteps)
            delay(16)
        }
        eyeBlinkWeight = 0f
    }

    /**
     * Drives real-time speech visemes according to time-stamped cues.
     */
    fun playVisemes(cues: List<VisemeCue>, getAudioPositionMs: () -> Long) {
        animationJob?.cancel()
        animationJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val currentAudioTime = getAudioPositionMs()
                if (currentAudioTime < 0) break

                // Find active viseme cue for the current playback millisecond
                val activeCue = cues.firstOrNull { cue ->
                    currentAudioTime in cue.timestampMs until (cue.timestampMs + cue.durationMs)
                }

                // Reset all mouth visemes to zero target
                Viseme.values().forEach { v ->
                    targetWeights[v.blendshapeName] = 0f
                }

                // Apply active viseme
                if (activeCue != null) {
                    targetWeights[activeCue.viseme.blendshapeName] = activeCue.targetWeight
                } else {
                    targetWeights[Viseme.SIL.blendshapeName] = 1.0f
                }

                // Smooth interpolation (lerp) to prevent robotic mouth snapping
                val lerpFactor = 0.28f
                synchronized(currentWeights) {
                    targetWeights.forEach { (shape, target) ->
                        val current = currentWeights[shape] ?: 0f
                        val smoothed = current + (target - current) * lerpFactor
                        currentWeights[shape] = smoothed
                    }
                    onUpdateBlendshapes(HashMap(currentWeights))
                }

                delay(16) // 60 FPS interpolation
            }
        }
    }

    fun stop() {
        animationJob?.cancel()
        idleBehaviorJob?.cancel()
    }
}
