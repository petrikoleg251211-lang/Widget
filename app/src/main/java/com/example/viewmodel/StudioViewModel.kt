package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BuiltInCatalog
import com.example.data.InstalledPluginEntity
import com.example.data.ProjectVersionEntity
import com.example.data.SettingsPreferences
import com.example.data.StudioDatabase
import com.example.data.StudioLogEntity
import com.example.data.StudioRepository
import com.example.data.StudioSettings
import com.example.data.WidgetProjectEntity
import com.example.engine.animation.AnimationEngine
import com.example.engine.game.GameRuntimeState
import com.example.engine.game.MiniGameEngine
import com.example.engine.script.ScriptSymbol
import com.example.engine.script.SystemTelemetrySnapshot
import com.example.engine.script.WidgetScriptEngine
import com.example.model.AnimProperty
import com.example.model.AnimationTrack
import com.example.model.DiagnosticMessage
import com.example.model.EasingCurve
import com.example.model.ElementType
import com.example.model.GameTemplateType
import com.example.model.Keyframe
import com.example.model.MiniGameConfig
import com.example.model.ParticlePreset
import com.example.model.ProjectFile
import com.example.model.StudioDestination
import com.example.model.StudioJsonSerializer
import com.example.model.TapActionType
import com.example.model.TriggerType
import com.example.model.WidgetElement
import com.example.model.WidgetGridSize
import com.example.model.WidgetTemplateItem
import com.example.widget.StudioAppWidgetProvider
import com.example.widget.WidgetBitmapRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

data class EditorWorkspaceState(
    val currentDestination: StudioDestination = StudioDestination.DASHBOARD,
    val navigationHistory: List<StudioDestination> = emptyList(),
    val activeProjectId: Long = 0L,
    val projectName: String = "System Dashboard Pro",
    val projectDescription: String = "",
    val gridSize: WidgetGridSize = WidgetGridSize.SIZE_2X2,
    val backgroundColor: String = "#10131C",
    val cornerRadius: Float = 24f,
    val elements: List<WidgetElement> = emptyList(),
    val selectedElementId: String? = null,
    val clipboardElement: WidgetElement? = null,
    val canvasZoom: Float = 1.0f,
    val undoDepth: Int = 0,
    val redoDepth: Int = 0,
    // Code Studio
    val files: List<ProjectFile> = emptyList(),
    val activeFileName: String = "main.ws",
    val splitPreviewEnabled: Boolean = false,
    val diagnostics: List<DiagnosticMessage> = emptyList(),
    val consoleOutput: List<String> = listOf("[READY] Рушій WidgetScript v1.0 готовий до роботи."),
    val symbols: List<ScriptSymbol> = emptyList(),
    val lastExecutionTimeMs: Long = 1L,
    val lastInstructionCount: Int = 0,
    // Animation Studio
    val animations: List<AnimationTrack> = emptyList(),
    val selectedTrackId: String? = null,
    val isAnimationPlaying: Boolean = true,
    val timelinePositionMs: Long = 0L,
    // Mini Game Studio
    val gameConfig: MiniGameConfig = MiniGameConfig(),
    val gameRuntime: GameRuntimeState = GameRuntimeState(),
    // Live Telemetry & Storage
    val telemetry: SystemTelemetrySnapshot = SystemTelemetrySnapshot(),
    val localStorage: Map<String, Any> = mapOf("taps" to 0, "sprints" to 1, "game_score" to 120),
    // Export / Import / Banner notifications
    val exportedBundleJson: String = "",
    val statusBannerMessage: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val database = StudioDatabase.getInstance(application)
    private val repository = StudioRepository(database.studioDao())
    private val settingsPrefs = SettingsPreferences(application)
    private val gameEngine = MiniGameEngine()

    private val _workspace = MutableStateFlow(EditorWorkspaceState())
    val workspace: StateFlow<EditorWorkspaceState> = _workspace.asStateFlow()

    val projects: StateFlow<List<WidgetProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plugins: StateFlow<List<InstalledPluginEntity>> = repository.allPlugins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<StudioLogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<StudioSettings> = settingsPrefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioSettings())

    private val _projectVersions = MutableStateFlow<List<ProjectVersionEntity>>(emptyList())
    val projectVersions: StateFlow<List<ProjectVersionEntity>> = _projectVersions.asStateFlow()

    val templates: List<WidgetTemplateItem> = BuiltInCatalog.getTemplates()

    private val undoStack = ArrayDeque<List<WidgetElement>>()
    private val redoStack = ArrayDeque<List<WidgetElement>>()
    private val mutableStorage = mutableMapOf<String, Any>("taps" to 0, "sprints" to 1, "game_score" to 120)

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            refreshTelemetry()
            val initialProject = repository.getPinnedOrLatestProject()
            if (initialProject != null) {
                loadProjectIntoWorkspace(initialProject)
            }
        }
    }

    fun refreshTelemetry() {
        val snap = WidgetBitmapRenderer.readDeviceTelemetry(getApplication())
        _workspace.update { it.copy(telemetry = snap) }
    }

    fun navigateTo(destination: StudioDestination) {
        _workspace.update { state ->
            if (state.currentDestination == destination) state
            else state.copy(
                currentDestination = destination,
                navigationHistory = (state.navigationHistory + state.currentDestination).takeLast(12)
            )
        }
    }

    fun navigateBack(): Boolean {
        val current = _workspace.value
        if (current.navigationHistory.isNotEmpty()) {
            val prev = current.navigationHistory.last()
            _workspace.update {
                it.copy(
                    currentDestination = prev,
                    navigationHistory = it.navigationHistory.dropLast(1)
                )
            }
            return true
        }
        if (current.currentDestination != StudioDestination.DASHBOARD) {
            _workspace.update { it.copy(currentDestination = StudioDestination.DASHBOARD) }
            return true
        }
        return false
    }

    fun clearStatusBanner() {
        _workspace.update { it.copy(statusBannerMessage = null) }
    }

    fun showStatus(msg: String) {
        _workspace.update { it.copy(statusBannerMessage = msg) }
    }

    // =========================================================================
    // PROJECT MANAGEMENT
    // =========================================================================

    fun selectProject(projectId: Long, openDestination: StudioDestination? = null) {
        viewModelScope.launch {
            val entity = repository.getProjectById(projectId) ?: return@launch
            loadProjectIntoWorkspace(entity)
            if (openDestination != null) {
                navigateTo(openDestination)
            }
        }
    }

    private fun loadProjectIntoWorkspace(entity: WidgetProjectEntity) {
        val elements = StudioJsonSerializer.elementsFromJson(entity.elementsJson)
        val files = StudioJsonSerializer.filesFromJson(entity.filesJson).ifEmpty {
            listOf(ProjectFile("main.ws", "widgetscript", "", isEntry = true))
        }
        val anims = StudioJsonSerializer.animationsFromJson(entity.animationsJson)
        val gameCfg = StudioJsonSerializer.gameConfigFromJson(entity.gameConfigJson)

        mutableStorage.clear()
        runCatching {
            val obj = JSONObject(entity.localStorageJson)
            for (k in obj.keys()) mutableStorage[k] = obj.get(k)
        }

        undoStack.clear()
        redoStack.clear()

        val entryFile = files.firstOrNull { it.isEntry } ?: files.first()
        val engine = createScriptEngine()
        val symbols = engine.extractSymbols(entryFile.content)
        val exec = engine.execute(
            script = entryFile.content,
            fileName = entryFile.fileName,
            telemetry = _workspace.value.telemetry,
            localStorage = mutableStorage,
            registeredPluginNamespaces = getEnabledPluginNamespaces()
        )

        _workspace.update { state ->
            state.copy(
                activeProjectId = entity.id,
                projectName = entity.name,
                projectDescription = entity.description,
                gridSize = WidgetGridSize.fromCode(entity.gridSizeCode),
                backgroundColor = entity.backgroundColor,
                cornerRadius = entity.cornerRadius,
                elements = elements.ifEmpty { exec.elements },
                selectedElementId = elements.firstOrNull()?.id,
                undoDepth = 0,
                redoDepth = 0,
                files = files,
                activeFileName = entryFile.fileName,
                diagnostics = exec.diagnostics,
                symbols = symbols,
                lastExecutionTimeMs = exec.executionTimeMs,
                lastInstructionCount = exec.instructionsExecuted,
                animations = anims,
                selectedTrackId = anims.firstOrNull()?.id,
                gameConfig = gameCfg,
                gameRuntime = gameEngine.createInitialState(gameCfg),
                localStorage = mutableStorage.toMap()
            )
        }

        viewModelScope.launch {
            repository.observeVersions(entity.id).collect { vers ->
                _projectVersions.value = vers
            }
        }
    }

    fun createNewProjectFromTemplate(template: WidgetTemplateItem, customName: String) {
        viewModelScope.launch {
            val id = repository.createProjectFromTemplate(template, customName)
            val created = repository.getProjectById(id)
            if (created != null) {
                loadProjectIntoWorkspace(created)
                showStatus("Створено проєкт '${created.name}'")
                navigateTo(StudioDestination.VISUAL_EDITOR)
            }
        }
    }

    fun saveCurrentProject(createSnapshot: Boolean = false, commitNote: String = "Збереження користувачем") {
        viewModelScope.launch {
            val st = _workspace.value
            if (st.activeProjectId == 0L) return@launch
            val existing = repository.getProjectById(st.activeProjectId) ?: return@launch
            val storageJson = JSONObject().apply {
                for ((k, v) in st.localStorage) put(k, v)
            }.toString()
            val updated = existing.copy(
                name = st.projectName,
                description = st.projectDescription,
                gridSizeCode = WidgetGridSize.toCode(st.gridSize),
                backgroundColor = st.backgroundColor,
                cornerRadius = st.cornerRadius,
                elementsJson = StudioJsonSerializer.elementsToJson(st.elements),
                filesJson = StudioJsonSerializer.filesToJson(st.files),
                animationsJson = StudioJsonSerializer.animationsToJson(st.animations),
                gameConfigJson = StudioJsonSerializer.gameConfigToJson(st.gameConfig),
                localStorageJson = storageJson
            )
            repository.saveProject(updated, createVersionSnapshot = createSnapshot, commitNote = commitNote)
            if (createSnapshot) {
                showStatus("Збережено версію: $commitNote")
            }
        }
    }

    fun renameProject(projectId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newName)
            if (_workspace.value.activeProjectId == projectId) {
                _workspace.update { it.copy(projectName = newName.trim().ifEmpty { it.projectName }) }
            }
            showStatus("Проєкт перейменовано")
        }
    }

    fun duplicateProject(projectId: Long) {
        viewModelScope.launch {
            val newId = repository.duplicateProject(projectId)
            if (newId != null) {
                showStatus("Створено копію проєкту")
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            val remaining = projects.value.filter { it.id != projectId }
            if (_workspace.value.activeProjectId == projectId && remaining.isNotEmpty()) {
                loadProjectIntoWorkspace(remaining.first())
            }
            showStatus("Проєкт видалено")
        }
    }

    fun restoreProjectVersion(version: ProjectVersionEntity) {
        viewModelScope.launch {
            val restored = repository.restoreVersion(version.projectId, version)
            if (restored != null) {
                loadProjectIntoWorkspace(restored)
                showStatus("Відновлено версію v${version.versionNumber}: ${version.commitMessage}")
            }
        }
    }

    fun pinCurrentProjectToAndroidWidget() {
        viewModelScope.launch {
            saveCurrentProject(createSnapshot = true, commitNote = "Публікація на домашній екран Android")
            val id = _workspace.value.activeProjectId
            repository.setActiveHomeProject(id)
            val context = getApplication<Application>()
            val pinnedDialogShown = StudioAppWidgetProvider.requestPinWidget(context)
            StudioAppWidgetProvider.updateAllWidgets(context)
            showStatus(
                if (pinnedDialogShown) "Запит на закріплення віджета відправлено системі Android!"
                else "Віджет синхронізовано з AppWidgetProvider та оновлено!"
            )
        }
    }

    fun exportCurrentProjectBundle() {
        viewModelScope.launch {
            saveCurrentProject()
            val json = repository.exportProjectArchiveJson(_workspace.value.activeProjectId)
            _workspace.update {
                it.copy(
                    exportedBundleJson = json,
                    statusBannerMessage = "Архів проєкту згенеровано та готово до копіювання/експорту!"
                )
            }
        }
    }

    fun importProjectBundle(bundleJson: String) {
        viewModelScope.launch {
            val res = repository.importProjectArchiveJson(bundleJson)
            showStatus(res.message)
            if (res.success && res.projectId != 0L) {
                selectProject(res.projectId)
            }
        }
    }

    // =========================================================================
    // VISUAL WIDGET EDITOR
    // =========================================================================

    private fun pushUndoSnapshot() {
        undoStack.addLast(_workspace.value.elements)
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undoVisualEdit() {
        if (undoStack.isEmpty()) return
        val current = _workspace.value.elements
        redoStack.addLast(current)
        val previous = undoStack.removeLast()
        _workspace.update {
            it.copy(
                elements = previous,
                undoDepth = undoStack.size,
                redoDepth = redoStack.size
            )
        }
        autoSaveIfEnabled()
    }

    fun redoVisualEdit() {
        if (redoStack.isEmpty()) return
        val current = _workspace.value.elements
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        _workspace.update {
            it.copy(
                elements = next,
                undoDepth = undoStack.size,
                redoDepth = redoStack.size
            )
        }
        autoSaveIfEnabled()
    }

    fun selectElement(elementId: String?) {
        _workspace.update { it.copy(selectedElementId = elementId) }
    }

    fun addElement(type: ElementType) {
        pushUndoSnapshot()
        val count = _workspace.value.elements.size + 1
        val id = "${type.name.lowercase()}_$count"
        val defaultText = when (type) {
            ElementType.TEXT -> "Новий текст $count"
            ElementType.CLOCK -> "12:45"
            ElementType.DATE_CALENDAR -> "Суб, 10 Жовт"
            ElementType.BATTERY -> "Battery: ${_workspace.value.telemetry.batteryLevel}%"
            ElementType.SYSTEM_METRIC -> "RAM: ${_workspace.value.telemetry.freeMemoryMb} MB"
            ElementType.BUTTON -> "⚡ Дія $count"
            ElementType.GAME_OBJECT -> "🛸 Спрайт $count"
            ElementType.ICON -> "★ Іконка"
            ElementType.CUSTOM_GRAPHIC -> "◈ Вектор"
            ElementType.INTERACTIVE_ZONE -> "Tap Zone"
            ElementType.ANIMATED_PARTICLE -> "✨ Частинки"
            else -> ""
        }
        val newEl = WidgetElement(
            id = id,
            name = id,
            type = type,
            x = 32f + (count * 12f) % 100f,
            y = 32f + (count * 16f) % 140f,
            width = when (type) {
                ElementType.CONTAINER, ElementType.PROGRESS_BAR, ElementType.DIVIDER_LINE -> 220f
                else -> 148f
            },
            height = when (type) {
                ElementType.DIVIDER_LINE -> 4f
                ElementType.PROGRESS_BAR -> 22f
                ElementType.CONTAINER -> 90f
                else -> 48f
            },
            backgroundColor = when (type) {
                ElementType.TEXT, ElementType.CLOCK, ElementType.DATE_CALENDAR, ElementType.BATTERY -> "#00000000"
                ElementType.BUTTON -> "#00E5FF"
                ElementType.SHAPE, ElementType.CONTAINER -> "#1E2638"
                else -> "#232D42"
            },
            textColor = if (type == ElementType.BUTTON) "#00262E" else "#FFFFFF",
            fontSize = if (type == ElementType.CLOCK) 36f else 15f,
            textValue = defaultText,
            borderWidth = if (type == ElementType.CONTAINER || type == ElementType.SHAPE) 1.5f else 0f,
            layerOrder = _workspace.value.elements.size,
            tapActionType = if (type == ElementType.BUTTON || type == ElementType.INTERACTIVE_ZONE) TapActionType.PLAY_ANIMATION else TapActionType.NONE
        )

        _workspace.update { state ->
            state.copy(
                elements = state.elements + newEl,
                selectedElementId = newEl.id,
                undoDepth = undoStack.size,
                redoDepth = redoStack.size
            )
        }
        syncVisualToCode()
        autoSaveIfEnabled()
    }

    fun updateSelectedElement(transform: (WidgetElement) -> WidgetElement) {
        val targetId = _workspace.value.selectedElementId ?: return
        val currentEl = _workspace.value.elements.firstOrNull { it.id == targetId } ?: return
        if (currentEl.locked) {
            showStatus("Елемент '${currentEl.name}' заблоковано від редагування")
            return
        }
        pushUndoSnapshot()
        _workspace.update { state ->
            state.copy(
                elements = state.elements.map { if (it.id == targetId) transform(it) else it },
                undoDepth = undoStack.size,
                redoDepth = redoStack.size
            )
        }
        syncVisualToCode()
        autoSaveIfEnabled()
    }

    fun moveElementByDrag(elementId: String, dx: Float, dy: Float) {
        val snap = settings.value.snapToGrid
        _workspace.update { state ->
            val target = state.elements.firstOrNull { it.id == elementId } ?: return@update state
            if (target.locked) return@update state
            val groupMembers = if (target.groupId.isNotBlank()) {
                state.elements.filter { it.groupId == target.groupId }.map { it.id }.toSet()
            } else {
                setOf(elementId)
            }
            state.copy(
                selectedElementId = elementId,
                elements = state.elements.map { el ->
                    if (el.id in groupMembers) {
                        val rawX = (el.x + dx).coerceIn(0f, 280f)
                        val rawY = (el.y + dy).coerceIn(0f, 280f)
                        val finalX = if (snap) (rawX / 8f).roundToInt() * 8f else rawX
                        val finalY = if (snap) (rawY / 8f).roundToInt() * 8f else rawY
                        el.copy(x = finalX, y = finalY)
                    } else el
                }
            )
        }
    }

    fun alignSelectedElement(alignment: String) {
        updateSelectedElement { el ->
            when (alignment) {
                "LEFT" -> el.copy(x = 16f)
                "CENTER_H" -> el.copy(x = ((300f - el.width) / 2f).coerceAtLeast(0f))
                "RIGHT" -> el.copy(x = (284f - el.width).coerceAtLeast(0f))
                "TOP" -> el.copy(y = 16f)
                "CENTER_V" -> el.copy(y = ((300f - el.height) / 2f).coerceAtLeast(0f))
                "BOTTOM" -> el.copy(y = (284f - el.height).coerceAtLeast(0f))
                else -> el
            }
        }
    }

    fun reorderSelectedLayer(moveUp: Boolean) {
        val id = _workspace.value.selectedElementId ?: return
        pushUndoSnapshot()
        _workspace.update { state ->
            val sorted = state.elements.sortedBy { it.layerOrder }.toMutableList()
            val idx = sorted.indexOfFirst { it.id == id }
            if (idx < 0) return@update state
            val targetIdx = if (moveUp) (idx + 1).coerceAtMost(sorted.lastIndex) else (idx - 1).coerceAtLeast(0)
            val item = sorted.removeAt(idx)
            sorted.add(targetIdx, item)
            val reindexed = sorted.mapIndexed { i, el -> el.copy(layerOrder = i) }
            state.copy(elements = reindexed, undoDepth = undoStack.size, redoDepth = redoStack.size)
        }
        syncVisualToCode()
    }

    fun copySelectedElement() {
        val selected = _workspace.value.elements.firstOrNull { it.id == _workspace.value.selectedElementId } ?: return
        _workspace.update {
            it.copy(
                clipboardElement = selected,
                statusBannerMessage = "Скопійовано елемент '${selected.name}' у буфер обміну"
            )
        }
    }

    fun pasteClipboardElement() {
        val clip = _workspace.value.clipboardElement ?: return
        pushUndoSnapshot()
        val copyId = "${clip.id}_copy_${_workspace.value.elements.size + 1}"
        val pasted = clip.copy(
            id = copyId,
            name = copyId,
            x = (clip.x + 16f).coerceAtMost(240f),
            y = (clip.y + 16f).coerceAtMost(240f),
            layerOrder = _workspace.value.elements.size
        )
        _workspace.update { state ->
            state.copy(
                elements = state.elements + pasted,
                selectedElementId = pasted.id,
                undoDepth = undoStack.size,
                redoDepth = redoStack.size
            )
        }
        syncVisualToCode()
        autoSaveIfEnabled()
    }

    fun deleteSelectedElement() {
        val id = _workspace.value.selectedElementId ?: return
        pushUndoSnapshot()
        _workspace.update { state ->
            val filtered = state.elements.filterNot { it.id == id }
            state.copy(
                elements = filtered,
                selectedElementId = filtered.firstOrNull()?.id,
                undoDepth = undoStack.size,
                redoDepth = redoStack.size
            )
        }
        syncVisualToCode()
        autoSaveIfEnabled()
    }

    fun toggleLockSelectedElement() {
        val id = _workspace.value.selectedElementId ?: return
        _workspace.update { state ->
            state.copy(
                elements = state.elements.map {
                    if (it.id == id) it.copy(locked = !it.locked) else it
                }
            )
        }
    }

    fun toggleGroupSelectedElement(groupName: String = "group_main") {
        val id = _workspace.value.selectedElementId ?: return
        _workspace.update { state ->
            state.copy(
                elements = state.elements.map {
                    if (it.id == id) {
                        val nextGroup = if (it.groupId.isBlank()) groupName else ""
                        it.copy(groupId = nextGroup)
                    } else it
                }
            )
        }
    }

    fun setWidgetGridSize(size: WidgetGridSize) {
        _workspace.update { it.copy(gridSize = size) }
        syncVisualToCode()
        autoSaveIfEnabled()
    }

    fun setWidgetBackgroundAndRadius(bgHex: String, radius: Float) {
        _workspace.update {
            it.copy(
                backgroundColor = bgHex,
                cornerRadius = radius.coerceIn(0f, 64f)
            )
        }
        syncVisualToCode()
        autoSaveIfEnabled()
    }

    fun setCanvasZoom(zoom: Float) {
        _workspace.update { it.copy(canvasZoom = zoom.coerceIn(0.6f, 1.8f)) }
    }

    fun syncVisualToCode() {
        val st = _workspace.value
        val engine = createScriptEngine()
        val generated = engine.generateScriptFromVisualState(
            title = st.projectName,
            gridSize = st.gridSize,
            backgroundColor = st.backgroundColor,
            cornerRadius = st.cornerRadius,
            elements = st.elements
        )
        val updatedFiles = st.files.map { f ->
            if (f.isEntry || f.fileName == "main.ws") f.copy(content = generated) else f
        }
        val symbols = engine.extractSymbols(generated)
        _workspace.update {
            it.copy(
                files = updatedFiles,
                symbols = symbols
            )
        }
    }

    // =========================================================================
    // CODE STUDIO & WIDGETSCRIPT ENGINE
    // =========================================================================

    private fun createScriptEngine(): WidgetScriptEngine {
        return WidgetScriptEngine(
            maxInstructions = settings.value.sandboxInstructionLimit
        )
    }

    private fun getEnabledPluginNamespaces(): Set<String> {
        return plugins.value.filter { it.enabled }.map { it.namespace }.toSet()
    }

    fun selectCodeFile(fileName: String) {
        val file = _workspace.value.files.firstOrNull { it.fileName == fileName } ?: return
        val engine = createScriptEngine()
        val symbols = engine.extractSymbols(file.content)
        _workspace.update {
            it.copy(
                activeFileName = fileName,
                symbols = symbols
            )
        }
    }

    fun createNewCodeFile(fileNameRaw: String) {
        val clean = fileNameRaw.trim().ifEmpty { "module_${_workspace.value.files.size + 1}.ws" }
        val finalName = if (clean.contains(".")) clean else "$clean.ws"
        if (_workspace.value.files.any { it.fileName.equals(finalName, ignoreCase = true) }) {
            showStatus("Файл '$finalName' вже існує у проєкті")
            return
        }
        val newFile = ProjectFile(
            fileName = finalName,
            language = if (finalName.endsWith(".json")) "json" else "widgetscript",
            content = "// Модуль $finalName\nfn helper() {\n    debug.log(\"Викликано $finalName\")\n}\n",
            isEntry = false
        )
        _workspace.update {
            it.copy(
                files = it.files + newFile,
                activeFileName = finalName
            )
        }
        autoSaveIfEnabled()
    }

    fun updateActiveFileContent(newContent: String) {
        val activeName = _workspace.value.activeFileName
        val engine = createScriptEngine()
        val symbols = engine.extractSymbols(newContent)
        val exec = engine.execute(
            script = newContent,
            fileName = activeName,
            telemetry = _workspace.value.telemetry,
            localStorage = mutableStorage,
            triggerTapEvent = false,
            registeredPluginNamespaces = getEnabledPluginNamespaces()
        )
        _workspace.update { state ->
            val updatedFiles = state.files.map {
                if (it.fileName == activeName) it.copy(content = newContent) else it
            }
            state.copy(
                files = updatedFiles,
                symbols = symbols,
                diagnostics = exec.diagnostics,
                lastExecutionTimeMs = exec.executionTimeMs,
                lastInstructionCount = exec.instructionsExecuted,
                elements = if (activeName == "main.ws" && exec.elements.isNotEmpty() && !exec.haltedBySandbox) exec.elements else state.elements,
                backgroundColor = if (activeName == "main.ws" && !exec.haltedBySandbox) exec.backgroundColor else state.backgroundColor,
                cornerRadius = if (activeName == "main.ws" && !exec.haltedBySandbox) exec.cornerRadius else state.cornerRadius,
                gridSize = if (activeName == "main.ws" && !exec.haltedBySandbox) exec.gridSize else state.gridSize
            )
        }
    }

    fun formatActiveFile() {
        val st = _workspace.value
        val active = st.files.firstOrNull { it.fileName == st.activeFileName } ?: return
        val formatted = createScriptEngine().formatCode(active.content)
        updateActiveFileContent(formatted)
        showStatus("Код відформатовано")
    }

    fun searchAndReplaceInActiveFile(findStr: String, replaceStr: String): Int {
        if (findStr.isEmpty()) return 0
        val st = _workspace.value
        val active = st.files.firstOrNull { it.fileName == st.activeFileName } ?: return 0
        val occurrences = active.content.windowed(findStr.length).count { it == findStr }
        if (occurrences > 0) {
            val updated = active.content.replace(findStr, replaceStr)
            updateActiveFileContent(updated)
            showStatus("Замінено $occurrences входжень '$findStr' -> '$replaceStr'")
        } else {
            showStatus("Символ або текст '$findStr' не знайдено")
        }
        return occurrences
    }

    fun toggleSplitPreview() {
        _workspace.update { it.copy(splitPreviewEnabled = !it.splitPreviewEnabled) }
    }

    fun runActiveScript(triggerTap: Boolean = false) {
        refreshTelemetry()
        val st = _workspace.value
        val active = st.files.firstOrNull { it.fileName == st.activeFileName } ?: return
        val engine = createScriptEngine()
        val res = engine.execute(
            script = active.content,
            fileName = active.fileName,
            telemetry = _workspace.value.telemetry,
            localStorage = mutableStorage,
            triggerTapEvent = triggerTap,
            registeredPluginNamespaces = getEnabledPluginNamespaces()
        )
        val header = "[EXEC ${res.executionTimeMs}ms | ${res.instructionsExecuted} instr] Файл: ${active.fileName}"
        _workspace.update { state ->
            state.copy(
                elements = if (res.elements.isNotEmpty() && !res.haltedBySandbox) res.elements else state.elements,
                diagnostics = res.diagnostics,
                consoleOutput = (state.consoleOutput + header + res.consoleOutput).takeLast(60),
                lastExecutionTimeMs = res.executionTimeMs,
                lastInstructionCount = res.instructionsExecuted,
                localStorage = mutableStorage.toMap()
            )
        }
        if (res.haltedBySandbox) {
            viewModelScope.launch {
                repository.logEvent("SANDBOX", "WidgetScript", "Виконання '${active.fileName}' зупинено Sandbox.")
            }
        }
        autoSaveIfEnabled()
    }

    // =========================================================================
    // ANIMATION STUDIO
    // =========================================================================

    fun selectAnimationTrack(trackId: String) {
        _workspace.update { it.copy(selectedTrackId = trackId) }
    }

    fun toggleAnimationPlayback() {
        _workspace.update { it.copy(isAnimationPlaying = !it.isAnimationPlaying) }
    }

    fun seekAnimationTimeline(timeMs: Long) {
        _workspace.update { it.copy(timelinePositionMs = timeMs.coerceAtLeast(0L)) }
    }

    fun applyAnimationPreset(preset: AnimationTrack) {
        val targetElement = _workspace.value.selectedElementId
            ?: _workspace.value.elements.firstOrNull()?.id
            ?: "clock"
        val configured = preset.copy(
            id = "track_${System.currentTimeMillis() % 10000}",
            targetElementId = targetElement
        )
        _workspace.update { state ->
            val updated = state.animations + configured
            state.copy(
                animations = updated,
                selectedTrackId = configured.id,
                statusBannerMessage = "Додано анімаційну доріжку '${configured.name}' для '$targetElement'"
            )
        }
        autoSaveIfEnabled()
    }

    fun updateSelectedAnimationTrack(transform: (AnimationTrack) -> AnimationTrack) {
        val id = _workspace.value.selectedTrackId ?: return
        _workspace.update { state ->
            state.copy(
                animations = state.animations.map { if (it.id == id) transform(it) else it }
            )
        }
        autoSaveIfEnabled()
    }

    fun addKeyframeToSelectedTrack(timeMs: Int, value: Float, easing: EasingCurve) {
        updateSelectedAnimationTrack { track ->
            val updatedKfs = (track.keyframes.filterNot { it.timeMs == timeMs } + Keyframe(timeMs, value, easing))
                .sortedBy { it.timeMs }
            track.copy(keyframes = updatedKfs, durationMs = maxOf(track.durationMs, timeMs))
        }
    }

    fun deleteAnimationTrack(trackId: String) {
        _workspace.update { state ->
            val filtered = state.animations.filterNot { it.id == trackId }
            state.copy(
                animations = filtered,
                selectedTrackId = filtered.firstOrNull()?.id
            )
        }
        autoSaveIfEnabled()
    }

    // =========================================================================
    // MINI GAME STUDIO
    // =========================================================================

    fun selectGameTemplate(templateType: GameTemplateType) {
        val newCfg = _workspace.value.gameConfig.copy(templateType = templateType)
        _workspace.update {
            it.copy(
                gameConfig = newCfg,
                gameRuntime = gameEngine.createInitialState(newCfg)
            )
        }
        autoSaveIfEnabled()
    }

    fun updateGameConfig(transform: (MiniGameConfig) -> MiniGameConfig) {
        _workspace.update { state ->
            val updated = transform(state.gameConfig)
            state.copy(gameConfig = updated)
        }
        autoSaveIfEnabled()
    }

    fun startOrResetMiniGame() {
        val cfg = _workspace.value.gameConfig
        val fresh = gameEngine.createInitialState(cfg).copy(
            isRunning = true,
            statusBanner = "Гра триває! Керуйте дотиком по арені."
        )
        _workspace.update { it.copy(gameRuntime = fresh) }
    }

    fun togglePauseMiniGame() {
        _workspace.update { state ->
            val rt = state.gameRuntime
            state.copy(
                gameRuntime = rt.copy(
                    isRunning = !rt.isRunning,
                    statusBanner = if (!rt.isRunning) "Продовжено" else "Пауза"
                )
            )
        }
    }

    fun stepMiniGameFrame(dtMs: Long) {
        val st = _workspace.value
        if (!st.gameRuntime.isRunning) return
        val nextRt = gameEngine.step(st.gameRuntime, st.gameConfig, dtMs)
        val newHighScore = maxOf(st.gameConfig.highScore, nextRt.highScore)
        mutableStorage["game_score"] = newHighScore
        _workspace.update { state ->
            state.copy(
                gameRuntime = nextRt,
                gameConfig = state.gameConfig.copy(
                    highScore = newHighScore,
                    lastScore = nextRt.score,
                    currentLevel = nextRt.level
                ),
                localStorage = mutableStorage.toMap()
            )
        }
    }

    fun onMiniGamePointerInput(arenaX: Float, arenaY: Float, isTap: Boolean) {
        _workspace.update { state ->
            val clampedX = arenaX.coerceIn(24f, 276f)
            val clampedY = arenaY.coerceIn(24f, 276f)
            var rt = state.gameRuntime
            if (state.gameConfig.templateType != GameTemplateType.ORBIT_DEFENDER) {
                rt = rt.copy(playerX = clampedX)
            }
            if (isTap) {
                rt = gameEngine.onTapOrAction(rt, state.gameConfig)
            }
            state.copy(gameRuntime = rt)
        }
    }

    // =========================================================================
    // PLUGINS & SETTINGS
    // =========================================================================

    fun togglePlugin(pluginId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.togglePlugin(pluginId, enabled)
        }
    }

    fun installCustomPlugin(manifestJson: String) {
        viewModelScope.launch {
            val result = repository.importCustomPluginManifest(manifestJson)
            if (result.isValid) {
                showStatus("Плагін '${result.plugin?.name}' успішно перевірено та встановлено!")
            } else {
                showStatus("Помилка маніфесту: ${result.errorReason}")
            }
        }
    }

    fun removeCustomPlugin(pluginId: String) {
        viewModelScope.launch {
            repository.removeCustomPlugin(pluginId)
            showStatus("Користувацький плагін видалено")
        }
    }

    fun clearSystemLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            showStatus("Журнал подій очищено")
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsPrefs.setDarkTheme(enabled) }
    }

    fun setCodeFontSize(sizeSp: Int) {
        viewModelScope.launch { settingsPrefs.setCodeFontSize(sizeSp) }
    }

    fun setSnapToGrid(enabled: Boolean) {
        viewModelScope.launch { settingsPrefs.setSnapToGrid(enabled) }
    }

    fun setShowAlignmentGuides(enabled: Boolean) {
        viewModelScope.launch { settingsPrefs.setShowGuides(enabled) }
    }

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch { settingsPrefs.setAutoSave(enabled) }
    }

    fun setLowPowerMode(enabled: Boolean) {
        viewModelScope.launch { settingsPrefs.setLowPowerMode(enabled) }
    }

    fun setSandboxLimit(limit: Int) {
        viewModelScope.launch { settingsPrefs.setSandboxLimit(limit) }
    }

    private fun autoSaveIfEnabled() {
        if (settings.value.autoSaveEnabled) {
            saveCurrentProject(createSnapshot = false)
        }
    }
}
