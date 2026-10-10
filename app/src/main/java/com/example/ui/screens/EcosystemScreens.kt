package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.InstalledPluginEntity
import com.example.data.StudioLogEntity
import com.example.data.StudioSettings
import com.example.engine.script.WidgetScriptEngine
import com.example.model.DiagnosticMessage
import com.example.model.WidgetTemplateItem
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateLibraryScreen(
    templates: List<WidgetTemplateItem>,
    onCloneTemplate: (WidgetTemplateItem, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("template_library_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Бібліотека шаблонів та ресурсів", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Кожен шаблон містить редаговані візуальні шари, вихідний код WidgetScript, пресети анімацій та конфігурацію",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(templates, key = { it.id }) { tpl ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("template_card_${tpl.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tpl.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${tpl.category} • Розмір ${tpl.gridSize.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${tpl.elements.size} шарів",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tpl.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tpl.requiredApi.forEach { api ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = api,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onCloneTemplate(tpl, "${tpl.title} (Копія)") },
                        modifier = Modifier.fillMaxWidth().testTag("use_template_${tpl.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Створити проєкт із цього шаблону")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PluginsScreen(
    plugins: List<InstalledPluginEntity>,
    onTogglePlugin: (String, Boolean) -> Unit,
    onInstallCustomPlugin: (String) -> Unit,
    onRemoveCustomPlugin: (String) -> Unit
) {
    var showInstallDialog by remember { mutableStateOf(false) }
    var customManifestJson by remember {
        mutableStateOf(
            """
            {
              "id": "plugin.custom.solar_flux",
              "name": "Solar & Geomagnetic Index",
              "version": "1.0.0",
              "apiVersion": "1.0",
              "author": "Indie Astro Lab",
              "namespace": "solar",
              "description": "Додає безпечні функції геомагнітного індексу solar.kpIndex()",
              "functions": ["solar.kpIndex()", "solar.flareAlert()"],
              "permissions": ["SAFE_NETWORK_CACHE"]
            }
            """.trimIndent()
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("plugins_screen"),
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
                    Text("Плагіни та розширення", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Ізольовані розширення з перевіркою маніфесту, версії API 1.0 та прав доступу",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showInstallDialog = true },
                    modifier = Modifier.testTag("install_plugin_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Імпорт")
                }
            }
        }

        items(plugins, key = { it.pluginId }) { plugin ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
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
                                    text = plugin.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (plugin.signatureVerified) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = "Підпис перевірено",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "ID: ${plugin.pluginId} • v${plugin.version} (API ${plugin.apiVersion}) • Модуль: ${plugin.namespace}.*",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = plugin.enabled,
                                onCheckedChange = { chk -> onTogglePlugin(plugin.pluginId, chk) }
                            )
                            if (!plugin.isBuiltIn) {
                                IconButton(onClick = { onRemoveCustomPlugin(plugin.pluginId) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Видалити плагін",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = plugin.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Експортовані функції та дозволи:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        plugin.functionsCsv.split(",").filter { it.isNotBlank() }.forEach { fn ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = fn,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        plugin.permissionsCsv.split(",").filter { it.isNotBlank() }.forEach { perm ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "PERM: $perm",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("Імпорт та перевірка маніфесту плагіна") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Система перевіряє сумісність API (1.0), відсутність конфліктів просторів імен та блокує небезпечні дозволи Android (SMS, CAMERA, CONTACTS).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = customManifestJson,
                        onValueChange = { customManifestJson = it },
                        label = { Text("JSON Маніфест плагіна") },
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth().testTag("plugin_manifest_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onInstallCustomPlugin(customManifestJson)
                        showInstallDialog = false
                    },
                    modifier = Modifier.testTag("confirm_install_plugin_button")
                ) {
                    Text("Перевірити та Встановити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) { Text("Скасувати") }
            }
        )
    }
}

@Composable
fun ApiDocsScreen() {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApis = remember(searchQuery) {
        WidgetScriptEngine.STDLIB_COMPLETIONS.filter { (sig, desc) ->
            searchQuery.isBlank() ||
                sig.contains(searchQuery, ignoreCase = true) ||
                desc.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("api_docs_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Документація API та Архітектура", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Повний довідник мови WidgetScript v1.0, моделі безпеки Sandbox та обмежень Android AppWidgets",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Пошук по функціях та просторах імен (system, ui, storage...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Архітектура та Режими Виконання",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "1. Домашні віджети Android (AppWidgetProvider + RemoteViews): рендеряться через апаратно-безпечний Canvas Bitmap із підтримкою системних подій натискання (PendingIntent), живого рівня батареї, пам'яті та системного часу з дотриманням обмежень енергозбереження ОС Android.\n" +
                            "2. Розширений інтерактивний режим (Studio Runtime & Mini-Game Engine): забезпечує 60 FPS анімації, системи частинок та інтерактивні 2D-ігри безпосередньо у середовищі Widget Studio X.\n" +
                            "3. Модель безпеки Sandbox: кожен сценарій WidgetScript виконується з жорстким бюджетом інструкцій (за замовчуванням 2000 інструкцій/кадр), обмеженням глибини рекурсії (24 рівні), тайм-аутом (80 мс) та повною забороною доступу до shell-команд чи файлової системи.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Text(
                text = "Стандартна бібліотека WidgetScript (${filteredApis.size})",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(filteredApis, key = { it.first }) { (signature, description) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = signature,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ErrorLogScreen(
    logs: List<StudioLogEntity>,
    diagnostics: List<DiagnosticMessage>,
    onClearLogs: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("error_log_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Журнал подій та Діагностика", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Моніторинг Sandbox, помилок компіляції WidgetScript та системних подій",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Text("Очистити")
                }
            }
        }

        if (diagnostics.isNotEmpty()) {
            item {
                Text(
                    text = "Поточна діагностика сценарію (${diagnostics.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(diagnostics) { d ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "[${d.severity}] ${d.fileName}:${d.line} — ${d.message}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (d.suggestion.isNotBlank()) {
                            Text(text = "Рекомендація: ${d.suggestion}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Системний журнал платформи (${logs.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(logs, key = { it.id }) { entry ->
            val timeStr = remember(entry.timestamp) {
                SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
            }
            val levelColor = when (entry.level) {
                "ERROR", "SANDBOX" -> MaterialTheme.colorScheme.error
                "WARN" -> Color(0xFFFFAB00)
                else -> MaterialTheme.colorScheme.primary
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[${entry.level}] ${entry.module}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = levelColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = timeStr,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = entry.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun SettingsScreen(
    settings: StudioSettings,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetCodeFontSize: (Int) -> Unit,
    onSetSnapToGrid: (Boolean) -> Unit,
    onSetShowGuides: (Boolean) -> Unit,
    onSetAutoSave: (Boolean) -> Unit,
    onSetLowPowerMode: (Boolean) -> Unit,
    onSetSandboxLimit: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Налаштування середовища IDE", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Керування темою оформлення, параметрами редактора, енергозбереженням та лімітами безпеки Sandbox",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsToggleRow(
                        title = "Темна тема професійної IDE (Obsidian Dark)",
                        subtitle = "Вимкніть для переходу у світлий режим Daylight Studio",
                        checked = settings.darkTheme,
                        onCheckedChange = onSetDarkTheme,
                        testTag = "toggle_dark_theme"
                    )
                    SettingsToggleRow(
                        title = "Прив'язка до сітки 8dp у Візуальному редакторі",
                        subtitle = "Автоматичне вирівнювання координат елементів під час перетягування",
                        checked = settings.snapToGrid,
                        onCheckedChange = onSetSnapToGrid,
                        testTag = "toggle_snap_grid"
                    )
                    SettingsToggleRow(
                        title = "Показувати направляючі та центральні осі",
                        subtitle = "Відображати допоміжну сітку на робочому полотні",
                        checked = settings.showAlignmentGuides,
                        onCheckedChange = onSetShowGuides,
                        testTag = "toggle_alignment_guides"
                    )
                    SettingsToggleRow(
                        title = "Автоматичне збереження проєкту",
                        subtitle = "Миттєво зберігати зміни у локальну базу даних Room",
                        checked = settings.autoSaveEnabled,
                        onCheckedChange = onSetAutoSave,
                        testTag = "toggle_auto_save"
                    )
                    SettingsToggleRow(
                        title = "Режим енергозбереження анімацій",
                        subtitle = "Зменшувати кількість частинок та частоту кадрів на слабких пристроях",
                        checked = settings.lowPowerAnimationMode,
                        onCheckedChange = onSetLowPowerMode,
                        testTag = "toggle_low_power"
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Розмір шрифту в Code Studio: ${settings.codeFontSizeSp} sp",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = settings.codeFontSizeSp.toFloat(),
                        onValueChange = { onSetCodeFontSize(it.roundToInt()) },
                        valueRange = 11f..20f
                    )

                    Text(
                        text = "Ліміт інструкцій безпеки Sandbox: ${settings.sandboxInstructionLimit} інстр./виклик",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Захищає інтерфейс від зависання при виконанні нескінченних циклів у користувацькому коді",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.sandboxInstructionLimit.toFloat(),
                        onValueChange = { onSetSandboxLimit(it.roundToInt()) },
                        valueRange = 500f..5000f
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
