package com.example.widget

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.BatteryManager
import com.example.engine.script.SystemTelemetrySnapshot
import com.example.model.ElementType
import com.example.model.WidgetElement
import com.example.model.WidgetGridSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetBitmapRenderer {

    fun readDeviceTelemetry(context: Context): SystemTelemetrySnapshot {
        val batteryStatus: Intent? = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 86

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val freeMemMb = runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            (memInfo.availMem / (1024 * 1024)).toInt().coerceAtLeast(512)
        }.getOrDefault(3420)

        return SystemTelemetrySnapshot(
            batteryLevel = batteryPct.coerceIn(1, 100),
            isCharging = isCharging,
            freeMemoryMb = freeMemMb,
            cpuLoadPercent = 22,
            weatherTempC = 21,
            weatherCondition = "Ясно",
            stepCount = 6420,
            nextCalendarEvent = "14:00 Архітектурне рев'ю",
            currentTimeMillis = System.currentTimeMillis()
        )
    }

    fun renderWidgetToBitmap(
        gridSize: WidgetGridSize,
        backgroundColorHex: String,
        cornerRadius: Float,
        elements: List<WidgetElement>,
        telemetry: SystemTelemetrySnapshot
    ): Bitmap {
        val bmpWidth = when (gridSize) {
            WidgetGridSize.SIZE_4X2 -> 720
            WidgetGridSize.SIZE_4X4 -> 720
            WidgetGridSize.SIZE_2X2 -> 480
        }
        val bmpHeight = when (gridSize) {
            WidgetGridSize.SIZE_4X2 -> 360
            WidgetGridSize.SIZE_4X4 -> 720
            WidgetGridSize.SIZE_2X2 -> 480
        }

        val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val scaleX = bmpWidth / 300f
        val scaleY = bmpHeight / 300f
        val avgScale = (scaleX + scaleY) * 0.5f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            val baseColor = parseColorSafe(backgroundColorHex, Color.parseColor("#10131C"))
            shader = LinearGradient(
                0f, 0f, bmpWidth.toFloat(), bmpHeight.toFloat(),
                baseColor,
                 adjustBrightness(baseColor, 1.18f),
                Shader.TileMode.CLAMP
            )
        }

        val rootRect = RectF(4f, 4f, bmpWidth - 4f, bmpHeight - 4f)
        val rootRadius = cornerRadius * avgScale
        canvas.drawRoundRect(rootRect, rootRadius, rootRadius, bgPaint)

        // Subtle outer border
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.parseColor("#2B3752")
        }
        canvas.drawRoundRect(rootRect, rootRadius, rootRadius, framePaint)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        for (el in elements.filter { it.visible }.sortedBy { it.layerOrder }) {
            canvas.save()
            val left = el.x * scaleX
            val top = el.y * scaleY
            val right = (el.x + el.width) * scaleX
            val bottom = (el.y + el.height) * scaleY
            val rect = RectF(left, top, right, bottom)
            val cx = rect.centerX()
            val cy = rect.centerY()

            if (el.rotation != 0f) {
                canvas.rotate(el.rotation, cx, cy)
            }

            val alphaInt = (el.opacity.coerceIn(0f, 1f) * 255).toInt()
            val bgCol = parseColorSafe(el.backgroundColor, Color.TRANSPARENT)
            val txtCol = parseColorSafe(el.textColor, Color.WHITE)
            val brdCol = parseColorSafe(el.borderColor, Color.parseColor("#00E5FF"))
            val elRadius = el.cornerRadius * avgScale

            // Draw background for container/shape/button/game_obj
            if (Color.alpha(bgCol) > 0 && el.type != ElementType.PROGRESS_BAR && el.type != ElementType.DIVIDER_LINE) {
                fillPaint.color = bgCol
                fillPaint.alpha = (Color.alpha(bgCol) * el.opacity).toInt().coerceIn(0, 255)
                canvas.drawRoundRect(rect, elRadius, elRadius, fillPaint)
            }

            if (el.borderWidth > 0f) {
                strokePaint.color = brdCol
                strokePaint.alpha = alphaInt
                strokePaint.strokeWidth = el.borderWidth * avgScale
                canvas.drawRoundRect(rect, elRadius, elRadius, strokePaint)
            }

            when (el.type) {
                ElementType.PROGRESS_BAR -> {
                    fillPaint.color = if (Color.alpha(bgCol) > 0) bgCol else Color.parseColor("#232D42")
                    fillPaint.alpha = alphaInt
                    canvas.drawRoundRect(rect, elRadius, elRadius, fillPaint)

                    val progRect = RectF(
                        rect.left,
                        rect.top,
                        rect.left + rect.width() * el.progressValue.coerceIn(0.04f, 1f),
                        rect.bottom
                    )
                    fillPaint.color = txtCol
                    fillPaint.alpha = alphaInt
                    canvas.drawRoundRect(progRect, elRadius, elRadius, fillPaint)
                }
                ElementType.DIVIDER_LINE -> {
                    strokePaint.color = txtCol
                    strokePaint.alpha = alphaInt
                    strokePaint.strokeWidth = (el.height * scaleY).coerceAtLeast(2f)
                    canvas.drawLine(rect.left, cy, rect.right, cy, strokePaint)
                }
                else -> {
                    val resolvedText = when (el.type) {
                        ElementType.CLOCK -> if (el.textValue.isNotBlank() && !el.textValue.contains(":")) el.textValue else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(telemetry.currentTimeMillis))
                        ElementType.DATE_CALENDAR -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(telemetry.currentTimeMillis))
                        ElementType.BATTERY -> if (el.textValue.isNotBlank()) el.textValue else "Battery: ${telemetry.batteryLevel}%"
                        else -> el.textValue
                    }
                    if (resolvedText.isNotBlank()) {
                        textPaint.color = txtCol
                        textPaint.alpha = alphaInt
                        textPaint.textSize = el.fontSize * avgScale
                        if (el.type == ElementType.BUTTON || el.type == ElementType.GAME_OBJECT) {
                            textPaint.textAlign = Paint.Align.CENTER
                            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
                            canvas.drawText(resolvedText, cx, textY, textPaint)
                        } else {
                            textPaint.textAlign = Paint.Align.LEFT
                            val textY = rect.top + (rect.height() / 2f) - (textPaint.descent() + textPaint.ascent()) / 2f
                            canvas.drawText(resolvedText, rect.left + 8f * scaleX, textY, textPaint)
                        }
                    }
                }
            }
            canvas.restore()
        }

        return bitmap
    }

    private fun parseColorSafe(hex: String, fallback: Int): Int {
        val clean = hex.trim()
        if (clean.isEmpty() || clean == "transparent") return Color.TRANSPARENT
        return runCatching { Color.parseColor(clean) }.getOrDefault(fallback)
    }

    private fun adjustBrightness(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
}
