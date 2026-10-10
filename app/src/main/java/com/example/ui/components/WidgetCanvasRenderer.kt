package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.animation.ParticlePoint
import com.example.engine.script.SystemTelemetrySnapshot
import com.example.model.ElementType
import com.example.model.WidgetElement
import com.example.model.WidgetGridSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun parseComposeColor(hex: String, fallback: Color = Color(0xFF1E2638)): Color {
    val clean = hex.trim()
    if (clean.isEmpty() || clean.equals("transparent", ignoreCase = true) || clean == "#00000000") {
        return Color.Transparent
    }
    return runCatching {
        Color(android.graphics.Color.parseColor(clean))
    }.getOrDefault(fallback)
}

@Composable
fun InteractiveWidgetCanvas(
    gridSize: WidgetGridSize,
    backgroundColorHex: String,
    cornerRadius: Float,
    elements: List<WidgetElement>,
    selectedElementId: String?,
    telemetry: SystemTelemetrySnapshot,
    showGridAndGuides: Boolean = true,
    interactiveEditorMode: Boolean = true,
    particles: List<ParticlePoint> = emptyList(),
    onSelectElement: (String?) -> Unit = {},
    onDragElement: (String, Float, Float) -> Unit = { _, _, _ -> },
    onWidgetTapPreview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var canvasWidthPx by remember { mutableStateOf(1f) }
    var canvasHeightPx by remember { mutableStateOf(1f) }

    val baseBg = parseComposeColor(backgroundColorHex, Color(0xFF10131C))
    val endBg = Color(
        red = (baseBg.red * 1.18f).coerceAtMost(1f),
        green = (baseBg.green * 1.18f).coerceAtMost(1f),
        blue = (baseBg.blue * 1.24f).coerceAtMost(1f),
        alpha = 1f
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(gridSize.aspect)
            .clip(RoundedCornerShape(cornerRadius.coerceIn(8f, 44f).dp))
            .background(Brush.linearGradient(listOf(baseBg, endBg)))
            .border(
                width = 1.5.dp,
                color = Color(0xFF2B3752),
                shape = RoundedCornerShape(cornerRadius.coerceIn(8f, 44f).dp)
            )
            .testTag("widget_interactive_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .pointerInput(elements, selectedElementId, interactiveEditorMode) {
                    if (interactiveEditorMode) {
                        detectTapGestures { tapOffset ->
                            val sx = size.width / 300f
                            val sy = size.height / 300f
                            val vx = tapOffset.x / sx
                            val vy = tapOffset.y / sy
                            val hit = elements
                                .filter { it.visible }
                                .sortedByDescending { it.layerOrder }
                                .firstOrNull { el ->
                                    vx >= el.x && vx <= (el.x + el.width) &&
                                        vy >= el.y && vy <= (el.y + el.height)
                                }
                            onSelectElement(hit?.id)
                        }
                    } else {
                        detectTapGestures {
                            onWidgetTapPreview()
                        }
                    }
                }
                .pointerInput(selectedElementId, interactiveEditorMode) {
                    if (interactiveEditorMode) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val activeId = selectedElementId ?: return@detectDragGestures
                            val sx = (size.width / 300f).coerceAtLeast(0.1f)
                            val sy = (size.height / 300f).coerceAtLeast(0.1f)
                            onDragElement(activeId, dragAmount.x / sx, dragAmount.y / sy)
                        }
                    }
                }
        ) {
            canvasWidthPx = size.width
            canvasHeightPx = size.height
            val scaleX = size.width / 300f
            val scaleY = size.height / 300f
            val avgScale = (scaleX + scaleY) * 0.5f

            // 1. Optional IDE Grid & Center Alignment Guides
            if (showGridAndGuides && interactiveEditorMode) {
                val gridStep = 25f
                var gx = gridStep
                while (gx < 300f) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(gx * scaleX, 0f),
                        end = Offset(gx * scaleX, size.height),
                        strokeWidth = 1f
                    )
                    gx += gridStep
                }
                var gy = gridStep
                while (gy < 300f) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, gy * scaleY),
                        end = Offset(size.width, gy * scaleY),
                        strokeWidth = 1f
                    )
                    gy += gridStep
                }
                // Center crosshair guides
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                    start = Offset(150f * scaleX, 0f),
                    end = Offset(150f * scaleX, size.height),
                    strokeWidth = 1.2f,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                    start = Offset(0f, 150f * scaleY),
                    end = Offset(size.width, 150f * scaleY),
                    strokeWidth = 1.2f,
                    pathEffect = dashEffect
                )
            }

            // 2. Render Particle Presets
            for (pt in particles) {
                val pCol = parseComposeColor(pt.colorHex, Color(0xFF00E5FF))
                drawCircle(
                    color = pCol.copy(alpha = pt.alpha),
                    radius = pt.radius * avgScale,
                    center = Offset(pt.x * scaleX, pt.y * scaleY)
                )
            }

            // 3. Render Widget Elements in layer order
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }

            for (el in elements.filter { it.visible }.sortedBy { it.layerOrder }) {
                val left = el.x * scaleX
                val top = el.y * scaleY
                val w = el.width * scaleX
                val h = el.height * scaleY
                val cx = left + w / 2f
                val cy = top + h / 2f
                val radiusPx = el.cornerRadius * avgScale

                withTransform({
                    if (el.rotation != 0f) {
                        rotate(degrees = el.rotation, pivot = Offset(cx, cy))
                    }
                }) {
                    val bgCol = parseComposeColor(el.backgroundColor, Color.Transparent)
                    val txtCol = parseComposeColor(el.textColor, Color.White)
                    val brdCol = parseComposeColor(el.borderColor, Color(0xFF00E5FF))

                    if (bgCol.alpha > 0f && el.type != ElementType.PROGRESS_BAR && el.type != ElementType.DIVIDER_LINE) {
                        drawRoundRect(
                            color = bgCol.copy(alpha = (bgCol.alpha * el.opacity).coerceIn(0f, 1f)),
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(radiusPx, radiusPx)
                        )
                    }

                    if (el.borderWidth > 0f) {
                        drawRoundRect(
                            color = brdCol.copy(alpha = el.opacity.coerceIn(0f, 1f)),
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(radiusPx, radiusPx),
                            style = Stroke(width = el.borderWidth * avgScale)
                        )
                    }

                    when (el.type) {
                        ElementType.PROGRESS_BAR -> {
                            val trackColor = if (bgCol.alpha > 0f) bgCol else Color(0xFF232D42)
                            drawRoundRect(
                                color = trackColor.copy(alpha = el.opacity),
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                cornerRadius = CornerRadius(radiusPx, radiusPx)
                            )
                            val fillW = (w * el.progressValue.coerceIn(0.04f, 1f))
                            drawRoundRect(
                                color = txtCol.copy(alpha = el.opacity),
                                topLeft = Offset(left, top),
                                size = Size(fillW, h),
                                cornerRadius = CornerRadius(radiusPx, radiusPx)
                            )
                        }
                        ElementType.DIVIDER_LINE -> {
                            drawLine(
                                color = txtCol.copy(alpha = el.opacity),
                                start = Offset(left, cy),
                                end = Offset(left + w, cy),
                                strokeWidth = h.coerceAtLeast(2f)
                            )
                        }
                        ElementType.CUSTOM_GRAPHIC -> {
                            // Draw concentric vector rings & cyber node
                            drawCircle(
                                color = brdCol.copy(alpha = el.opacity * 0.7f),
                                radius = (minOf(w, h) * 0.42f),
                                center = Offset(cx, cy),
                                style = Stroke(width = 3f)
                            )
                            drawCircle(
                                color = txtCol.copy(alpha = el.opacity),
                                radius = (minOf(w, h) * 0.18f),
                                center = Offset(cx, cy)
                            )
                        }
                        else -> {
                            val resolvedText = when (el.type) {
                                ElementType.CLOCK -> if (el.textValue.isNotBlank() && !el.textValue.contains(":")) el.textValue
                                else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(telemetry.currentTimeMillis))
                                ElementType.DATE_CALENDAR -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(telemetry.currentTimeMillis))
                                ElementType.BATTERY -> if (el.textValue.isNotBlank()) el.textValue else "Battery: ${telemetry.batteryLevel}%"
                                else -> el.textValue
                            }

                            if (resolvedText.isNotBlank()) {
                                textPaint.color = android.graphics.Color.argb(
                                    (el.opacity.coerceIn(0f, 1f) * 255).toInt(),
                                    (txtCol.red * 255).toInt(),
                                    (txtCol.green * 255).toInt(),
                                    (txtCol.blue * 255).toInt()
                                )
                                textPaint.textSize = (el.fontSize * avgScale).coerceAtLeast(10f)
                                val centered = el.type == ElementType.BUTTON ||
                                    el.type == ElementType.GAME_OBJECT ||
                                    el.type == ElementType.INTERACTIVE_ZONE
                                textPaint.textAlign = if (centered) {
                                    android.graphics.Paint.Align.CENTER
                                } else {
                                    android.graphics.Paint.Align.LEFT
                                }
                                val baselineY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
                                val drawX = if (centered) cx else left + 8f * scaleX
                                drawContext.canvas.nativeCanvas.drawText(resolvedText, drawX, baselineY, textPaint)
                            }
                        }
                    }

                    // Selection bounding box & corner handles in Editor Mode
                    if (interactiveEditorMode && el.id == selectedElementId) {
                        val selColor = if (el.locked) Color(0xFFFFAB00) else Color(0xFF00E5FF)
                        drawRoundRect(
                            color = selColor,
                            topLeft = Offset(left - 2f, top - 2f),
                            size = Size(w + 4f, h + 4f),
                            cornerRadius = CornerRadius(radiusPx + 2f, radiusPx + 2f),
                            style = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                        )
                        // Corner resize/anchor dots
                        val handleRadius = 5f
                        listOf(
                            Offset(left, top),
                            Offset(left + w, top),
                            Offset(left, top + h),
                            Offset(left + w, top + h)
                        ).forEach { corner ->
                            drawCircle(color = selColor, radius = handleRadius, center = corner)
                        }
                    }
                }
            }
        }
    }
}
