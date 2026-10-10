package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ProjectVersionEntity
import com.example.data.WidgetProjectEntity
import com.example.engine.animation.AnimationEngine
import com.example.model.StudioDestination
import com.example.model.WidgetTemplateItem
import com.example.ui.components.InteractiveWidgetCanvas
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.EditorWorkspaceState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    workspace: EditorWorkspaceState,
    projectsCount: Int,
    pluginsCount: Int,
    onNavigate: (StudioDestination) -> Unit,
    onPinWidget: () -> Unit,
    onSimulateTap: () -> Unit
) {
    var animClockMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(32L)
            animClockMs += 32L
        }
    }

    val animatedElements = remember(workspace.elements, workspace.animations, animClockMs) {
        AnimationEngine.applyAnimationsToElements(workspace.elements, workspace.animations, animClockMs)
    }
    val particles = remember(workspace.animations, animClockMs) {
        val preset = workspace.animations.firstOrNull()?.particlePreset
            ?: com.example.model.ParticlePreset.NONE
        AnimationEngine.generateParticles(preset, animClockMs)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Hero Studio Banner
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(176.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero_1791628699014),
                        contentDescription = "Widget Studio X Hero Workspace",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x660D111A),
                                        Color(0xEE0D111A)
                                    )
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "WIDGET STUDIO X • ENGINE v1.0",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Платформа конструювання, коду та анімації віджетів",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Активний проєкт: ${workspace.projectName} (${workspace.gridSize.label})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB8C7E0)
                            )
                        }
                    }
                }
            }
        }

        // Telemetry & Engine Status Strip
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelemetryStatBadge("Батарея", "${workspace.telemetry.batteryLevel}%")
                    TelemetryStatBadge("Вільна RAM", "${workspace.telemetry.freeMemoryMb} МБ")
                    TelemetryStatBadge("Проєкти", "$projectsCount")
                    TelemetryStatBadge("Плагіни", "$pluginsCount активні")
                    TelemetryStatBadge("Sandbox", "${workspace.lastExecutionTimeMs} мс")
                }
            }
        }

        // Live Interactive Widget Sandbox Card
        item {
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Живий інтерактивний перегляд: ${workspace.projectName}",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Натисніть на віджет для перевірки подій on tap та анімацій",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AssistChip(
                            onClick = { onNavigate(StudioDestination.VISUAL_EDITOR) },
                            label = { Text(workspace.gridSize.label) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        InteractiveWidgetCanvas(
                            gridSize = workspace.gridSize,
                            backgroundColorHex = workspace.backgroundColor,
                            cornerRadius = workspace.cornerRadius,
                            elements = animatedElements,
                            selectedElementId = null,
                            telemetry = workspace.telemetry,
                            showGridAndGuides = false,
                            interactiveEditorMode = false,
                            particles = particles,
                            onWidgetTapPreview = onSimulateTap,
                            modifier = Modifier.widthIn(max = 310.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPinWidget,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pin_android_widget_button")
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Встановити віджет")
                        }
                        FilledTonalButton(
                            onClick = { onNavigate(StudioDestination.VISUAL_EDITOR) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_visual_editor_button")
                        ) {
                            Icon(Icons.Default.DesignServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Редактор")
                        }
                    }
                }
            }
        }

        // Studio Modules Grid
        item {
            Text(
                text = "Інструменти та студії платформи",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            val modules = listOf(
                ModuleCardSpec(
                    StudioDestination.VISUAL_EDITOR,
                    "Візуальний редактор",
                    "WYSIWYG-полотно, шари, прив'язки, сітка, властивості",
                    Icons.Default.DesignServices,
                    "module_visual_editor"
                ),
                ModuleCardSpec(
                    StudioDestination.CODE_STUDIO,
                    "Code Studio (IDE)",
                    "WidgetScript, автодоповнення, діагностика, консоль",
                    Icons.Default.Code,
                    "module_code_studio"
                ),
                ModuleCardSpec(
                    StudioDestination.ANIMATION_STUDIO,
                    "Анімаційна студія",
                    "Таймлайн, ключові кадри, криві плавності, частинки",
                    Icons.Default.Animation,
                    "module_animation_studio"
                ),
                ModuleCardSpec(
                    StudioDestination.GAME_STUDIO,
                    "Студія мініігор 2D",
                    "Ігровий цикл, спрайти, фізика, зіткнення, рекорди",
                    Icons.Default.SportsEsports,
                    "module_game_studio"
                ),
                ModuleCardSpec(
                    StudioDestination.PROJECTS,
                    "Менеджер проєктів",
                    "Версії, резервні копії, імпорт/експорт архівів",
                    Icons.Default.Folder,
                    "module_projects"
                ),
                ModuleCardSpec(
                    StudioDestination.TEMPLATE_LIBRARY,
                    "Бібліотека шаблонів",
                    "Годинники, погода, продуктивність, аркадні пресети",
                    Icons.Default.ViewQuilt,
                    "module_templates"
                ),
                ModuleCardSpec(
                    StudioDestination.PLUGINS,
                    "Плагіни та розширення",
                    "Перевірка маніфестів, права доступу, ізоляція API",
                    Icons.Default.Extension,
                    "module_plugins"
                ),
                ModuleCardSpec(
                    StudioDestination.API_DOCS,
                    "Документація та Архітектура",
                    "Специфікація WidgetScript, модулі, модель безпеки",
                    Icons.Default.MenuBook,
                    "module_api_docs"
                ),
                ModuleCardSpec(
                    StudioDestination.ERROR_LOG,
                    "Журнал та Діагностика",
                    "Події Sandbox, помилки сценаріїв, стан рантайму",
                    Icons.Default.BugReport,
                    "module_error_log"
                ),
                ModuleCardSpec(
                    StudioDestination.SETTINGS,
                    "Налаштування IDE",
                    "Тема, розмір шрифту, сітка, енергозбереження, ліміти",
                    Icons.Default.Settings,
                    "module_settings"
                )
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                modules.forEach { mod ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 150.dp)
                            .clickable { onNavigate(mod.destination) }
                            .testTag(mod.testTag)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = mod.icon,
                                        contentDescription = mod.title,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = mod.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mod.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class ModuleCardSpec(
    val destination: StudioDestination,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
private fun TelemetryStatBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = JetBrainsMonoFontFamily,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProjectsScreen(
    projects: List<WidgetProjectEntity>,
    activeProjectId: Long,
    versions: List<ProjectVersionEntity>,
    templates: List<WidgetTemplateItem>,
    exportedBundleJson: String,
    onSelectProject: (Long, StudioDestination?) -> Unit,
    onCreateProject: (WidgetTemplateItem, String) -> Unit,
    onRenameProject: (Long, String) -> Unit,
    onDuplicateProject: (Long) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onSaveSnapshot: (String) -> Unit,
    onRestoreVersion: (ProjectVersionEntity) -> Unit,
    onExportBundle: () -> Unit,
    onImportBundle: (String) -> Unit,
    onPinActiveToAndroid: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialogForId by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var snapshotNote by remember { mutableStateOf("Контрольна точка архітектури") }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("projects_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Менеджер проєктів", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Автозбереження, історія версій, резервні копії та експорт",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("create_project_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Новий проєкт")
                }
            }
        }

        // Actions strip for active project (Backup / Snapshot / Export / Import)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showSnapshotDialog = true },
                        modifier = Modifier.weight(1f).testTag("create_version_snapshot_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Знімок версії")
                    }
                    OutlinedButton(
                        onClick = {
                            onExportBundle()
                            showArchiveDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("archive_export_import_button")
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Імпорт / Експорт")
                    }
                }
            }
        }

        // List of Projects
        items(projects, key = { it.id }) { proj ->
            val isSelected = proj.id == activeProjectId
            val dateStr = remember(proj.updatedAt) {
                SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(proj.updatedAt))
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_card_${proj.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = proj.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (proj.isPinnedToHome) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "HOME WIDGET",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${proj.gridSizeCode} • Оновлено $dateStr • v${proj.schemaVersion}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    renameText = proj.name
                                    showRenameDialogForId = proj.id
                                }
                            ) {
                                Icon(Icons.Default.DesignServices, contentDescription = "Перейменувати")
                            }
                            IconButton(onClick = { onDuplicateProject(proj.id) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Дублювати")
                            }
                            if (projects.size > 1) {
                                IconButton(onClick = { onDeleteProject(proj.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Видалити",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = proj.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSelectProject(proj.id, StudioDestination.VISUAL_EDITOR) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isSelected) "Відкрити редактор" else "Активувати і редагувати")
                        }
                        FilledTonalButton(
                            onClick = {
                                onSelectProject(proj.id, null)
                                onPinActiveToAndroid()
                            }
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("На екран")
                        }
                    }
                }
            }
        }

        // Version History & Crash Recovery Section
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Історія версій активного проєкту (${versions.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(versions, key = { it.versionId }) { ver ->
            val vTime = remember(ver.timestamp) {
                SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date(ver.timestamp))
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Версія v${ver.versionNumber} • $vTime",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = ver.commitMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    OutlinedButton(onClick = { onRestoreVersion(ver) }) {
                        Text("Відновити")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }

    if (showCreateDialog) {
        var newProjName by remember { mutableStateOf("Мій смарт-віджет") }
        var selectedTpl by remember { mutableStateOf(templates.first()) }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Створити новий проєкт") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newProjName,
                        onValueChange = { newProjName = it },
                        label = { Text("Назва проєкту") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_project_name_input")
                    )
                    Text("Оберіть базовий шаблон архітектури:", style = MaterialTheme.typography.labelLarge)
                    templates.forEach { tpl ->
                        val chosen = tpl.id == selectedTpl.id
                        Surface(
                            color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTpl = tpl }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(tpl.title, fontWeight = FontWeight.Bold)
                                Text(tpl.category, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateProject(selectedTpl, newProjName)
                        showCreateDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_project_button")
                ) {
                    Text("Створити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Скасувати") }
            }
        )
    }

    if (showRenameDialogForId != null) {
        val targetId = showRenameDialogForId!!
        AlertDialog(
            onDismissRequest = { showRenameDialogForId = null },
            title = { Text("Перейменувати проєкт") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Нова назва") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameProject(targetId, renameText)
                        showRenameDialogForId = null
                    }
                ) { Text("Зберегти") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialogForId = null }) { Text("Скасувати") }
            }
        )
    }

    if (showSnapshotDialog) {
        AlertDialog(
            onDismissRequest = { showSnapshotDialog = false },
            title = { Text("Створити контрольний знімок версії") },
            text = {
                OutlinedTextField(
                    value = snapshotNote,
                    onValueChange = { snapshotNote = it },
                    label = { Text("Коментар до версії") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveSnapshot(snapshotNote)
                        showSnapshotDialog = false
                    }
                ) { Text("Зберегти версію") }
            },
            dismissButton = {
                TextButton(onClick = { showSnapshotDialog = false }) { Text("Скасувати") }
            }
        )
    }

    if (showArchiveDialog) {
        LaunchedEffect(exportedBundleJson) {
            if (importJsonInput.isBlank() && exportedBundleJson.isNotBlank()) {
                importJsonInput = exportedBundleJson
            }
        }
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Архів проєкту (WidgetStudioX.Bundle)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Вміст пакета перевіряється на цілісність схеми та відсутність заборонених системних викликів.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        label = { Text("JSON пакет проєкту") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("archive_json_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportBundle(importJsonInput)
                        showArchiveDialog = false
                    },
                    modifier = Modifier.testTag("confirm_import_bundle_button")
                ) {
                    Text("Перевірити та Імпортувати")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) { Text("Закрити") }
            }
        )
    }
}
