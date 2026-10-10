package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.StudioSettings
import com.example.engine.animation.AnimationEngine
import com.example.engine.game.EntityKind
import com.example.model.AnimProperty
import com.example.model.AnimationTrack
import com.example.model.EasingCurve
import com.example.model.GameTemplateType
import com.example.model.MiniGameConfig
import com.example.model.ParticlePreset
import com.example.model.TriggerType
import com.example.ui.components.InteractiveWidgetCanvas
import com.example.ui.components.parseComposeColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.EditorWorkspaceState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimationStudioScreen(
    workspace: EditorWorkspaceState,
    settings: StudioSettings,
    onSelectTrack: (String) -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekTimeline: (Long) -> Unit,
    onApplyPreset: (AnimationTrack) -> Unit,
    onUpdateSelectedTrack: ((AnimationTrack) -> AnimationTrack) -> Unit,
    onAddKeyframe: (Int, Float, EasingCurve) -> Unit,
    onDeleteTrack: (String) -> Unit
) {
    val selectedTrack = workspace.animations.firstOrNull { it.id == workspace.selectedTrackId }
        ?: workspace.animations.firstOrNull()
    val durationMs = selectedTrack?.durationMs ?: 1200

    LaunchedEffect(workspace.isAnimationPlaying, durationMs, settings.lowPowerAnimationMode) {
        val frameDelay = if (settings.lowPowerAnimationMode) 64L else 24L
        while (workspace.isAnimationPlaying) {
            delay(frameDelay)
            val next = (workspace.timelinePositionMs + frameDelay) % (durationMs * 2L).coerceAtLeast(200L)
            onSeekTimeline(next)
        }
    }

    val animatedElements = remember(workspace.elements, workspace.animations, workspace.timelinePositionMs) {
        AnimationEngine.applyAnimationsToElements(
            elements = workspace.elements,
            tracks = workspace.animations,
            timeMs = workspace.timelinePositionMs
        )
    }

    val activeParticlePreset = selectedTrack?.particlePreset ?: ParticlePreset.NONE
    val particles = remember(activeParticlePreset, workspace.timelinePositionMs, settings.lowPowerAnimationMode) {
        AnimationEngine.generateParticles(
            preset = activeParticlePreset,
            timeMs = workspace.timelinePositionMs,
            lowPowerMode = settings.lowPowerAnimationMode
        )
    }

    val presets = remember(workspace.selectedElementId) {
        AnimationEngine.getBuiltInPresets(workspace.selectedElementId ?: "clock")
    }

    var newKfTime by remember { mutableIntStateOf(600) }
    var newKfValue by remember { mutableFloatStateOf(0.4f) }
    var newKfEasing by remember { mutableStateOf(EasingCurve.SPRING) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("animation_studio_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Анімаційна студія і Таймлайн", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = if (settings.lowPowerAnimationMode) "Активно енергоощадний режим (~15 FPS)"
                else "Плавний режим попереднього перегляду (~40 FPS)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Live Animated Preview + Scrubber
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InteractiveWidgetCanvas(
                        gridSize = workspace.gridSize,
                        backgroundColorHex = workspace.backgroundColor,
                        cornerRadius = workspace.cornerRadius,
                        elements = animatedElements,
                        selectedElementId = selectedTrack?.targetElementId,
                        telemetry = workspace.telemetry,
                        showGridAndGuides = false,
                        interactiveEditorMode = false,
                        particles = particles,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timeline Scrubber Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePlayback,
                            modifier = Modifier.testTag("anim_play_pause_button")
                        ) {
                            Icon(
                                if (workspace.isAnimationPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Відтворення таймлайну"
                            )
                        }
                        Text(
                            text = "${workspace.timelinePositionMs % durationMs.coerceAtLeast(1)} / ${durationMs}мс",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = (workspace.timelinePositionMs % durationMs.coerceAtLeast(1)).toFloat(),
                            onValueChange = { onSeekTimeline(it.toLong()) },
                            valueRange = 0f..durationMs.coerceAtLeast(100).toFloat(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Animation Presets Strip
        item {
            Text("Готові пресети анімацій:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilledTonalButton(
                        onClick = { onApplyPreset(preset) },
                        modifier = Modifier.testTag("apply_preset_${preset.name}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${preset.name} (${preset.property.labelUk})")
                    }
                }
            }
        }

        // Active Tracks List
        item {
            Text("Доріжки анімації у проєкті (${workspace.animations.size}):", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                workspace.animations.forEach { track ->
                    FilterChip(
                        selected = track.id == selectedTrack?.id,
                        onClick = { onSelectTrack(track.id) },
                        label = { Text("${track.name} -> ${track.targetElementId}") }
                    )
                }
            }
        }

        if (selectedTrack != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Доріжка '${selectedTrack.name}' (Ціль: ${selectedTrack.targetElementId})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onDeleteTrack(selectedTrack.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Видалити доріжку",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Target Element Selector
                        Text("Цільовий елемент віджета:", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            workspace.elements.forEach { el ->
                                FilterChip(
                                    selected = selectedTrack.targetElementId == el.id,
                                    onClick = { onUpdateSelectedTrack { it.copy(targetElementId = el.id) } },
                                    label = { Text(el.name) }
                                )
                            }
                        }

                        // Animated Property Selector
                        Text("Властивість анімації:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimProperty.entries.forEach { prop ->
                                FilterChip(
                                    selected = selectedTrack.property == prop,
                                    onClick = { onUpdateSelectedTrack { it.copy(property = prop) } },
                                    label = { Text(prop.labelUk) }
                                )
                            }
                        }

                        // Trigger & Particle Emitter
                        Text("Тригер запуску:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TriggerType.entries.forEach { trig ->
                                FilterChip(
                                    selected = selectedTrack.trigger == trig,
                                    onClick = { onUpdateSelectedTrack { it.copy(trigger = trig) } },
                                    label = { Text(trig.labelUk) }
                                )
                            }
                        }

                        Text("Система частинок (Particle Engine):", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ParticlePreset.entries.forEach { part ->
                                FilterChip(
                                    selected = selectedTrack.particlePreset == part,
                                    onClick = { onUpdateSelectedTrack { it.copy(particlePreset = part) } },
                                    label = { Text(part.labelUk) }
                                )
                            }
                        }

                        // Keyframes List & Add Keyframe
                        Text("Ключові кадри (Keyframes):", style = MaterialTheme.typography.labelLarge)
                        selectedTrack.keyframes.forEach { kf ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "t = ${kf.timeMs} мс",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "значення = ${kf.value}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = kf.easing.labelUk,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Додати / оновити ключовий кадр:", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "Час: ${newKfTime}мс • Значення: ${(newKfValue * 100).roundToInt() / 100f}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = newKfTime.toFloat(),
                            onValueChange = { newKfTime = it.roundToInt() },
                            valueRange = 0f..2400f
                        )
                        Slider(
                            value = newKfValue,
                            onValueChange = { newKfValue = it },
                            valueRange = -40f..40f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EasingCurve.entries.forEach { curve ->
                                FilterChip(
                                    selected = newKfEasing == curve,
                                    onClick = { newKfEasing = curve },
                                    label = { Text(curve.labelUk) }
                                )
                            }
                        }
                        Button(
                            onClick = { onAddKeyframe(newKfTime, newKfValue, newKfEasing) },
                            modifier = Modifier.fillMaxWidth().testTag("add_keyframe_button")
                        ) {
                            Text("Зберегти ключовий кадр")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun MiniGameStudioScreen(
    workspace: EditorWorkspaceState,
    onSelectTemplate: (GameTemplateType) -> Unit,
    onUpdateConfig: ((MiniGameConfig) -> MiniGameConfig) -> Unit,
    onStartOrReset: () -> Unit,
    onTogglePause: () -> Unit,
    onStepFrame: (Long) -> Unit,
    onPointerInput: (Float, Float, Boolean) -> Unit,
    onSyncScoreToWidget: () -> Unit
) {
    val gameRuntime = workspace.gameRuntime
    val gameConfig = workspace.gameConfig

    LaunchedEffect(gameRuntime.isRunning) {
        while (gameRuntime.isRunning) {
            delay(20L)
            onStepFrame(20L)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("game_studio_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Студія мініігор 2D", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Інтерактивний ігровий рушій із фізикою, колізіями та синхронізацією рекордів у віджет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Template Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameTemplateType.entries.forEach { tpl ->
                    FilterChip(
                        selected = gameConfig.templateType == tpl,
                        onClick = { onSelectTemplate(tpl) },
                        label = { Text(tpl.labelUk) },
                        modifier = Modifier.testTag("game_template_${tpl.name.lowercase()}")
                    )
                }
            }
        }

        // Score & Telemetry HUD + Interactive 2D Arena
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "РАХУНОК: ${gameRuntime.score}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "РЕКОРД: ${gameConfig.highScore}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "РІВЕНЬ ${gameRuntime.level} • ❤️ ${gameRuntime.lives}",
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = gameRuntime.statusBanner,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (gameRuntime.isGameOver) MaterialTheme.colorScheme.error
                        else if (gameRuntime.isVictory) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2D Game Canvas Arena (300x300 virtual space)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 310.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF090D16))
                            .border(2.dp, Color(0xFF2B3752), RoundedCornerShape(20.dp))
                            .testTag("mini_game_arena_canvas")
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(gameConfig.templateType) {
                                    detectTapGestures { offset ->
                                        val sx = size.width / 300f
                                        val sy = size.height / 300f
                                        onPointerInput(offset.x / sx, offset.y / sy, true)
                                    }
                                }
                                .pointerInput(gameConfig.templateType) {
                                    detectDragGestures { change, _ ->
                                        change.consume()
                                        val sx = (size.width / 300f).coerceAtLeast(0.1f)
                                        val sy = (size.height / 300f).coerceAtLeast(0.1f)
                                        onPointerInput(change.position.x / sx, change.position.y / sy, false)
                                    }
                                }
                        ) {
                            val sx = size.width / 300f
                            val sy = size.height / 300f
                            val avg = (sx + sy) * 0.5f

                            // Background grid
                            for (g in 30..270 step 30) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.04f),
                                    start = Offset(g * sx, 0f),
                                    end = Offset(g * sx, size.height)
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.04f),
                                    start = Offset(0f, g * sy),
                                    end = Offset(size.width, g * sy)
                                )
                            }

                            if (gameConfig.templateType == GameTemplateType.ORBIT_DEFENDER) {
                                drawCircle(
                                    color = Color(0xFF8C62FF).copy(alpha = 0.35f),
                                    radius = 92f * avg,
                                    center = Offset(150f * sx, 150f * sy),
                                    style = Stroke(width = 4f)
                                )
                            }

                            // Draw entities
                            for (ent in gameRuntime.entities) {
                                val col = parseComposeColor(ent.colorHex, Color(0xFF00E5FF))
                                val left = (ent.x - ent.width / 2f) * sx
                                val top = (ent.y - ent.height / 2f) * sy
                                val w = ent.width * sx
                                val h = ent.height * sy

                                if (ent.kind == EntityKind.BALL || ent.kind == EntityKind.CRYSTAL || ent.kind == EntityKind.ORBIT_TARGET) {
                                    drawCircle(
                                        color = col,
                                        radius = (ent.width * 0.5f) * avg,
                                        center = Offset(ent.x * sx, ent.y * sy)
                                    )
                                } else {
                                    drawRoundRect(
                                        color = col,
                                        topLeft = Offset(left, top),
                                        size = Size(w, h),
                                        cornerRadius = CornerRadius(6f * avg, 6f * avg)
                                    )
                                }

                                if (gameConfig.showDebugHitboxes) {
                                    drawRect(
                                        color = Color(0xFFFFAB00),
                                        topLeft = Offset(left, top),
                                        size = Size(w, h),
                                        style = Stroke(width = 1.5f)
                                    )
                                }
                            }

                            // Draw Player Sprite / Paddle / Orbiter
                            val px = gameRuntime.playerX * sx
                            val py = gameRuntime.playerY * sy
                            val pw = if (gameConfig.templateType == GameTemplateType.CYBER_PONG) 72f * sx else 42f * sx
                            val ph = if (gameConfig.templateType == GameTemplateType.CYBER_PONG) 12f * sy else 20f * sy
                            drawRoundRect(
                                color = Color(0xFF00E5FF),
                                topLeft = Offset(px - pw / 2f, py - ph / 2f),
                                size = Size(pw, ph),
                                cornerRadius = CornerRadius(8f * avg, 8f * avg)
                            )
                            if (gameConfig.showDebugHitboxes) {
                                drawRect(
                                    color = Color(0xFF00E676),
                                    topLeft = Offset(px - pw / 2f, py - ph / 2f),
                                    size = Size(pw, ph),
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartOrReset,
                            modifier = Modifier.weight(1f).testTag("start_game_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (gameRuntime.isRunning) "Перезапуск" else "Старт гри")
                        }
                        FilledTonalButton(
                            onClick = onTogglePause,
                            modifier = Modifier.weight(1f).testTag("pause_game_button")
                        ) {
                            Icon(
                                if (gameRuntime.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (gameRuntime.isRunning) "Пауза" else "Продовжити")
                        }
                    }
                }
            }
        }

        // Physics & Game Mechanics Inspector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Параметри фізики та ігрової механіки",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Базова швидкість (${(gameConfig.baseSpeed * 100).roundToInt()}%):", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = gameConfig.baseSpeed,
                        onValueChange = { s -> onUpdateConfig { it.copy(baseSpeed = s) } },
                        valueRange = 0.5f..2.2f
                    )

                    Text("Гравітація (${(gameConfig.gravity * 100).roundToInt() / 100f}):", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = gameConfig.gravity,
                        onValueChange = { g -> onUpdateConfig { it.copy(gravity = g) } },
                        valueRange = 0f..2.5f
                    )

                    Text("Ціль перемоги (${gameConfig.targetWinScore} очок):", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = gameConfig.targetWinScore.toFloat(),
                        onValueChange = { w -> onUpdateConfig { it.copy(targetWinScore = w.roundToInt()) } },
                        valueRange = 50f..500f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Налагодження: показувати хітбокси колізій", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = gameConfig.showDebugHitboxes,
                            onCheckedChange = { chk -> onUpdateConfig { it.copy(showDebugHitboxes = chk) } }
                        )
                    }

                    AssistChip(
                        onClick = onSyncScoreToWidget,
                        label = { Text("Синхронізувати рекорд (${gameConfig.highScore} PTS) із віджетом") }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
