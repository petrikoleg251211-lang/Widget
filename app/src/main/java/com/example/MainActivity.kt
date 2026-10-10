package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.InstalledPluginEntity
import com.example.data.ProjectVersionEntity
import com.example.data.StudioLogEntity
import com.example.data.StudioSettings
import com.example.data.WidgetProjectEntity
import com.example.model.StudioDestination
import com.example.model.WidgetTemplateItem
import com.example.ui.screens.AnimationStudioScreen
import com.example.ui.screens.ApiDocsScreen
import com.example.ui.screens.CodeStudioScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ErrorLogScreen
import com.example.ui.screens.MiniGameStudioScreen
import com.example.ui.screens.PluginsScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TemplateLibraryScreen
import com.example.ui.screens.VisualEditorScreen
import com.example.ui.theme.WidgetStudioXTheme
import com.example.viewmodel.EditorWorkspaceState
import com.example.viewmodel.StudioViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val studioViewModel: StudioViewModel = viewModel()
            val settings by studioViewModel.settings.collectAsStateWithLifecycle()

            WidgetStudioXTheme(darkTheme = settings.darkTheme) {
                WidgetStudioXApp(viewModel = studioViewModel)
            }
        }
    }
}

private data class NavItemSpec(
    val destination: StudioDestination,
    val label: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val testTag: String
)

/**
 * Primary Bottom / Rail Navigation Items featuring direct top-level switching between
 * Dashboard, My Projects, Visual Editor, Code Studio, and Settings without fragments.
 */
private val PRIMARY_NAV_ITEMS = listOf(
    NavItemSpec(StudioDestination.DASHBOARD, "Головна", "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    NavItemSpec(StudioDestination.PROJECTS, "Мої проєкти", "My Projects", Icons.Default.Folder, "nav_projects"),
    NavItemSpec(StudioDestination.VISUAL_EDITOR, "Редактор", "Visual Editor", Icons.Default.DesignServices, "nav_visual_editor"),
    NavItemSpec(StudioDestination.CODE_STUDIO, "Код IDE", "Code Studio", Icons.Default.Code, "nav_code_studio"),
    NavItemSpec(StudioDestination.SETTINGS, "Налаштування", "Settings", Icons.Default.Settings, "nav_settings")
)

private val ALL_DRAWER_ITEMS = listOf(
    NavItemSpec(StudioDestination.DASHBOARD, "Головна панель", "Dashboard", Icons.Default.Dashboard, "drawer_dashboard"),
    NavItemSpec(StudioDestination.PROJECTS, "Мої проєкти (My Projects)", "My Projects", Icons.Default.Folder, "drawer_projects"),
    NavItemSpec(StudioDestination.VISUAL_EDITOR, "Візуальний редактор (Visual Editor)", "Visual Editor", Icons.Default.DesignServices, "drawer_visual_editor"),
    NavItemSpec(StudioDestination.CODE_STUDIO, "Code Studio (WidgetScript)", "Code Studio", Icons.Default.Code, "drawer_code_studio"),
    NavItemSpec(StudioDestination.ANIMATION_STUDIO, "Анімаційна студія", "Animation Studio", Icons.Default.Animation, "drawer_animation_studio"),
    NavItemSpec(StudioDestination.GAME_STUDIO, "Студія мініігор 2D", "Mini Game Studio", Icons.Default.SportsEsports, "drawer_game_studio"),
    NavItemSpec(StudioDestination.TEMPLATE_LIBRARY, "Бібліотека шаблонів", "Template Library", Icons.Default.ViewQuilt, "drawer_templates"),
    NavItemSpec(StudioDestination.PLUGINS, "Плагіни та розширення", "Plugins", Icons.Default.Extension, "drawer_plugins"),
    NavItemSpec(StudioDestination.API_DOCS, "Документація API", "API Docs", Icons.Default.MenuBook, "drawer_api_docs"),
    NavItemSpec(StudioDestination.ERROR_LOG, "Журнал та Діагностика", "Error Log", Icons.Default.BugReport, "drawer_error_log"),
    NavItemSpec(StudioDestination.SETTINGS, "Налаштування (Settings)", "Settings", Icons.Default.Settings, "drawer_settings")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetStudioXApp(viewModel: StudioViewModel) {
    val workspace by viewModel.workspace.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val versions by viewModel.projectVersions.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Widget Studio X",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Проєкт: ${workspace.projectName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    ALL_DRAWER_ITEMS.forEach { item ->
                        NavigationDrawerItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = workspace.currentDestination == item.destination,
                            onClick = {
                                viewModel.navigateTo(item.destination)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 680.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            navigationIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_drawer_button")
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Меню студії")
                                    }
                                    if (workspace.currentDestination != StudioDestination.DASHBOARD) {
                                        IconButton(
                                            onClick = { viewModel.navigateBack() },
                                            modifier = Modifier.testTag("top_back_button")
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                                        }
                                    }
                                }
                            },
                            title = {
                                Column {
                                    Text(
                                        text = workspace.currentDestination.titleUk,
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${workspace.projectName} • ${workspace.gridSize.cellsW}x${workspace.gridSize.cellsH}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.navigateTo(StudioDestination.PROJECTS) },
                                    modifier = Modifier.testTag("top_projects_button")
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = "Мої проєкти")
                                }
                                IconButton(
                                    onClick = { viewModel.navigateTo(StudioDestination.VISUAL_EDITOR) },
                                    modifier = Modifier.testTag("top_visual_editor_button")
                                ) {
                                    Icon(Icons.Default.DesignServices, contentDescription = "Візуальний редактор")
                                }
                                IconButton(
                                    onClick = { viewModel.navigateTo(StudioDestination.SETTINGS) },
                                    modifier = Modifier.testTag("top_settings_button")
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = "Налаштування")
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.saveCurrentProject(
                                            createSnapshot = true,
                                            commitNote = "Ручне збереження у студії"
                                        )
                                    },
                                    modifier = Modifier.testTag("top_save_button")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Зберегти версію")
                                }
                                IconButton(
                                    onClick = { viewModel.pinCurrentProjectToAndroidWidget() },
                                    modifier = Modifier.testTag("top_pin_widget_button")
                                ) {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = "Встановити віджет на Android",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )

                        // Top-Level Composable Screen Switcher Pill Strip
                        TopLevelScreenSwitcherBar(
                            currentDestination = workspace.currentDestination,
                            onSelectDestination = { viewModel.navigateTo(it) }
                        )
                    }
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                            PRIMARY_NAV_ITEMS.forEach { item ->
                                NavigationBarItem(
                                    selected = workspace.currentDestination == item.destination,
                                    onClick = { viewModel.navigateTo(item.destination) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(modifier = Modifier.testTag("side_navigation_rail")) {
                            PRIMARY_NAV_ITEMS.forEach { item ->
                                NavigationRailItem(
                                    selected = workspace.currentDestination == item.destination,
                                    onClick = { viewModel.navigateTo(item.destination) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Dismissible Status Notification Banner
                        AnimatedVisibility(visible = workspace.statusBannerMessage != null) {
                            workspace.statusBannerMessage?.let { msg ->
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .testTag("status_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { viewModel.clearStatusBanner() }) {
                                            Text("OK")
                                        }
                                    }
                                }
                            }
                        }

                        // Fragment-free Top-Level Composable Screen Switcher
                        StudioScreenSwitcher(
                            workspace = workspace,
                            projects = projects,
                            plugins = plugins,
                            logs = logs,
                            settings = settings,
                            versions = versions,
                            templates = viewModel.templates,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopLevelScreenSwitcherBar(
    currentDestination: StudioDestination,
    onSelectDestination: (StudioDestination) -> Unit
) {
    val switcherTabs = listOf(
        Triple(StudioDestination.PROJECTS, "Мої проєкти (My Projects)", "switcher_my_projects"),
        Triple(StudioDestination.VISUAL_EDITOR, "Візуальний редактор (Visual Editor)", "switcher_visual_editor"),
        Triple(StudioDestination.SETTINGS, "Налаштування (Settings)", "switcher_settings"),
        Triple(StudioDestination.DASHBOARD, "Головна", "switcher_dashboard"),
        Triple(StudioDestination.CODE_STUDIO, "Code Studio", "switcher_code_studio"),
        Triple(StudioDestination.ANIMATION_STUDIO, "Анімація", "switcher_animation_studio"),
        Triple(StudioDestination.GAME_STUDIO, "Мініігри", "switcher_game_studio"),
        Triple(StudioDestination.TEMPLATE_LIBRARY, "Шаблони", "switcher_templates"),
        Triple(StudioDestination.PLUGINS, "Плагіни", "switcher_plugins"),
        Triple(StudioDestination.API_DOCS, "API Docs", "switcher_api_docs"),
        Triple(StudioDestination.ERROR_LOG, "Журнал", "switcher_error_log")
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_level_screen_switcher_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            switcherTabs.forEach { (destination, label, tag) ->
                val isSelected = currentDestination == destination
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectDestination(destination) },
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag(tag)
                )
            }
        }
    }
}

/**
 * Pure Jetpack Compose Top-Level Screen Switcher with state-driven routing and BackHandler support.
 */
@Composable
fun StudioScreenSwitcher(
    workspace: EditorWorkspaceState,
    projects: List<WidgetProjectEntity>,
    plugins: List<InstalledPluginEntity>,
    logs: List<StudioLogEntity>,
    settings: StudioSettings,
    versions: List<ProjectVersionEntity>,
    templates: List<WidgetTemplateItem>,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    // Handle system back navigation in all secondary screens
    BackHandler(enabled = workspace.currentDestination != StudioDestination.DASHBOARD) {
        viewModel.navigateBack()
    }

    Box(modifier = modifier.testTag("studio_screen_switcher_host")) {
        AnimatedContent(
            targetState = workspace.currentDestination,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
            },
            label = "StudioScreenSwitcherTransition"
        ) { targetScreen ->
            when (targetScreen) {
                StudioDestination.DASHBOARD -> DashboardScreen(
                    workspace = workspace,
                    projectsCount = projects.size,
                    pluginsCount = plugins.count { it.enabled },
                    onNavigate = { viewModel.navigateTo(it) },
                    onPinWidget = { viewModel.pinCurrentProjectToAndroidWidget() },
                    onSimulateTap = { viewModel.runActiveScript(triggerTap = true) }
                )
                StudioDestination.PROJECTS -> {
                    BackHandler { viewModel.navigateBack() }
                    ProjectsScreen(
                        projects = projects,
                        activeProjectId = workspace.activeProjectId,
                        versions = versions,
                        templates = templates,
                        exportedBundleJson = workspace.exportedBundleJson,
                        onSelectProject = { id, dest -> viewModel.selectProject(id, dest) },
                        onCreateProject = { tpl, name -> viewModel.createNewProjectFromTemplate(tpl, name) },
                        onRenameProject = { id, name -> viewModel.renameProject(id, name) },
                        onDuplicateProject = { id -> viewModel.duplicateProject(id) },
                        onDeleteProject = { id -> viewModel.deleteProject(id) },
                        onSaveSnapshot = { note -> viewModel.saveCurrentProject(true, note) },
                        onRestoreVersion = { ver -> viewModel.restoreProjectVersion(ver) },
                        onExportBundle = { viewModel.exportCurrentProjectBundle() },
                        onImportBundle = { json -> viewModel.importProjectBundle(json) },
                        onPinActiveToAndroid = { viewModel.pinCurrentProjectToAndroidWidget() }
                    )
                }
                StudioDestination.VISUAL_EDITOR -> {
                    BackHandler { viewModel.navigateBack() }
                    VisualEditorScreen(
                        workspace = workspace,
                        settings = settings,
                        onSelectElement = { viewModel.selectElement(it) },
                        onAddElement = { viewModel.addElement(it) },
                        onUpdateSelected = { viewModel.updateSelectedElement(it) },
                        onDragElement = { id, dx, dy -> viewModel.moveElementByDrag(id, dx, dy) },
                        onAlignSelected = { viewModel.alignSelectedElement(it) },
                        onReorderLayer = { viewModel.reorderSelectedLayer(it) },
                        onCopySelected = { viewModel.copySelectedElement() },
                        onPasteClipboard = { viewModel.pasteClipboardElement() },
                        onDeleteSelected = { viewModel.deleteSelectedElement() },
                        onToggleLock = { viewModel.toggleLockSelectedElement() },
                        onToggleGroup = { viewModel.toggleGroupSelectedElement() },
                        onUndo = { viewModel.undoVisualEdit() },
                        onRedo = { viewModel.redoVisualEdit() },
                        onSetGridSize = { viewModel.setWidgetGridSize(it) },
                        onSetBgAndRadius = { bg, r -> viewModel.setWidgetBackgroundAndRadius(bg, r) },
                        onSetZoom = { viewModel.setCanvasZoom(it) },
                        onNavigateToCode = { viewModel.navigateTo(StudioDestination.CODE_STUDIO) },
                        onPinWidget = { viewModel.pinCurrentProjectToAndroidWidget() }
                    )
                }
                StudioDestination.CODE_STUDIO -> {
                    BackHandler { viewModel.navigateBack() }
                    CodeStudioScreen(
                        workspace = workspace,
                        settings = settings,
                        onSelectFile = { viewModel.selectCodeFile(it) },
                        onCreateFile = { viewModel.createNewCodeFile(it) },
                        onUpdateCode = { viewModel.updateActiveFileContent(it) },
                        onFormatCode = { viewModel.formatActiveFile() },
                        onSearchReplace = { f, r -> viewModel.searchAndReplaceInActiveFile(f, r) },
                        onToggleSplitPreview = { viewModel.toggleSplitPreview() },
                        onRunScript = { tap -> viewModel.runActiveScript(tap) }
                    )
                }
                StudioDestination.ANIMATION_STUDIO -> {
                    BackHandler { viewModel.navigateBack() }
                    AnimationStudioScreen(
                        workspace = workspace,
                        settings = settings,
                        onSelectTrack = { viewModel.selectAnimationTrack(it) },
                        onTogglePlayback = { viewModel.toggleAnimationPlayback() },
                        onSeekTimeline = { viewModel.seekAnimationTimeline(it) },
                        onApplyPreset = { viewModel.applyAnimationPreset(it) },
                        onUpdateSelectedTrack = { viewModel.updateSelectedAnimationTrack(it) },
                        onAddKeyframe = { t, v, e -> viewModel.addKeyframeToSelectedTrack(t, v, e) },
                        onDeleteTrack = { viewModel.deleteAnimationTrack(it) }
                    )
                }
                StudioDestination.GAME_STUDIO -> {
                    BackHandler { viewModel.navigateBack() }
                    MiniGameStudioScreen(
                        workspace = workspace,
                        onSelectTemplate = { viewModel.selectGameTemplate(it) },
                        onUpdateConfig = { viewModel.updateGameConfig(it) },
                        onStartOrReset = { viewModel.startOrResetMiniGame() },
                        onTogglePause = { viewModel.togglePauseMiniGame() },
                        onStepFrame = { viewModel.stepMiniGameFrame(it) },
                        onPointerInput = { x, y, tap -> viewModel.onMiniGamePointerInput(x, y, tap) },
                        onSyncScoreToWidget = {
                            viewModel.saveCurrentProject(
                                createSnapshot = false,
                                commitNote = "Синхронізація рекорду мінігри"
                            )
                            viewModel.showStatus("Рекорд синхронізовано з віджетом!")
                        }
                    )
                }
                StudioDestination.TEMPLATE_LIBRARY -> {
                    BackHandler { viewModel.navigateBack() }
                    TemplateLibraryScreen(
                        templates = templates,
                        onCloneTemplate = { tpl, name -> viewModel.createNewProjectFromTemplate(tpl, name) }
                    )
                }
                StudioDestination.PLUGINS -> {
                    BackHandler { viewModel.navigateBack() }
                    PluginsScreen(
                        plugins = plugins,
                        onTogglePlugin = { id, en -> viewModel.togglePlugin(id, en) },
                        onInstallCustomPlugin = { json -> viewModel.installCustomPlugin(json) },
                        onRemoveCustomPlugin = { id -> viewModel.removeCustomPlugin(id) }
                    )
                }
                StudioDestination.API_DOCS -> {
                    BackHandler { viewModel.navigateBack() }
                    ApiDocsScreen()
                }
                StudioDestination.ERROR_LOG -> {
                    BackHandler { viewModel.navigateBack() }
                    ErrorLogScreen(
                        logs = logs,
                        diagnostics = workspace.diagnostics,
                        onClearLogs = { viewModel.clearSystemLogs() }
                    )
                }
                StudioDestination.SETTINGS -> {
                    BackHandler { viewModel.navigateBack() }
                    SettingsScreen(
                        settings = settings,
                        onSetDarkTheme = { viewModel.setDarkTheme(it) },
                        onSetCodeFontSize = { viewModel.setCodeFontSize(it) },
                        onSetSnapToGrid = { viewModel.setSnapToGrid(it) },
                        onSetShowGuides = { viewModel.setShowAlignmentGuides(it) },
                        onSetAutoSave = { viewModel.setAutoSave(it) },
                        onSetLowPowerMode = { viewModel.setLowPowerMode(it) },
                        onSetSandboxLimit = { viewModel.setSandboxLimit(it) }
                    )
                }
            }
        }
    }
}
