package com.example

import com.example.data.BuiltInCatalog
import com.example.engine.animation.AnimationEngine
import com.example.engine.game.MiniGameEngine
import com.example.engine.script.SystemTelemetrySnapshot
import com.example.engine.script.WidgetScriptEngine
import com.example.model.AnimProperty
import com.example.model.AnimationTrack
import com.example.model.DiagnosticSeverity
import com.example.model.EasingCurve
import com.example.model.ElementType
import com.example.model.GameTemplateType
import com.example.model.Keyframe
import com.example.model.MiniGameConfig
import com.example.model.StudioJsonSerializer
import com.example.model.WidgetElement
import com.example.model.WidgetGridSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun `WidgetScript parser evaluates declarative components and system expressions`() {
        val script = """
            widget "System Dashboard" {
                size: 2x2
                background: "#10131C"
                cornerRadius: 24

                text "clock" {
                    position: center
                    value: system.time("HH:mm")
                    fontSize: 42
                    color: "#FFFFFF"
                }

                text "battery" {
                    position: bottomLeft
                    value: "Battery: " + system.battery.level + "%"
                }

                on tap {
                    animation.play("pulse")
                }
            }
        """.trimIndent()

        val engine = WidgetScriptEngine()
        val telemetry = SystemTelemetrySnapshot(batteryLevel = 92)
        val result = engine.execute(script = script, telemetry = telemetry, triggerTapEvent = true)

        assertFalse(result.haltedBySandbox)
        assertTrue(result.diagnostics.isEmpty())
        assertEquals("System Dashboard", result.widgetTitle)
        assertEquals(WidgetGridSize.SIZE_2X2, result.gridSize)
        assertEquals(2, result.elements.size)
        assertEquals("Battery: 92%", result.elements[1].textValue)
        assertTrue(result.triggeredAnimations.contains("pulse"))
    }

    @Test
    fun `WidgetScript sandbox blocks forbidden shell and filesystem tokens`() {
        val maliciousScript = """
            widget "Hack" {
                var x = Runtime.getRuntime().exec("sh -c ls")
            }
        """.trimIndent()

        val engine = WidgetScriptEngine()
        val result = engine.execute(maliciousScript)

        assertTrue(result.haltedBySandbox)
        assertTrue(result.diagnostics.any { it.severity == DiagnosticSeverity.ERROR && it.message.contains("Sandbox") })
    }

    @Test
    fun `WidgetScript sandbox halts infinite or excessive loops`() {
        val heavyLoopScript = """
            widget "LoopTest" {
                var sum = 0
                for (i in 0..5000) {
                    sum = sum + i
                }
            }
        """.trimIndent()

        val engine = WidgetScriptEngine(maxInstructions = 250)
        val result = engine.execute(heavyLoopScript)

        assertTrue(result.haltedBySandbox)
        assertTrue(result.diagnostics.any { it.severity == DiagnosticSeverity.ERROR })
    }

    @Test
    fun `WidgetScript reports syntax errors with line numbers and const immutability`() {
        val brokenScript = """
            widget "Broken" {
                const LIMIT = 10
                LIMIT = 20
                text "unclosed {
                    value: "hello
                }
        """.trimIndent()

        val engine = WidgetScriptEngine()
        val result = engine.execute(brokenScript)

        assertTrue(result.diagnostics.any { it.line == 3 && it.message.contains("константу") })
        assertTrue(result.diagnostics.any { it.message.contains("Незакритий рядковий літерал") })
    }

    @Test
    fun `Bidirectional code generator and serializer preserve widget elements`() {
        val elements = listOf(
            WidgetElement(
                id = "clock_1",
                name = "clock_1",
                type = ElementType.CLOCK,
                x = 40f,
                y = 50f,
                width = 180f,
                height = 60f,
                fontSize = 38f,
                textColor = "#00E5FF"
            )
        )
        val json = StudioJsonSerializer.elementsToJson(elements)
        val restored = StudioJsonSerializer.elementsFromJson(json)
        assertEquals(1, restored.size)
        assertEquals("clock_1", restored.first().name)
        assertEquals(38f, restored.first().fontSize, 0.01f)

        val engine = WidgetScriptEngine()
        val generatedScript = engine.generateScriptFromVisualState(
            title = "My Clock",
            gridSize = WidgetGridSize.SIZE_4X2,
            backgroundColor = "#10131C",
            cornerRadius = 20f,
            elements = elements
        )
        val recompiled = engine.execute(generatedScript)
        assertEquals("My Clock", recompiled.widgetTitle)
        assertEquals(WidgetGridSize.SIZE_4X2, recompiled.gridSize)
        assertEquals(1, recompiled.elements.size)
    }

    @Test
    fun `AnimationEngine interpolates keyframes and respects low power particle mode`() {
        val track = AnimationTrack(
            id = "t1",
            name = "scale_track",
            targetElementId = "clock",
            property = AnimProperty.SCALE,
            durationMs = 1000,
            loop = false,
            keyframes = listOf(
                Keyframe(0, 0f, EasingCurve.LINEAR),
                Keyframe(1000, 10f, EasingCurve.LINEAR)
            )
        )
        val midVal = AnimationEngine.evaluateTrackAt(track, 500L)
        assertEquals(5f, midVal, 0.05f)

        val fullParticles = AnimationEngine.generateParticles(
            com.example.model.ParticlePreset.CYBER_SPARKS,
            timeMs = 400L,
            lowPowerMode = false
        )
        val ecoParticles = AnimationEngine.generateParticles(
            com.example.model.ParticlePreset.CYBER_SPARKS,
            timeMs = 400L,
            lowPowerMode = true
        )
        assertTrue(fullParticles.size > ecoParticles.size)
    }

    @Test
    fun `Plugin manifest validator accepts valid manifest and rejects dangerous Android permissions`() {
        val validManifest = """
            {
              "id": "plugin.test.analytics",
              "name": "Safe Metrics",
              "version": "1.0.0",
              "apiVersion": "1.0",
              "namespace": "metrics_ext",
              "permissions": ["SYSTEM_METRICS"]
            }
        """.trimIndent()
        val okRes = BuiltInCatalog.validatePluginManifestJson(validManifest, setOf("system", "ui"))
        assertTrue(okRes.isValid)

        val dangerousManifest = """
            {
              "id": "plugin.spy.sms",
              "name": "SMS Reader",
              "version": "1.0.0",
              "apiVersion": "1.0",
              "namespace": "sms_ext",
              "permissions": ["READ_SMS"]
            }
        """.trimIndent()
        val blockedRes = BuiltInCatalog.validatePluginManifestJson(dangerousManifest, setOf("system", "ui"))
        assertFalse(blockedRes.isValid)
        assertTrue(blockedRes.errorReason.contains("READ_SMS"))
    }

    @Test
    fun `MiniGameEngine advances frame state and tracks score`() {
        val engine = MiniGameEngine()
        val cfg = MiniGameConfig(templateType = GameTemplateType.ASTRO_DODGER, targetWinScore = 100)
        val initial = engine.createInitialState(cfg).copy(isRunning = true)
        val stepped = engine.step(initial, cfg, dtMs = 32L)
        assertEquals(32L, stepped.elapsedMs)
        assertTrue(stepped.entities.isNotEmpty())
    }
}
