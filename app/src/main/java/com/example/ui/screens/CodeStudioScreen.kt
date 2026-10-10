package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudioSettings
import com.example.engine.script.WidgetScriptEngine
import com.example.model.DiagnosticSeverity
import com.example.ui.components.InteractiveWidgetCanvas
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.EditorWorkspaceState

@Composable
fun CodeStudioScreen(
    workspace: EditorWorkspaceState,
    settings: StudioSettings,
    onSelectFile: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onUpdateCode: (String) -> Unit,
    onFormatCode: () -> Unit,
    onSearchReplace: (String, String) -> Unit,
    onToggleSplitPreview: () -> Unit,
    onRunScript: (Boolean) -> Unit
) {
    val activeFile = workspace.files.firstOrNull { it.fileName == workspace.activeFileName }
        ?: workspace.files.firstOrNull()
    val codeText = activeFile?.content ?: ""

    var bottomTab by remember { mutableIntStateOf(0) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showFindReplaceDialog by remember { mutableStateOf(false) }
    var autocompleteFilter by remember { mutableStateOf("") }

    val syntaxVisualTransformation = remember {
        WidgetScriptSyntaxHighlighter()
    }

    val suggestions = remember(autocompleteFilter) {
        WidgetScriptEngine().getAutocompleteSuggestions(autocompleteFilter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("code_studio_screen")
    ) {
        // 1. Multi-file Tabs + New File Button
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                workspace.files.forEach { file ->
                    FilterChip(
                        selected = file.fileName == workspace.activeFileName,
                        onClick = { onSelectFile(file.fileName) },
                        label = {
                            Text(
                                text = file.fileName,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        },
                        modifier = Modifier.testTag("file_tab_${file.fileName}")
                    )
                }
                IconButton(
                    onClick = { showNewFileDialog = true },
                    modifier = Modifier.testTag("create_file_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Новий файл проєкту")
                }
            }
        }

        // 2. IDE Action Bar & Symbol Outline Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onRunScript(false) },
                        modifier = Modifier.testTag("run_script_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Виконати")
                    }
                    AssistChip(
                        onClick = { onRunScript(true) },
                        label = { Text("Подія on tap") },
                        leadingIcon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("simulate_tap_button")
                    )
                    AssistChip(
                        onClick = onFormatCode,
                        label = { Text("Форматувати") },
                        leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("format_code_button")
                    )
                    AssistChip(
                        onClick = { showFindReplaceDialog = true },
                        label = { Text("Пошук / Заміна") },
                        leadingIcon = { Icon(Icons.Default.FindReplace, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("search_replace_button")
                    )
                    AssistChip(
                        onClick = onToggleSplitPreview,
                        label = { Text(if (workspace.splitPreviewEnabled) "Сховати прев'ю" else "Split-прев'ю") },
                        leadingIcon = { Icon(Icons.Default.Splitscreen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("split_preview_button")
                    )
                }

                // Symbol Navigation Bar (Jump to definition outline)
                if (workspace.symbols.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Символи:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        workspace.symbols.forEach { sym ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${sym.name} :${sym.line}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Code Editor + Optional Split Live Widget Preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.56f)
                .background(Color(0xFF0A0E17))
        ) {
            val vScroll = rememberScrollState()
            val linesCount = remember(codeText) { codeText.lines().size.coerceAtLeast(1) }
            val errorLines = remember(workspace.diagnostics) {
                workspace.diagnostics.filter { it.severity == DiagnosticSeverity.ERROR }.map { it.line }.toSet()
            }

            // Line Numbers Gutter
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF101624))
                    .verticalScroll(vScroll)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (ln in 1..linesCount) {
                    val hasErr = ln in errorLines
                    Text(
                        text = if (hasErr) "!$ln" else "$ln",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = settings.codeFontSizeSp.sp,
                        lineHeight = (settings.codeFontSizeSp + 6).sp,
                        color = if (hasErr) Color(0xFFFF5252) else Color(0xFF5D7090),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Syntax Highlighted Code Editor
            Box(
                modifier = Modifier
                    .weight(if (workspace.splitPreviewEnabled) 0.58f else 1f)
                    .fillMaxHeight()
                    .verticalScroll(vScroll)
                    .padding(10.dp)
            ) {
                BasicTextField(
                    value = codeText,
                    onValueChange = { updated ->
                        val lastToken = updated.takeLast(24).substringAfterLast(" ").substringAfterLast("\n")
                        autocompleteFilter = lastToken
                        onUpdateCode(updated)
                    },
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = settings.codeFontSizeSp.sp,
                        lineHeight = (settings.codeFontSizeSp + 6).sp,
                        color = Color(0xFFF0F4F8)
                    ),
                    cursorBrush = SolidColor(Color(0xFF00E5FF)),
                    visualTransformation = syntaxVisualTransformation,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_input")
                )
            }

            // Optional Split Live Preview Pane
            if (workspace.splitPreviewEnabled) {
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    InteractiveWidgetCanvas(
                        gridSize = workspace.gridSize,
                        backgroundColorHex = workspace.backgroundColor,
                        cornerRadius = workspace.cornerRadius,
                        elements = workspace.elements,
                        selectedElementId = null,
                        telemetry = workspace.telemetry,
                        showGridAndGuides = false,
                        interactiveEditorMode = false,
                        onWidgetTapPreview = { onRunScript(true) },
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }
            }
        }

        // 4. Autocomplete / API Snippet Strip
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Автодоповнення:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                suggestions.forEach { (snippet, _) ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            onUpdateCode(codeText.trimEnd() + "\n    " + snippet + "\n")
                        }
                    ) {
                        Text(
                            text = snippet,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 5. Diagnostics & Console Panel
        val errorCount = workspace.diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
        val warnCount = workspace.diagnostics.count { it.severity == DiagnosticSeverity.WARNING }
        val bottomTabs = listOf(
            "Діагностика ($errorCount пом., $warnCount поп.)",
            "Консоль (${workspace.lastExecutionTimeMs}мс | ${workspace.lastInstructionCount} інстр.)"
        )
        TabRow(selectedTabIndex = bottomTab) {
            bottomTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = bottomTab == idx,
                    onClick = { bottomTab = idx },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("code_bottom_tab_$idx")
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.32f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (bottomTab == 0) {
                if (workspace.diagnostics.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "✓ Синтаксичних помилок чи порушень Sandbox не виявлено",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(workspace.diagnostics) { diag ->
                            val badgeColor = when (diag.severity) {
                                DiagnosticSeverity.ERROR -> MaterialTheme.colorScheme.error
                                DiagnosticSeverity.WARNING -> Color(0xFFFFAB00)
                                DiagnosticSeverity.INFO -> MaterialTheme.colorScheme.primary
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "[${diag.severity}] ${diag.fileName}:${diag.line} — ${diag.message}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (diag.suggestion.isNotBlank()) {
                                        Text(
                                            text = "Підказка: ${diag.suggestion}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0E17))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(workspace.consoleOutput) { line ->
                        val col = when {
                            line.contains("SANDBOX") || line.contains("ERROR") -> Color(0xFFFF5252)
                            line.contains("WARN") -> Color(0xFFFFAB00)
                            line.contains("EXEC") -> Color(0xFF00E5FF)
                            else -> Color(0xFF00E676)
                        }
                        Text(
                            text = line,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = col
                        )
                    }
                }
            }
        }
    }

    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("helpers.ws") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Новий файл у проєкті") },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Ім'я файлу (.ws або .json)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateFile(newFileName)
                        showNewFileDialog = false
                    }
                ) { Text("Створити") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Скасувати") }
            }
        )
    }

    if (showFindReplaceDialog) {
        var findStr by remember { mutableStateOf("") }
        var replaceStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFindReplaceDialog = false },
            title = { Text("Пошук та заміна / Перейменування символів") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = findStr,
                        onValueChange = { findStr = it },
                        label = { Text("Знайти символ або вираз") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = replaceStr,
                        onValueChange = { replaceStr = it },
                        label = { Text("Замінити на") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSearchReplace(findStr, replaceStr)
                        showFindReplaceDialog = false
                    }
                ) { Text("Замінити все") }
            },
            dismissButton = {
                TextButton(onClick = { showFindReplaceDialog = false }) { Text("Закрити") }
            }
        )
    }
}

/**
 * Real-time non-offset-changing syntax highlighter for WidgetScript.
 */
private class WidgetScriptSyntaxHighlighter : VisualTransformation {
    private val keywordRegex = Regex("\\b(widget|text|shape|rect|container|button|progress|clock|date|battery|metric|icon|line|custom|zone|particles|game_obj|var|const|fn|if|else|for|while|on|tap|tick|init|true|false)\\b")
    private val namespaceRegex = Regex("\\b(system|ui|graphics|animation|input|storage|timer|game|audio|network|debug)\\b")
    private val stringRegex = Regex("\"[^\"]*\"")
    private val numberRegex = Regex("\\b\\d+(\\.\\d+)?(x\\d+)?\\b")
    private val commentRegex = Regex("//.*")

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val annotated = buildAnnotatedString {
            append(raw)
            keywordRegex.findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = Color(0xFF8C62FF), fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
            }
            namespaceRegex.findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.Medium), m.range.first, m.range.last + 1)
            }
            numberRegex.findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = Color(0xFFFFAB00)), m.range.first, m.range.last + 1)
            }
            stringRegex.findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = Color(0xFF00E676)), m.range.first, m.range.last + 1)
            }
            commentRegex.findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = Color(0xFF627597)), m.range.first, m.range.last + 1)
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}
