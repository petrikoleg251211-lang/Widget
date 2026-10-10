package com.example.engine.animation

import com.example.model.AnimProperty
import com.example.model.AnimationTrack
import com.example.model.EasingCurve
import com.example.model.Keyframe
import com.example.model.ParticlePreset
import com.example.model.TriggerType
import com.example.model.WidgetElement
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

data class ParticlePoint(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val colorHex: String
)

object AnimationEngine {

    fun interpolateEasing(tRaw: Float, curve: EasingCurve): Float {
        val t = tRaw.coerceIn(0f, 1f)
        return when (curve) {
            EasingCurve.LINEAR -> t
            EasingCurve.EASE_IN -> t * t * t
            EasingCurve.EASE_OUT -> 1f - (1f - t).pow(3)
            EasingCurve.EASE_IN_OUT -> if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
            EasingCurve.SPRING -> {
                val c4 = (2f * PI.toFloat()) / 3f
                if (t == 0f || t == 1f) t
                else 2f.pow(-8f * t) * sin((t * 8f - 0.75f) * c4) + 1f
            }
            EasingCurve.BOUNCE -> {
                val n1 = 7.5625f
                val d1 = 2.75f
                when {
                    t < 1f / d1 -> n1 * t * t
                    t < 2f / d1 -> {
                        val t2 = t - 1.5f / d1
                        n1 * t2 * t2 + 0.75f
                    }
                    t < 2.5f / d1 -> {
                        val t2 = t - 2.25f / d1
                        n1 * t2 * t2 + 0.9375f
                    }
                    else -> {
                        val t2 = t - 2.625f / d1
                        n1 * t2 * t2 + 0.984375f
                    }
                }
            }
        }
    }

    fun evaluateTrackAt(track: AnimationTrack, rawTimeMs: Long): Float {
        val kfs = track.keyframes.sortedBy { it.timeMs }
        if (kfs.isEmpty()) return 0f
        if (kfs.size == 1) return kfs.first().value

        val duration = track.durationMs.coerceAtLeast(100)
        val effectiveTime: Int = if (!track.loop) {
            rawTimeMs.coerceAtMost(duration.toLong()).toInt()
        } else {
            val cycle = (rawTimeMs / duration).toInt()
            val rem = (rawTimeMs % duration).toInt()
            if (track.reverseOnRepeat && cycle % 2 == 1) {
                duration - rem
            } else {
                rem
            }
        }

        if (effectiveTime <= kfs.first().timeMs) return kfs.first().value
        if (effectiveTime >= kfs.last().timeMs) return kfs.last().value

        for (i in 0 until kfs.size - 1) {
            val startKf = kfs[i]
            val endKf = kfs[i + 1]
            if (effectiveTime in startKf.timeMs..endKf.timeMs) {
                val span = (endKf.timeMs - startKf.timeMs).coerceAtLeast(1)
                val localT = (effectiveTime - startKf.timeMs).toFloat() / span.toFloat()
                val easedT = interpolateEasing(localT, endKf.easing)
                return startKf.value + (endKf.value - startKf.value) * easedT
            }
        }
        return kfs.last().value
    }

    fun applyAnimationsToElements(
        elements: List<WidgetElement>,
        tracks: List<AnimationTrack>,
        timeMs: Long
    ): List<WidgetElement> {
        if (tracks.isEmpty()) return elements
        return elements.map { element ->
            var updated = element
            val matchingTracks = tracks.filter {
                it.targetElementId.equals(element.id, ignoreCase = true) ||
                    it.targetElementId.equals(element.name, ignoreCase = true)
            }
            for (track in matchingTracks) {
                val delta = evaluateTrackAt(track, timeMs)
                updated = when (track.property) {
                    AnimProperty.X -> updated.copy(x = updated.x + delta)
                    AnimProperty.Y -> updated.copy(y = updated.y + delta)
                    AnimProperty.SCALE -> {
                        val factor = (1f + delta * 0.25f).coerceIn(0.4f, 2.2f)
                        updated.copy(
                            width = updated.width * factor,
                            height = updated.height * factor
                        )
                    }
                    AnimProperty.ROTATION -> updated.copy(rotation = updated.rotation + delta)
                    AnimProperty.OPACITY -> updated.copy(opacity = delta.coerceIn(0.1f, 1f))
                    AnimProperty.CORNER_RADIUS -> updated.copy(cornerRadius = delta.coerceIn(0f, 80f))
                    AnimProperty.GLOW_BLUR -> updated.copy(
                        borderWidth = (delta * 0.2f).coerceIn(0f, 8f)
                    )
                }
            }
            updated
        }
    }

    fun generateParticles(
        preset: ParticlePreset,
        timeMs: Long,
        lowPowerMode: Boolean = false
    ): List<ParticlePoint> {
        if (preset == ParticlePreset.NONE) return emptyList()
        val baseCount = if (lowPowerMode) 6 else 18
        val tSec = timeMs / 1000f
        val list = mutableListOf<ParticlePoint>()

        for (i in 0 until baseCount) {
            val seed = i * 1.618f
            when (preset) {
                ParticlePreset.CYBER_SPARKS -> {
                    val angle = seed * 2.4f + tSec * 1.5f
                    val dist = ((tSec * 60f + i * 17f) % 130f)
                    val px = 150f + cos(angle) * dist
                    val py = 150f + sin(angle) * dist
                    val alpha = (1f - dist / 135f).coerceIn(0.15f, 0.95f)
                    list.add(
                        ParticlePoint(
                            x = px,
                            y = py,
                            radius = if (i % 2 == 0) 3.5f else 2.2f,
                            alpha = alpha,
                            colorHex = if (i % 2 == 0) "#00E5FF" else "#8C62FF"
                        )
                    )
                }
                ParticlePreset.NEBULA_ORBS -> {
                    val px = 40f + ((i * 37f + sin(tSec + seed) * 28f) % 220f)
                    val py = 40f + ((i * 29f + cos(tSec * 0.8f + seed) * 24f) % 220f)
                    list.add(
                        ParticlePoint(
                            x = px,
                            y = py,
                            radius = 8f + (i % 4) * 3f,
                            alpha = 0.28f,
                            colorHex = if (i % 3 == 0) "#8C62FF" else "#00E5FF"
                        )
                    )
                }
                ParticlePreset.PULSE_RINGS -> {
                    val phase = ((tSec * 0.8f + i * 0.2f) % 1f)
                    val angle = (i.toFloat() / baseCount) * 2f * PI.toFloat()
                    val r = 25f + phase * 110f
                    list.add(
                        ParticlePoint(
                            x = 150f + cos(angle) * r,
                            y = 150f + sin(angle) * r,
                            radius = 3f,
                            alpha = (1f - phase).coerceIn(0.1f, 0.9f),
                            colorHex = "#00E676"
                        )
                    )
                }
                ParticlePreset.MATRIX_FLOW -> {
                    val colX = 20f + (i * (260f / baseCount))
                    val fallY = (tSec * (70f + (i % 5) * 20f) + i * 35f) % 280f
                    list.add(
                        ParticlePoint(
                            x = colX,
                            y = fallY,
                            radius = 2.8f,
                            alpha = 0.75f,
                            colorHex = "#00E676"
                        )
                    )
                }
                ParticlePreset.NONE -> Unit
            }
        }
        return list
    }

    fun getBuiltInPresets(defaultTargetId: String = "clock"): List<AnimationTrack> = listOf(
        AnimationTrack(
            id = "preset_pulse",
            name = "pulse",
            targetElementId = defaultTargetId,
            property = AnimProperty.SCALE,
            durationMs = 1200,
            loop = true,
            reverseOnRepeat = true,
            trigger = TriggerType.ON_LOAD,
            particlePreset = ParticlePreset.CYBER_SPARKS,
            keyframes = listOf(
                Keyframe(0, 0f, EasingCurve.EASE_IN_OUT),
                Keyframe(600, 0.35f, EasingCurve.SPRING),
                Keyframe(1200, 0f, EasingCurve.EASE_IN_OUT)
            )
        ),
        AnimationTrack(
            id = "preset_float_y",
            name = "levitate",
            targetElementId = defaultTargetId,
            property = AnimProperty.Y,
            durationMs = 1800,
            loop = true,
            reverseOnRepeat = true,
            trigger = TriggerType.ON_LOAD,
            particlePreset = ParticlePreset.NEBULA_ORBS,
            keyframes = listOf(
                Keyframe(0, -8f, EasingCurve.EASE_IN_OUT),
                Keyframe(900, 8f, EasingCurve.EASE_IN_OUT),
                Keyframe(1800, -8f, EasingCurve.EASE_IN_OUT)
            )
        ),
        AnimationTrack(
            id = "preset_spin",
            name = "radar_sweep",
            targetElementId = defaultTargetId,
            property = AnimProperty.ROTATION,
            durationMs = 2400,
            loop = true,
            reverseOnRepeat = false,
            trigger = TriggerType.ON_TAP,
            particlePreset = ParticlePreset.PULSE_RINGS,
            keyframes = listOf(
                Keyframe(0, 0f, EasingCurve.LINEAR),
                Keyframe(1200, 180f, EasingCurve.LINEAR),
                Keyframe(2400, 360f, EasingCurve.LINEAR)
            )
        ),
        AnimationTrack(
            id = "preset_bounce",
            name = "bounce_drop",
            targetElementId = defaultTargetId,
            property = AnimProperty.Y,
            durationMs = 1000,
            loop = true,
            reverseOnRepeat = true,
            trigger = TriggerType.ON_TAP,
            particlePreset = ParticlePreset.MATRIX_FLOW,
            keyframes = listOf(
                Keyframe(0, -24f, EasingCurve.EASE_IN),
                Keyframe(1000, 0f, EasingCurve.BOUNCE)
            )
        )
    )
}
