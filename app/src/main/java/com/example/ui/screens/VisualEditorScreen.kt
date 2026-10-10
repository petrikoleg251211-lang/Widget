package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.StudioSettings
import com.example.model.ElementType
import com.example.model.StudioDestination
import com.example.model.TapActionType
import com.example.model.WidgetElement
import com.example.model.WidgetGridSize
import com.example.ui.components.InteractiveWidgetCanvas
import com.example.ui.components.parseComposeColor
import com.example.viewmodel.EditorWorkspaceState
import kotlin.math.roundToInt

private val COLOR_SWATCHES = listOf(
    "#00E5FF", "#8C62FF", "#00E676", "#FFAB00",
    "#FF5252", "#FFFFFF", "#9FB0C7", "#1E2638",
    "#10131C", "#2A1B54", "#003B46", "#00000000"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisualEditorScreen(
    workspace: EditorWorkspaceState,
    settings: StudioSettings,
    onSelectElement: (String?) -> Unit,
    onAddElement: (ElementType) -> Unit,
    onUpdateSelected: ((WidgetElement) -> WidgetElement) -> Unit,
    onDragElement: (String, Float, Float) -> Unit,
    onAlignSelected: (String) -> Unit,
    onReorderLayer: (Boolean) -> Unit,
    onCopySelected: () -> Unit,
    onPasteClipboard: () -> Unit,
    onDeleteSelected: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleGroup: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSetGridSize: (WidgetGridSize) -> Unit,
    onSetBgAndRadius: (String, Float) -> Unit,
    onSetZoom: (Float) -> Unit,
    onNavigateToCode: () -> Unit,
    onPinWidget: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val selectedElement = workspace.elements.firstOrNull { it.id == workspace.selectedElementId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("visual_editor_screen")
    ) {
        // 1. Top Precision Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = workspace.undoDepth > 0,
                    modifier = Modifier.testTag("undo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Скасувати")
                }
                IconButton(
                    onClick = onRedo,
                    enabled = workspace.redoDepth > 0,
                    modifier = Modifier.testTag("redo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Повторити")
                }
                IconButton(
                    onClick = onCopySelected,
                    enabled = selectedElement != null,
                    modifier = Modifier.testTag("copy_element_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Копіювати")
                }
                IconButton(
                    onClick = onPasteClipboard,
                    enabled = workspace.clipboardElement != null,
                    modifier = Modifier.testTag("paste_element_button")
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Вставити")
                }
                IconButton(
                    onClick = onToggleLock,
                    enabled = selectedElement != null,
                    modifier = Modifier.testTag("lock_element_button")
                ) {
                    Icon(
                        if (selectedElement?.locked == true) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Блокування шару"
                    )
                }
                IconButton(
                    onClick = onToggleGroup,
                    enabled = selectedElement != null,
                    modifier = Modifier.testTag("group_element_button")
                ) {
                    Icon(
                        Icons.Default.GroupWork,
                        contentDescription = "Групувати",
                        tint = if (selectedElement?.groupId?.isNotBlank() == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDeleteSelected,
                    enabled = selectedElement != null,
                    modifier = Modifier.testTag("delete_element_button")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Видалити елемент",
                        tint = if (selectedElement != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { onSetZoom(workspace.canvasZoom - 0.15f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Зменшити масштаб")
                }
                Text(
                    text = "${(workspace.canvasZoom * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
                IconButton(onClick = { onSetZoom(workspace.canvasZoom + 0.15f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Збільшити масштаб")
                }

                AssistChip(
                    onClick = onNavigateToCode,
                    label = { Text("WidgetScript") },
                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("editor_to_code_button")
                )
                AssistChip(
                    onClick = onPinWidget,
                    label = { Text("На екран") },
                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        // 2. Interactive WYSIWYG Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f)
                .background(MaterialTheme.colorScheme.background)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxCanvasWidth = (240f * workspace.canvasZoom).coerceIn(160f, 360f).dp
            InteractiveWidgetCanvas(
                gridSize = workspace.gridSize,
                backgroundColorHex = workspace.backgroundColor,
                cornerRadius = workspace.cornerRadius,
                elements = workspace.elements,
                selectedElementId = workspace.selectedElementId,
                telemetry = workspace.telemetry,
                showGridAndGuides = settings.showAlignmentGuides,
                interactiveEditorMode = true,
                onSelectElement = onSelectElement,
                onDragElement = onDragElement,
                modifier = Modifier.widthIn(max = maxCanvasWidth)
            )
        }

        // 3. Workbench Tabs (Components / Inspector / Layers / Canvas Settings)
        val tabTitles = listOf("Інспектор", "Компоненти", "Шари (${workspace.elements.size})", "Полотно")
        TabRow(selectedTabIndex = activeTab) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = activeTab == idx,
                    onClick = { activeTab = idx },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("editor_tab_$idx")
                )
            }
        }

        // 4. Tab Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (activeTab) {
                0 -> PropertyInspectorTab(
                    selectedElement = selectedElement,
                    onUpdateSelected = onUpdateSelected,
                    onAlignSelected = onAlignSelected
                )
                1 -> ComponentPaletteTab(onAddElement = {
                    onAddElement(it)
                    activeTab = 0
                })
                2 -> LayersHierarchyTab(
                    elements = workspace.elements,
                    selectedElementId = workspace.selectedElementId,
                    onSelectElement = onSelectElement,
                    onReorderLayer = onReorderLayer,
                    onUpdateSelected = onUpdateSelected,
                    onDeleteSelected = onDeleteSelected
                )
                3 -> CanvasSettingsTab(
                    gridSize = workspace.gridSize,
                    backgroundColor = workspace.backgroundColor,
                    cornerRadius = workspace.cornerRadius,
                    onSetGridSize = onSetGridSize,
                    onSetBgAndRadius = onSetBgAndRadius
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComponentPaletteTab(onAddElement: (ElementType) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Додати компонент на полотно віджета",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Кожен елемент автоматично синхронізується з кодом WidgetScript у main.ws",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElementType.entries.forEach { type ->
                    FilledTonalButton(
                        onClick = { onAddElement(type) },
                        modifier = Modifier.testTag("add_element_${type.name.lowercase()}")
                    ) {
                        Text("+ ${type.labelUk}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PropertyInspectorTab(
    selectedElement: WidgetElement?,
    onUpdateSelected: ((WidgetElement) -> WidgetElement) -> Unit,
    onAlignSelected: (String) -> Unit
) {
    if (selectedElement == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Оберіть елемент на полотні або додайте новий у вкладці 'Компоненти'",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Елемент: ${selectedElement.name} (${selectedElement.type.labelUk})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (selectedElement.groupId.isNotBlank()) {
                    Text(
                        text = "Група: ${selectedElement.groupId}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Fast Alignment Controls
        item {
            Text("Швидке вирівнювання на полотні:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "LEFT" to "Ліворуч",
                    "CENTER_H" to "Центр Г",
                    "RIGHT" to "Праворуч",
                    "TOP" to "Зверху",
                    "CENTER_V" to "Центр В",
                    "BOTTOM" to "Знизу"
                ).forEach { (code, label) ->
                    OutlinedButton(onClick = { onAlignSelected(code) }) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Text / Value & Name
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selectedElement.name,
                    onValueChange = { newName -> onUpdateSelected { it.copy(name = newName) } },
                    label = { Text("ID / Ім'я шару") },
                    singleLine = true,
                    modifier = Modifier.weight(0.45f)
                )
                OutlinedTextField(
                    value = selectedElement.textValue,
                    onValueChange = { newVal -> onUpdateSelected { it.copy(textValue = newVal) } },
                    label = { Text("Текст / Значення") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.55f)
                        .testTag("inspector_text_value_input")
                )
            }
        }

        // Coordinates X / Y & Size W / H
        item {
            Text(
                text = "Координати X=${selectedElement.x.roundToInt()} Y=${selectedElement.y.roundToInt()} • Розмір ${selectedElement.width.roundToInt()}x${selectedElement.height.roundToInt()}",
                style = MaterialTheme.typography.labelMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Позиція X (${selectedElement.x.roundToInt()})", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.x,
                        onValueChange = { v -> onUpdateSelected { it.copy(x = v) } },
                        valueRange = 0f..280f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Позиція Y (${selectedElement.y.roundToInt()})", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.y,
                        onValueChange = { v -> onUpdateSelected { it.copy(y = v) } },
                        valueRange = 0f..280f
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ширина (${selectedElement.width.roundToInt()})", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.width,
                        onValueChange = { v -> onUpdateSelected { it.copy(width = v) } },
                        valueRange = 24f..284f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Висота (${selectedElement.height.roundToInt()})", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.height,
                        onValueChange = { v -> onUpdateSelected { it.copy(height = v) } },
                        valueRange = 4f..284f
                    )
                }
            }
        }

        // Typography, Rotation, Opacity, Corner Radius
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Розмір шрифту (${selectedElement.fontSize.roundToInt()} sp)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.fontSize,
                        onValueChange = { v -> onUpdateSelected { it.copy(fontSize = v) } },
                        valueRange = 10f..64f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Радіус кутів (${selectedElement.cornerRadius.roundToInt()} dp)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.cornerRadius,
                        onValueChange = { v -> onUpdateSelected { it.copy(cornerRadius = v) } },
                        valueRange = 0f..48f
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Поворот (${selectedElement.rotation.roundToInt()}°)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.rotation,
                        onValueChange = { v -> onUpdateSelected { it.copy(rotation = v) } },
                        valueRange = -180f..180f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Прозорість (${(selectedElement.opacity * 100).roundToInt()}%)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.opacity,
                        onValueChange = { v -> onUpdateSelected { it.copy(opacity = v) } },
                        valueRange = 0.1f..1f
                    )
                }
            }
        }

        // Progress Bar value or Border Width
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Обведення (${selectedElement.borderWidth.roundToInt()} px)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.borderWidth,
                        onValueChange = { v -> onUpdateSelected { it.copy(borderWidth = v) } },
                        valueRange = 0f..8f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Прогрес (${(selectedElement.progressValue * 100).roundToInt()}%)", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = selectedElement.progressValue,
                        onValueChange = { v -> onUpdateSelected { it.copy(progressValue = v) } },
                        valueRange = 0f..1f
                    )
                }
            }
        }

        // Color Pickers (Text/Primary Color & Background Color)
        item {
            Text("Колір тексту / індикатора (${selectedElement.textColor}):", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            ColorSwatchRow(
                selectedHex = selectedElement.textColor,
                onSelectHex = { hex -> onUpdateSelected { it.copy(textColor = hex) } }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Колір фону елемента (${selectedElement.backgroundColor}):", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            ColorSwatchRow(
                selectedHex = selectedElement.backgroundColor,
                onSelectHex = { hex -> onUpdateSelected { it.copy(backgroundColor = hex) } }
            )
        }

        // Interactive Tap Action
        item {
            Text("Поведінка при натисканні (onTap):", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TapActionType.entries.forEach { actionType ->
                    FilterChip(
                        selected = selectedElement.tapActionType == actionType,
                        onClick = { onUpdateSelected { it.copy(tapActionType = actionType) } },
                        label = { Text(actionType.labelUk) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ColorSwatchRow(
    selectedHex: String,
    onSelectHex: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        COLOR_SWATCHES.forEach { hex ->
            val col = parseComposeColor(hex, Color.Transparent)
            val isSelected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (hex == "#00000000") Color(0xFF2B3752) else col)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
                    .clickable { onSelectHex(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (hex == "#00000000") {
                    Text("∅", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun LayersHierarchyTab(
    elements: List<WidgetElement>,
    selectedElementId: String?,
    onSelectElement: (String?) -> Unit,
    onReorderLayer: (Boolean) -> Unit,
    onUpdateSelected: ((WidgetElement) -> WidgetElement) -> Unit,
    onDeleteSelected: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ієрархія шарів (зверху вниз)", style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = { onReorderLayer(true) }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Вище за порядком шарів")
                    }
                    IconButton(onClick = { onReorderLayer(false) }) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Нижче за порядком шарів")
                    }
                }
            }
        }

        items(elements.sortedByDescending { it.layerOrder }, key = { it.id }) { el ->
            val isSelected = el.id == selectedElementId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectElement(el.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#${el.layerOrder} • ${el.name} (${el.type.labelUk})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "X=${el.x.roundToInt()}, Y=${el.y.roundToInt()} • ${el.width.roundToInt()}x${el.height.roundToInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(
                            onClick = {
                                onSelectElement(el.id)
                                onUpdateSelected { it.copy(visible = !it.visible) }
                            }
                        ) {
                            Icon(
                                if (el.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Видимість шару"
                            )
                        }
                        IconButton(
                            onClick = {
                                onSelectElement(el.id)
                                onDeleteSelected()
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Видалити шар",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasSettingsTab(
    gridSize: WidgetGridSize,
    backgroundColor: String,
    cornerRadius: Float,
    onSetGridSize: (WidgetGridSize) -> Unit,
    onSetBgAndRadius: (String, Float) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Розмір сітки Android-віджета:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WidgetGridSize.entries.forEach { size ->
                    FilterChip(
                        selected = gridSize == size,
                        onClick = { onSetGridSize(size) },
                        label = { Text(size.label) }
                    )
                }
            }
        }

        item {
            Text("Колір фону віджета ($backgroundColor):", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ColorSwatchRow(
                selectedHex = backgroundColor,
                onSelectHex = { hex -> onSetBgAndRadius(hex, cornerRadius) }
            )
        }

        item {
            Text("Радіус заокруглення кутів віджета (${cornerRadius.roundToInt()} dp):", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = cornerRadius,
                onValueChange = { r -> onSetBgAndRadius(backgroundColor, r) },
                valueRange = 8f..44f
            )
        }
    }
}
