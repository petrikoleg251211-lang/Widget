package com.example.engine.script

import com.example.model.DiagnosticMessage
import com.example.model.DiagnosticSeverity
import com.example.model.ElementType
import com.example.model.TapActionType
import com.example.model.WidgetElement
import com.example.model.WidgetGridSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class SystemTelemetrySnapshot(
    val batteryLevel: Int = 86,
    val isCharging: Boolean = true,
    val freeMemoryMb: Int = 3420,
    val cpuLoadPercent: Int = 24,
    val weatherTempC: Int = 21,
    val weatherCondition: String = "Ясно",
    val stepCount: Int = 6420,
    val nextCalendarEvent: String = "14:00 Архітектурне рев'ю",
    val currentTimeMillis: Long = System.currentTimeMillis()
)

data class ScriptExecutionResult(
    val widgetTitle: String,
    val gridSize: WidgetGridSize,
    val backgroundColor: String,
    val cornerRadius: Float,
    val elements: List<WidgetElement>,
    val consoleOutput: List<String>,
    val diagnostics: List<DiagnosticMessage>,
    val triggeredAnimations: List<String>,
    val variables: Map<String, Any>,
    val instructionsExecuted: Int,
    val executionTimeMs: Long,
    val haltedBySandbox: Boolean = false
)

data class ScriptSymbol(
    val name: String,
    val kind: String, // "widget", "element", "var", "const", "fn", "event"
    val line: Int
)

class WidgetScriptEngine(
    private val maxInstructions: Int = 2_000,
    private val maxRecursionDepth: Int = 24,
    private val maxExecutionMs: Long = 80L
) {
    companion object {
        val ALLOWED_NAMESPACES = setOf(
            "ui", "graphics", "animation", "input", "system",
            "storage", "timer", "game", "audio", "network", "debug"
        )
        val FORBIDDEN_TOKENS = setOf(
            "Runtime.getRuntime", "ProcessBuilder", "System.exit",
            "java.io.File", "Class.forName", "exec(", "sh -c", "/system/bin"
        )
        val KEYWORDS = listOf(
            "widget", "text", "rect", "shape", "container", "button", "progress",
            "clock", "battery", "icon", "line", "custom", "zone", "particles", "game_obj",
            "var", "const", "fn", "if", "else", "for", "while", "on", "tap", "tick", "init",
            "size", "background", "cornerRadius", "position", "value", "fontSize", "color",
            "width", "height", "x", "y", "opacity", "rotation", "borderColor", "borderWidth"
        )
        val STDLIB_COMPLETIONS = listOf(
            "system.time(\"HH:mm\")" to "Поточний системний час за форматом",
            "system.date(\"EEE, d MMM\")" to "Поточна системна дата",
            "system.battery.level" to "Рівень заряду батареї (0..100)",
            "system.battery.charging" to "Статус заряджання (true/false)",
            "system.memory.freeMb" to "Вільна оперативна пам'ять (МБ)",
            "system.weather.temp" to "Температура повітря (°C, кешована)",
            "system.weather.condition" to "Стан погоди",
            "system.calendar.next" to "Наступна подія календаря",
            "ui.setText(\"id\", \"value\")" to "Оновити текст елемента за ID",
            "ui.setColor(\"id\", \"#00E5FF\")" to "Змінити колір елемента за ID",
            "ui.setProgress(\"id\", 0.8)" to "Встановити значення прогрес-бару (0..1)",
            "animation.play(\"pulse\")" to "Запустити анімаційну доріжку за назвою",
            "animation.stop(\"pulse\")" to "Зупинити анімаційну доріжку",
            "storage.get(\"counter\", 0)" to "Зчитати локальне значення проєкту",
            "storage.set(\"counter\", 1)" to "Зберегти локальне значення проєкту",
            "storage.increment(\"counter\")" to "Збільшити збережений лічильник на 1",
            "game.score()" to "Поточний рахунок мінігри",
            "game.addScore(10)" to "Додати бали у мінігрі",
            "debug.log(\"message\")" to "Вивести повідомлення в консоль Code Studio",
            "network.fetchCached(\"weather\")" to "Безпечний запит до дозволеного кешу даних"
        )
    }

    fun execute(
        script: String,
        fileName: String = "main.ws",
        telemetry: SystemTelemetrySnapshot = SystemTelemetrySnapshot(),
        localStorage: MutableMap<String, Any> = mutableMapOf(),
        triggerTapEvent: Boolean = false,
        registeredPluginNamespaces: Set<String> = emptySet()
    ): ScriptExecutionResult {
        val startNs = System.nanoTime()
        val diagnostics = mutableListOf<DiagnosticMessage>()
        val console = mutableListOf<String>()
        val triggeredAnimations = mutableListOf<String>()
        val variables = mutableMapOf<String, Any>()
        val functions = mutableMapOf<String, Pair<List<String>, List<Pair<Int, String>>>>()
        val elements = mutableListOf<WidgetElement>()

        // Prepopulate variables from localStorage
        for ((k, v) in localStorage) {
            variables["storage.$k"] = v
        }

        var widgetTitle = "Untitled Widget"
        var gridSize = WidgetGridSize.SIZE_2X2
        var backgroundColor = "#10131C"
        var cornerRadius = 24f
        var instructionCount = 0
        var haltedBySandbox = false

        val lines = script.lines()

        // 1. Security & Bracket Balance Static Pass
        var braceBalance = 0
        lines.forEachIndexed { idx, rawLine ->
            val lineNum = idx + 1
            val stripped = stripComments(rawLine)
            for (forbidden in FORBIDDEN_TOKENS) {
                if (stripped.contains(forbidden)) {
                    diagnostics.add(
                        DiagnosticMessage(
                            fileName = fileName,
                            line = lineNum,
                            severity = DiagnosticSeverity.ERROR,
                            message = "Порушення безпеки Sandbox: заборонений виклик '$forbidden'",
                            suggestion = "Використовуйте безпечні простори імен system.*, storage.* або ui.*"
                        )
                    )
                    haltedBySandbox = true
                }
            }
            // Quote balance check on line
            val quoteCount = stripped.count { it == '"' }
            if (quoteCount % 2 != 0) {
                diagnostics.add(
                    DiagnosticMessage(
                        fileName = fileName,
                        line = lineNum,
                        severity = DiagnosticSeverity.ERROR,
                        message = "Незакритий рядковий літерал (непарна кількість лапок)",
                        suggestion = "Додайте закриваючу лапку \" у кінці рядка"
                    )
                )
            }
            for (ch in stripped) {
                if (ch == '{') braceBalance++
                if (ch == '}') braceBalance--
            }
            if (braceBalance < 0) {
                diagnostics.add(
                    DiagnosticMessage(
                        fileName = fileName,
                        line = lineNum,
                        severity = DiagnosticSeverity.ERROR,
                        message = "Зайва закриваюча дужка '}'",
                        suggestion = "Перевірте парність блоків { ... }"
                    )
                )
                braceBalance = 0
            }
        }

        if (braceBalance > 0) {
            diagnostics.add(
                DiagnosticMessage(
                    fileName = fileName,
                    line = max(1, lines.size),
                    severity = DiagnosticSeverity.ERROR,
                    message = "Не закрито $braceBalance блок(ів) коду — очікується '}'",
                    suggestion = "Додайте '}' наприкінці опису віджета або функції"
                )
            )
        }

        if (haltedBySandbox) {
            val elapsed = (System.nanoTime() - startNs) / 1_000_000
            return ScriptExecutionResult(
                widgetTitle = widgetTitle,
                gridSize = gridSize,
                backgroundColor = backgroundColor,
                cornerRadius = cornerRadius,
                elements = emptyList(),
                consoleOutput = listOf("[SANDBOX BLOCKED] Виконання зупинено через порушення політики безпеки."),
                diagnostics = diagnostics,
                triggeredAnimations = emptyList(),
                variables = variables,
                instructionsExecuted = instructionCount,
                executionTimeMs = elapsed,
                haltedBySandbox = true
            )
        }

        fun checkSandboxBudget(lineNum: Int): Boolean {
            instructionCount++
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
            if (instructionCount > maxInstructions) {
                if (!haltedBySandbox) {
                    haltedBySandbox = true
                    diagnostics.add(
                        DiagnosticMessage(
                            fileName = fileName,
                            line = lineNum,
                            severity = DiagnosticSeverity.ERROR,
                            message = "Перевищено ліміт інструкцій Sandbox ($maxInstructions). Ймовірний нескінченний цикл.",
                            suggestion = "Зменшіть кількість ітерацій циклу або рекурсивних викликів"
                        )
                    )
                    console.add("[SANDBOX HALT] Перевищено ліміт інструкцій ($maxInstructions) на рядку $lineNum")
                }
                return false
            }
            if (elapsedMs > maxExecutionMs) {
                if (!haltedBySandbox) {
                    haltedBySandbox = true
                    diagnostics.add(
                        DiagnosticMessage(
                            fileName = fileName,
                            line = lineNum,
                            severity = DiagnosticSeverity.ERROR,
                            message = "Перевищено ліміт часу виконання (${maxExecutionMs}мс) на рядку $lineNum.",
                            suggestion = "Оптимізуйте обчислення у сценарії"
                        )
                    )
                }
                return false
            }
            return true
        }

        // Expression Evaluator
        fun evaluateExpression(exprRaw: String, lineNum: Int): Any {
            if (!checkSandboxBudget(lineNum)) return ""
            val expr = exprRaw.trim()
            if (expr.isEmpty()) return ""

            // Handle string concatenation or arithmetic with '+' at top level (outside quotes/parens)
            val plusParts = splitTopLevel(expr, '+')
            if (plusParts.size > 1) {
                val evaluatedParts = plusParts.map { evaluateExpression(it, lineNum) }
                val hasString = evaluatedParts.any { it is String }
                return if (hasString) {
                    evaluatedParts.joinToString("") { formatValue(it) }
                } else {
                    evaluatedParts.sumOf { (it as? Number)?.toDouble() ?: 0.0 }
                }
            }

            // Subtraction / Multiplication / Division (top level)
            val minusParts = splitTopLevel(expr, '-')
            if (minusParts.size > 1 && minusParts.all { it.isNotBlank() }) {
                var acc = (evaluateExpression(minusParts[0], lineNum) as? Number)?.toDouble() ?: 0.0
                for (idx in 1 until minusParts.size) {
                    acc -= (evaluateExpression(minusParts[idx], lineNum) as? Number)?.toDouble() ?: 0.0
                }
                return acc
            }

            val mulParts = splitTopLevel(expr, '*')
            if (mulParts.size > 1) {
                return mulParts.fold(1.0) { acc, part ->
                    acc * ((evaluateExpression(part, lineNum) as? Number)?.toDouble() ?: 1.0)
                }
            }

            // String literal
            if (expr.startsWith("\"") && expr.endsWith("\"") && expr.length >= 2) {
                return expr.substring(1, expr.length - 1)
            }

            // Booleans
            if (expr == "true") return true
            if (expr == "false") return false

            // Numeric literal
            expr.toDoubleOrNull()?.let { num ->
                return if (num % 1.0 == 0.0) num.toInt() else num
            }

            // Grid size literal like 2x2, 4x2, 4x4
            if (expr.matches(Regex("\\d+x\\d+"))) {
                return expr
            }

            // Position keywords
            if (expr in setOf("center", "topLeft", "topRight", "bottomLeft", "bottomRight", "topCenter", "bottomCenter")) {
                return expr
            }

            // System calls & properties
            if (expr.startsWith("system.time(") && expr.endsWith(")")) {
                val pattern = expr.removePrefix("system.time(").removeSuffix(")").trim().removeSurrounding("\"")
                    .ifBlank { "HH:mm" }
                return runCatching {
                    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(telemetry.currentTimeMillis))
                }.getOrDefault("12:45")
            }
            if (expr.startsWith("system.date(") && expr.endsWith(")")) {
                val pattern = expr.removePrefix("system.date(").removeSuffix(")").trim().removeSurrounding("\"")
                    .ifBlank { "EEE, d MMM" }
                return runCatching {
                    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(telemetry.currentTimeMillis))
                }.getOrDefault("Пт, 10 Жовт")
            }
            when (expr) {
                "system.battery.level" -> return telemetry.batteryLevel
                "system.battery.charging" -> return telemetry.isCharging
                "system.memory.freeMb" -> return telemetry.freeMemoryMb
                "system.cpu.load" -> return telemetry.cpuLoadPercent
                "system.weather.temp" -> return telemetry.weatherTempC
                "system.weather.condition" -> return telemetry.weatherCondition
                "system.steps" -> return telemetry.stepCount
                "system.calendar.next" -> return telemetry.nextCalendarEvent
                "timer.now()" -> return telemetry.currentTimeMillis
                "game.score()" -> return (localStorage["game_score"] as? Number)?.toInt() ?: 0
            }

            // storage.get("key", default)
            if (expr.startsWith("storage.get(") && expr.endsWith(")")) {
                val args = splitTopLevel(expr.removePrefix("storage.get(").removeSuffix(")"), ',')
                val key = args.getOrNull(0)?.trim()?.removeSurrounding("\"") ?: "default"
                val fallback = args.getOrNull(1)?.let { evaluateExpression(it, lineNum) } ?: 0
                return localStorage[key] ?: fallback
            }

            // network.fetchCached("topic")
            if (expr.startsWith("network.fetchCached(") && expr.endsWith(")")) {
                val topic = expr.removePrefix("network.fetchCached(").removeSuffix(")").trim().removeSurrounding("\"")
                return when (topic.lowercase()) {
                    "weather" -> "${telemetry.weatherTempC}°C • ${telemetry.weatherCondition}"
                    "crypto", "btc" -> "BTC $68,420 (+2.4%)"
                    "quote" -> "Create. Compile. Ship."
                    else -> "Cached[$topic]: OK"
                }
            }

            // Variable lookup
            if (variables.containsKey(expr)) {
                return variables[expr] ?: ""
            }

            // Check unknown namespace call
            if (expr.contains(".")) {
                val ns = expr.substringBefore(".")
                val allAllowed = ALLOWED_NAMESPACES + registeredPluginNamespaces
                if (ns !in allAllowed && !variables.containsKey(expr)) {
                    diagnostics.add(
                        DiagnosticMessage(
                            fileName = fileName,
                            line = lineNum,
                            severity = DiagnosticSeverity.WARNING,
                            message = "Невідомий простір імен або API '$ns' у виразі '$expr'",
                            suggestion = "Доступні модулі: ${allAllowed.joinToString(", ")}"
                        )
                    )
                } else if (ns in registeredPluginNamespaces) {
                    return "Plugin[$expr]"
                }
            }

            return expr.removeSurrounding("\"")
        }

        fun evaluateCondition(condRaw: String, lineNum: Int): Boolean {
            val cond = condRaw.trim().removeSurrounding("(", ")")
            val ops = listOf("==", "!=", "<=", ">=", "<", ">")
            for (op in ops) {
                val idx = cond.indexOf(op)
                if (idx > 0) {
                    val left = evaluateExpression(cond.substring(0, idx), lineNum)
                    val right = evaluateExpression(cond.substring(idx + op.length), lineNum)
                    val lNum = (left as? Number)?.toDouble() ?: left.toString().toDoubleOrNull()
                    val rNum = (right as? Number)?.toDouble() ?: right.toString().toDoubleOrNull()
                    return if (lNum != null && rNum != null) {
                        when (op) {
                            "==" -> lNum == rNum
                            "!=" -> lNum != rNum
                            "<=" -> lNum <= rNum
                            ">=" -> lNum >= rNum
                            "<" -> lNum < rNum
                            ">" -> lNum > rNum
                            else -> false
                        }
                    } else {
                        when (op) {
                            "==" -> left.toString() == right.toString()
                            "!=" -> left.toString() != right.toString()
                            else -> false
                        }
                    }
                }
            }
            return evaluateExpression(cond, lineNum).toString().equals("true", ignoreCase = true)
        }

        fun executeStatementLine(stmt: String, lineNum: Int, depth: Int) {
            if (depth > maxRecursionDepth) {
                diagnostics.add(
                    DiagnosticMessage(
                        fileName = fileName,
                        line = lineNum,
                        severity = DiagnosticSeverity.ERROR,
                        message = "Перевищено максимальну глибину рекурсії ($maxRecursionDepth)",
                        suggestion = "Перевірте умову виходу з функції"
                    )
                )
                haltedBySandbox = true
                return
            }
            if (!checkSandboxBudget(lineNum) || haltedBySandbox) return

            when {
                stmt.startsWith("var ") || stmt.startsWith("const ") -> {
                    val isConst = stmt.startsWith("const ")
                    val body = stmt.removePrefix("var ").removePrefix("const ").trim()
                    val eqIdx = body.indexOf('=')
                    if (eqIdx > 0) {
                        val name = body.substring(0, eqIdx).trim()
                        val expr = body.substring(eqIdx + 1).trim()
                        val value = evaluateExpression(expr, lineNum)
                        variables[name] = value
                        if (isConst) variables["__const_$name"] = true
                    } else {
                        diagnostics.add(
                            DiagnosticMessage(
                                fileName = fileName,
                                line = lineNum,
                                severity = DiagnosticSeverity.ERROR,
                                message = "Синтаксична помилка оголошення змінної: відсутній знак '='",
                                suggestion = "Приклад: var counter = 0"
                            )
                        )
                    }
                }
                stmt.startsWith("debug.log(") && stmt.endsWith(")") -> {
                    val arg = stmt.removePrefix("debug.log(").removeSuffix(")")
                    val msg = formatValue(evaluateExpression(arg, lineNum))
                    console.add("[LOG:$lineNum] $msg")
                }
                stmt.startsWith("debug.warn(") && stmt.endsWith(")") -> {
                    val arg = stmt.removePrefix("debug.warn(").removeSuffix(")")
                    val msg = formatValue(evaluateExpression(arg, lineNum))
                    console.add("[WARN:$lineNum] $msg")
                    diagnostics.add(
                        DiagnosticMessage(
                            fileName = fileName,
                            line = lineNum,
                            severity = DiagnosticSeverity.WARNING,
                            message = msg
                        )
                    )
                }
                stmt.startsWith("animation.play(") && stmt.endsWith(")") -> {
                    val animName = formatValue(
                        evaluateExpression(stmt.removePrefix("animation.play(").removeSuffix(")"), lineNum)
                    )
                    triggeredAnimations.add(animName)
                    console.add("[ANIM] Запущено анімацію '$animName'")
                }
                stmt.startsWith("storage.set(") && stmt.endsWith(")") -> {
                    val args = splitTopLevel(stmt.removePrefix("storage.set(").removeSuffix(")"), ',')
                    if (args.size >= 2) {
                        val key = formatValue(evaluateExpression(args[0], lineNum))
                        val value = evaluateExpression(args[1], lineNum)
                        localStorage[key] = value
                        variables["storage.$key"] = value
                        console.add("[STORAGE] $key = ${formatValue(value)}")
                    }
                }
                stmt.startsWith("storage.increment(") && stmt.endsWith(")") -> {
                    val key = formatValue(
                        evaluateExpression(stmt.removePrefix("storage.increment(").removeSuffix(")"), lineNum)
                    )
                    val current = (localStorage[key] as? Number)?.toInt() ?: 0
                    val next = current + 1
                    localStorage[key] = next
                    variables["storage.$key"] = next
                    console.add("[STORAGE] $key збільшено до $next")
                }
                stmt.startsWith("ui.setText(") && stmt.endsWith(")") -> {
                    val args = splitTopLevel(stmt.removePrefix("ui.setText(").removeSuffix(")"), ',')
                    if (args.size >= 2) {
                        val targetId = formatValue(evaluateExpression(args[0], lineNum))
                        val newText = formatValue(evaluateExpression(args[1], lineNum))
                        val idx = elements.indexOfFirst { it.id == targetId || it.name == targetId }
                        if (idx >= 0) {
                            elements[idx] = elements[idx].copy(textValue = newText)
                        }
                    }
                }
                stmt.startsWith("ui.setColor(") && stmt.endsWith(")") -> {
                    val args = splitTopLevel(stmt.removePrefix("ui.setColor(").removeSuffix(")"), ',')
                    if (args.size >= 2) {
                        val targetId = formatValue(evaluateExpression(args[0], lineNum))
                        val newColor = formatValue(evaluateExpression(args[1], lineNum))
                        val idx = elements.indexOfFirst { it.id == targetId || it.name == targetId }
                        if (idx >= 0) {
                            elements[idx] = elements[idx].copy(
                                textColor = newColor,
                                backgroundColor = if (elements[idx].type == ElementType.SHAPE || elements[idx].type == ElementType.CONTAINER) newColor else elements[idx].backgroundColor
                            )
                        }
                    }
                }
                stmt.startsWith("ui.setProgress(") && stmt.endsWith(")") -> {
                    val args = splitTopLevel(stmt.removePrefix("ui.setProgress(").removeSuffix(")"), ',')
                    if (args.size >= 2) {
                        val targetId = formatValue(evaluateExpression(args[0], lineNum))
                        val prog = (evaluateExpression(args[1], lineNum) as? Number)?.toFloat() ?: 0.5f
                        val idx = elements.indexOfFirst { it.id == targetId || it.name == targetId }
                        if (idx >= 0) {
                            elements[idx] = elements[idx].copy(progressValue = prog.coerceIn(0f, 1f))
                        }
                    }
                }
                stmt.startsWith("game.addScore(") && stmt.endsWith(")") -> {
                    val pts = (evaluateExpression(stmt.removePrefix("game.addScore(").removeSuffix(")"), lineNum) as? Number)?.toInt() ?: 10
                    val cur = (localStorage["game_score"] as? Number)?.toInt() ?: 0
                    localStorage["game_score"] = cur + pts
                    console.add("[GAME] Рахунок +$pts (Всього: ${cur + pts})")
                }
                stmt.contains("=") && !stmt.contains(":") && !stmt.startsWith("if") && !stmt.startsWith("for") -> {
                    val eqIdx = stmt.indexOf('=')
                    val varName = stmt.substring(0, eqIdx).trim()
                    if (variables["__const_$varName"] == true) {
                        diagnostics.add(
                            DiagnosticMessage(
                                fileName = fileName,
                                line = lineNum,
                                severity = DiagnosticSeverity.ERROR,
                                message = "Спроба змінити константу '$varName'",
                                suggestion = "Використовуйте 'var' замість 'const', якщо значення змінюється"
                            )
                        )
                    } else if (varName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))) {
                        variables[varName] = evaluateExpression(stmt.substring(eqIdx + 1), lineNum)
                    }
                }
                stmt.endsWith(")") && stmt.contains("(") -> {
                    val fnName = stmt.substringBefore("(").trim()
                    val fnDef = functions[fnName]
                    if (fnDef != null) {
                        val argExprs = splitTopLevel(stmt.substringAfter("(").removeSuffix(")"), ',')
                            .filter { it.isNotBlank() }
                        val (paramNames, bodyLines) = fnDef
                        paramNames.forEachIndexed { idx, pName ->
                            val argVal = argExprs.getOrNull(idx)?.let { evaluateExpression(it, lineNum) } ?: ""
                            variables[pName] = argVal
                        }
                        for ((bLine, bStmt) in bodyLines) {
                            executeStatementLine(bStmt, bLine, depth + 1)
                        }
                    }
                }
            }
        }

        // 2. Main Block & Component Parser / Interpreter
        var i = 0
        var elementCounter = 0

        while (i < lines.size && !haltedBySandbox) {
            val lineNum = i + 1
            val line = stripComments(lines[i]).trim()
            if (line.isEmpty() || line == "}") {
                i++
                continue
            }

            // Widget root declaration: widget "Name" {
            if (line.startsWith("widget ")) {
                val titleMatch = Regex("widget\\s+\"([^\"]+)\"").find(line)
                if (titleMatch != null) {
                    widgetTitle = titleMatch.groupValues[1]
                }
                i++
                continue
            }

            // Function declaration: fn name(a, b) {
            if (line.startsWith("fn ")) {
                val fnMatch = Regex("fn\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)\\s*\\{?").find(line)
                if (fnMatch != null) {
                    val fnName = fnMatch.groupValues[1]
                    val params = fnMatch.groupValues[2].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val body = mutableListOf<Pair<Int, String>>()
                    i++
                    var innerDepth = 1
                    while (i < lines.size && innerDepth > 0) {
                        val bLine = stripComments(lines[i]).trim()
                        innerDepth += bLine.count { it == '{' }
                        innerDepth -= bLine.count { it == '}' }
                        if (innerDepth > 0 && bLine.isNotEmpty()) {
                            body.add((i + 1) to bLine)
                        }
                        i++
                    }
                    functions[fnName] = params to body
                    continue
                }
            }

            // Bounded For Loop: for (idx in 0..4) {
            if (line.startsWith("for ") && line.contains("in") && line.contains("..")) {
                val forMatch = Regex("for\\s*\\(?\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s+in\\s+(\\d+)\\.\\.(\\d+)\\s*\\)?\\s*\\{?").find(line)
                if (forMatch != null) {
                    val iterVar = forMatch.groupValues[1]
                    val startVal = forMatch.groupValues[2].toIntOrNull() ?: 0
                    val endVal = forMatch.groupValues[3].toIntOrNull() ?: 0
                    val body = mutableListOf<Pair<Int, String>>()
                    i++
                    var innerDepth = 1
                    while (i < lines.size && innerDepth > 0) {
                        val bLine = stripComments(lines[i]).trim()
                        innerDepth += bLine.count { it == '{' }
                        innerDepth -= bLine.count { it == '}' }
                        if (innerDepth > 0 && bLine.isNotEmpty()) {
                            body.add((i + 1) to bLine)
                        }
                        i++
                    }
                    for (step in startVal..endVal) {
                        if (!checkSandboxBudget(lineNum) || haltedBySandbox) break
                        variables[iterVar] = step
                        for ((bLine, bStmt) in body) {
                            executeStatementLine(bStmt, bLine, 1)
                        }
                    }
                    continue
                }
            }

            // Conditional: if (...) {
            if (line.startsWith("if ") || line.startsWith("if(")) {
                val condStr = line.removePrefix("if").substringBefore("{").trim()
                val condMet = evaluateCondition(condStr, lineNum)
                val ifBody = mutableListOf<Pair<Int, String>>()
                i++
                var innerDepth = 1
                while (i < lines.size && innerDepth > 0) {
                    val bLine = stripComments(lines[i]).trim()
                    innerDepth += bLine.count { it == '{' }
                    innerDepth -= bLine.count { it == '}' }
                    if (innerDepth > 0 && bLine.isNotEmpty()) {
                        ifBody.add((i + 1) to bLine)
                    }
                    i++
                }
                if (condMet) {
                    for ((bLine, bStmt) in ifBody) {
                        executeStatementLine(bStmt, bLine, 1)
                    }
                }
                continue
            }

            // Event block: on tap { ... } or on init { ... }
            if (line.startsWith("on ")) {
                val eventType = line.removePrefix("on ").substringBefore("{").trim()
                val eventBody = mutableListOf<Pair<Int, String>>()
                i++
                var innerDepth = 1
                while (i < lines.size && innerDepth > 0) {
                    val bLine = stripComments(lines[i]).trim()
                    innerDepth += bLine.count { it == '{' }
                    innerDepth -= bLine.count { it == '}' }
                    if (innerDepth > 0 && bLine.isNotEmpty()) {
                        eventBody.add((i + 1) to bLine)
                    }
                    i++
                }
                if (eventType == "init" || eventType == "tick" || (eventType == "tap" && triggerTapEvent)) {
                    for ((bLine, bStmt) in eventBody) {
                        executeStatementLine(bStmt, bLine, 1)
                    }
                }
                continue
            }

            // Component declarations inside widget:
            // text "id" { ... }, shape "id" { ... }, progress "id" { ... }, button "id" { ... }, clock "id" { ... }, battery "id" { ... }
            val compMatch = Regex("^(text|shape|rect|container|button|progress|clock|date|battery|metric|icon|line|custom|zone|particles|game_obj)\\s+\"([^\"]+)\"\\s*\\{").find(line)
            if (compMatch != null) {
                val keyword = compMatch.groupValues[1]
                val compName = compMatch.groupValues[2]
                val elType = when (keyword) {
                    "text" -> ElementType.TEXT
                    "shape", "rect" -> ElementType.SHAPE
                    "container" -> ElementType.CONTAINER
                    "button" -> ElementType.BUTTON
                    "progress" -> ElementType.PROGRESS_BAR
                    "clock" -> ElementType.CLOCK
                    "date" -> ElementType.DATE_CALENDAR
                    "battery" -> ElementType.BATTERY
                    "metric" -> ElementType.SYSTEM_METRIC
                    "icon" -> ElementType.ICON
                    "line" -> ElementType.DIVIDER_LINE
                    "custom" -> ElementType.CUSTOM_GRAPHIC
                    "zone" -> ElementType.INTERACTIVE_ZONE
                    "particles" -> ElementType.ANIMATED_PARTICLE
                    "game_obj" -> ElementType.GAME_OBJECT
                    else -> ElementType.TEXT
                }

                val props = mutableMapOf<String, Any>()
                i++
                var innerDepth = 1
                while (i < lines.size && innerDepth > 0) {
                    val pLine = stripComments(lines[i]).trim()
                    innerDepth += pLine.count { it == '{' }
                    innerDepth -= pLine.count { it == '}' }
                    if (innerDepth > 0 && pLine.contains(":")) {
                        val key = pLine.substringBefore(":").trim()
                        val rawVal = pLine.substringAfter(":").trim()
                        props[key] = evaluateExpression(rawVal, i + 1)
                    }
                    i++
                }

                // Resolve position presets or explicit x/y
                val posPreset = props["position"]?.toString() ?: ""
                val defaultX = when (posPreset) {
                    "center", "topCenter", "bottomCenter" -> 80f
                    "topRight", "bottomRight" -> 165f
                    else -> (props["x"] as? Number)?.toFloat() ?: 24f
                }
                val defaultY = when (posPreset) {
                    "center" -> 115f
                    "bottomLeft", "bottomRight", "bottomCenter" -> 225f
                    "topLeft", "topRight", "topCenter" -> 24f
                    else -> (props["y"] as? Number)?.toFloat() ?: (28f + elementCounter * 52f)
                }

                val valStr = props["value"]?.let { formatValue(it) } ?: when (elType) {
                    ElementType.CLOCK -> formatValue(evaluateExpression("system.time(\"HH:mm\")", lineNum))
                    ElementType.DATE_CALENDAR -> formatValue(evaluateExpression("system.date(\"EEE, d MMM\")", lineNum))
                    ElementType.BATTERY -> "Battery: ${telemetry.batteryLevel}%"
                    else -> compName
                }

                val actionStr = props["onTap"]?.toString() ?: ""
                val tapType = when {
                    actionStr.startsWith("anim:") -> TapActionType.PLAY_ANIMATION
                    actionStr.startsWith("inc:") -> TapActionType.INCREMENT_COUNTER
                    actionStr.startsWith("fn:") -> TapActionType.RUN_SCRIPT_FN
                    actionStr == "game" -> TapActionType.LAUNCH_MINIGAME
                    else -> TapActionType.NONE
                }
                val tapTarget = actionStr.substringAfter(":", "pulse")

                elements.add(
                    WidgetElement(
                        id = compName,
                        name = compName,
                        type = elType,
                        x = (props["x"] as? Number)?.toFloat() ?: defaultX,
                        y = (props["y"] as? Number)?.toFloat() ?: defaultY,
                        width = (props["width"] as? Number)?.toFloat() ?: if (elType == ElementType.PROGRESS_BAR || elType == ElementType.CONTAINER) 240f else 150f,
                        height = (props["height"] as? Number)?.toFloat() ?: if (elType == ElementType.DIVIDER_LINE) 4f else if (elType == ElementType.PROGRESS_BAR) 22f else 50f,
                        rotation = (props["rotation"] as? Number)?.toFloat() ?: 0f,
                        opacity = (props["opacity"] as? Number)?.toFloat()?.coerceIn(0.05f, 1f) ?: 1f,
                        backgroundColor = props["background"]?.toString() ?: props["backgroundColor"]?.toString() ?: "#1E2638",
                        borderColor = props["borderColor"]?.toString() ?: "#00E5FF",
                        borderWidth = (props["borderWidth"] as? Number)?.toFloat() ?: 0f,
                        cornerRadius = (props["cornerRadius"] as? Number)?.toFloat() ?: 14f,
                        textValue = valStr,
                        fontSize = (props["fontSize"] as? Number)?.toFloat() ?: if (elType == ElementType.CLOCK) 40f else 16f,
                        textColor = props["color"]?.toString() ?: props["textColor"]?.toString() ?: "#FFFFFF",
                        progressValue = ((props["progress"] as? Number)?.toFloat() ?: (telemetry.batteryLevel / 100f)).coerceIn(0f, 1f),
                        iconName = props["icon"]?.toString() ?: "bolt",
                        layerOrder = elementCounter++,
                        tapActionType = tapType,
                        tapActionTarget = tapTarget
                    )
                )
                continue
            }

            // Root widget properties (size, background, cornerRadius)
            if (line.contains(":") && !line.startsWith("//")) {
                val propKey = line.substringBefore(":").trim()
                val propVal = evaluateExpression(line.substringAfter(":").trim(), lineNum)
                when (propKey) {
                    "size" -> gridSize = WidgetGridSize.fromCode(propVal.toString())
                    "background" -> backgroundColor = propVal.toString()
                    "cornerRadius" -> cornerRadius = (propVal as? Number)?.toFloat() ?: 24f
                    else -> {
                        if (propKey !in KEYWORDS) {
                            diagnostics.add(
                                DiagnosticMessage(
                                    fileName = fileName,
                                    line = lineNum,
                                    severity = DiagnosticSeverity.WARNING,
                                    message = "Невідома властивість віджета '$propKey'",
                                    suggestion = "Підтримувані властивості кореня: size, background, cornerRadius"
                                )
                            )
                        }
                    }
                }
                i++
                continue
            }

            // Imperative statement at root/widget level
            executeStatementLine(line, lineNum, 0)
            i++
        }

        val elapsedMs = max(1L, (System.nanoTime() - startNs) / 1_000_000)
        return ScriptExecutionResult(
            widgetTitle = widgetTitle,
            gridSize = gridSize,
            backgroundColor = backgroundColor,
            cornerRadius = cornerRadius,
            elements = elements,
            consoleOutput = console,
            diagnostics = diagnostics,
            triggeredAnimations = triggeredAnimations,
            variables = variables.filterKeys { !it.startsWith("__const_") },
            instructionsExecuted = instructionCount,
            executionTimeMs = elapsedMs,
            haltedBySandbox = haltedBySandbox
        )
    }

    /**
     * Generates clean, well-formatted WidgetScript from Visual Editor state so Visual Editor & Code Studio stay in sync.
     */
    fun generateScriptFromVisualState(
        title: String,
        gridSize: WidgetGridSize,
        backgroundColor: String,
        cornerRadius: Float,
        elements: List<WidgetElement>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("// Сгенеровано у Widget Studio X (WidgetScript v1.0)")
        sb.appendLine("widget \"$title\" {")
        sb.appendLine("    size: ${WidgetGridSize.toCode(gridSize)}")
        sb.appendLine("    background: \"$backgroundColor\"")
        sb.appendLine("    cornerRadius: ${cornerRadius.toInt()}")
        sb.appendLine()

        for (el in elements.sortedBy { it.layerOrder }) {
            val kw = when (el.type) {
                ElementType.TEXT -> "text"
                ElementType.SHAPE -> "shape"
                ElementType.CONTAINER -> "container"
                ElementType.BUTTON -> "button"
                ElementType.PROGRESS_BAR -> "progress"
                ElementType.CLOCK -> "clock"
                ElementType.DATE_CALENDAR -> "date"
                ElementType.BATTERY -> "battery"
                ElementType.SYSTEM_METRIC -> "metric"
                ElementType.ICON -> "icon"
                ElementType.DIVIDER_LINE -> "line"
                ElementType.CUSTOM_GRAPHIC -> "custom"
                ElementType.INTERACTIVE_ZONE -> "zone"
                ElementType.ANIMATED_PARTICLE -> "particles"
                ElementType.GAME_OBJECT -> "game_obj"
            }
            sb.appendLine("    $kw \"${el.name}\" {")
            sb.appendLine("        x: ${el.x.toInt()}")
            sb.appendLine("        y: ${el.y.toInt()}")
            sb.appendLine("        width: ${el.width.toInt()}")
            sb.appendLine("        height: ${el.height.toInt()}")
            when (el.type) {
                ElementType.CLOCK -> sb.appendLine("        value: system.time(\"HH:mm\")")
                ElementType.DATE_CALENDAR -> sb.appendLine("        value: system.date(\"EEE, d MMM\")")
                ElementType.BATTERY -> sb.appendLine("        value: \"Battery: \" + system.battery.level + \"%\"")
                else -> if (el.textValue.isNotBlank()) {
                    sb.appendLine("        value: \"${el.textValue.replace("\"", "'")}\"")
                }
            }
            sb.appendLine("        fontSize: ${el.fontSize.toInt()}")
            sb.appendLine("        color: \"${el.textColor}\"")
            sb.appendLine("        background: \"${el.backgroundColor}\"")
            sb.appendLine("        cornerRadius: ${el.cornerRadius.toInt()}")
            if (el.borderWidth > 0f) {
                sb.appendLine("        borderWidth: ${el.borderWidth.toInt()}")
                sb.appendLine("        borderColor: \"${el.borderColor}\"")
            }
            if (el.rotation != 0f) {
                sb.appendLine("        rotation: ${el.rotation.toInt()}")
            }
            if (el.opacity < 0.99f) {
                sb.appendLine("        opacity: ${((el.opacity * 100).toInt() / 100f)}")
            }
            if (el.type == ElementType.PROGRESS_BAR || el.type == ElementType.BATTERY) {
                sb.appendLine("        progress: ${((el.progressValue * 100).toInt() / 100f)}")
            }
            if (el.tapActionType != TapActionType.NONE) {
                val prefix = when (el.tapActionType) {
                    TapActionType.PLAY_ANIMATION -> "anim:${el.tapActionTarget}"
                    TapActionType.INCREMENT_COUNTER -> "inc:${el.tapActionTarget}"
                    TapActionType.RUN_SCRIPT_FN -> "fn:${el.tapActionTarget}"
                    TapActionType.LAUNCH_MINIGAME -> "game"
                    else -> ""
                }
                if (prefix.isNotEmpty()) {
                    sb.appendLine("        onTap: \"$prefix\"")
                }
            }
            sb.appendLine("    }")
            sb.appendLine()
        }

        sb.appendLine("    on tap {")
        sb.appendLine("        animation.play(\"pulse\")")
        sb.appendLine("        debug.log(\"Widget tapped: \" + system.time(\"HH:mm:ss\"))")
        sb.appendLine("    }")
        sb.appendLine("}")
        return sb.toString()
    }

    fun formatCode(rawCode: String): String {
        val result = StringBuilder()
        var indentLevel = 0
        for (rawLine in rawCode.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                result.appendLine()
                continue
            }
            if (trimmed.startsWith("}")) {
                indentLevel = max(0, indentLevel - 1)
            }
            val indent = "    ".repeat(indentLevel)
            result.appendLine("$indent$trimmed")
            if (trimmed.endsWith("{")) {
                indentLevel++
            }
        }
        return result.toString().trimEnd() + "\n"
    }

    fun extractSymbols(code: String): List<ScriptSymbol> {
        val symbols = mutableListOf<ScriptSymbol>()
        code.lines().forEachIndexed { idx, raw ->
            val line = stripComments(raw).trim()
            val lineNum = idx + 1
            Regex("^widget\\s+\"([^\"]+)\"").find(line)?.let {
                symbols.add(ScriptSymbol(it.groupValues[1], "widget", lineNum))
            }
            Regex("^(text|shape|rect|container|button|progress|clock|date|battery|metric|icon|line|custom|zone|particles|game_obj)\\s+\"([^\"]+)\"").find(line)?.let {
                symbols.add(ScriptSymbol("${it.groupValues[1]}:${it.groupValues[2]}", "element", lineNum))
            }
            Regex("^fn\\s+([a-zA-Z_][a-zA-Z0-9_]*)").find(line)?.let {
                symbols.add(ScriptSymbol(it.groupValues[1] + "()", "fn", lineNum))
            }
            Regex("^(var|const)\\s+([a-zA-Z_][a-zA-Z0-9_]*)").find(line)?.let {
                symbols.add(ScriptSymbol(it.groupValues[2], it.groupValues[1], lineNum))
            }
            Regex("^on\\s+([a-zA-Z_]+)").find(line)?.let {
                symbols.add(ScriptSymbol("on ${it.groupValues[1]}", "event", lineNum))
            }
        }
        return symbols
    }

    fun getAutocompleteSuggestions(prefix: String): List<Pair<String, String>> {
        val clean = prefix.trim().lowercase()
        if (clean.isEmpty()) return STDLIB_COMPLETIONS.take(8)
        return STDLIB_COMPLETIONS.filter { (code, desc) ->
            code.lowercase().contains(clean) || desc.lowercase().contains(clean)
        }.ifEmpty { STDLIB_COMPLETIONS.take(6) }
    }

    private fun stripComments(line: String): String {
        var inQuotes = false
        for (i in 0 until line.length - 1) {
            if (line[i] == '"') inQuotes = !inQuotes
            if (!inQuotes && line[i] == '/' && line[i + 1] == '/') {
                return line.substring(0, i)
            }
        }
        return line
    }

    private fun splitTopLevel(input: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var parenDepth = 0
        for (ch in input) {
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }
                !inQuotes && ch == '(' -> {
                    parenDepth++
                    current.append(ch)
                }
                !inQuotes && ch == ')' -> {
                    parenDepth = max(0, parenDepth - 1)
                    current.append(ch)
                }
                !inQuotes && parenDepth == 0 && ch == delimiter -> {
                    parts.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            parts.add(current.toString().trim())
        }
        return parts
    }

    private fun formatValue(v: Any?): String = when (v) {
        null -> ""
        is Double -> if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.2f", v)
        else -> v.toString()
    }
}
