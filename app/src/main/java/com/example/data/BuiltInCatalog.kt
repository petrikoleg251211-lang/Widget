package com.example.data

import com.example.engine.animation.AnimationEngine
import com.example.model.AnimProperty
import com.example.model.AnimationTrack
import com.example.model.EasingCurve
import com.example.model.ElementType
import com.example.model.GameTemplateType
import com.example.model.Keyframe
import com.example.model.MiniGameConfig
import com.example.model.ParticlePreset
import com.example.model.ProjectFile
import com.example.model.StudioPlugin
import com.example.model.TapActionType
import com.example.model.TriggerType
import com.example.model.WidgetElement
import com.example.model.WidgetGridSize
import com.example.model.WidgetTemplateItem
import org.json.JSONObject

data class PluginValidationResult(
    val isValid: Boolean,
    val plugin: StudioPlugin? = null,
    val errorReason: String = ""
)

object BuiltInCatalog {

    const val STUDIO_API_VERSION = "1.0"

    val DANGEROUS_ANDROID_PERMISSIONS = setOf(
        "READ_SMS", "SEND_SMS", "READ_CONTACTS", "RECORD_AUDIO",
        "CAMERA", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE",
        "INSTALL_PACKAGES", "ACCESS_FINE_LOCATION"
    )

    fun getTemplates(): List<WidgetTemplateItem> {
        val systemDashboardElements = listOf(
            WidgetElement(
                id = "card_bg",
                name = "card_bg",
                type = ElementType.CONTAINER,
                x = 16f,
                y = 16f,
                width = 268f,
                height = 268f,
                backgroundColor = "#172033",
                borderColor = "#00E5FF",
                borderWidth = 1.5f,
                cornerRadius = 22f,
                layerOrder = 0
            ),
            WidgetElement(
                id = "clock",
                name = "clock",
                type = ElementType.CLOCK,
                x = 34f,
                y = 32f,
                width = 230f,
                height = 64f,
                textValue = "12:45",
                fontSize = 42f,
                textColor = "#00E5FF",
                backgroundColor = "#00000000",
                layerOrder = 1,
                tapActionType = TapActionType.PLAY_ANIMATION,
                tapActionTarget = "pulse"
            ),
            WidgetElement(
                id = "date_label",
                name = "date_label",
                type = ElementType.DATE_CALENDAR,
                x = 36f,
                y = 98f,
                width = 220f,
                height = 32f,
                textValue = "Субота, 10 Жовт",
                fontSize = 15f,
                textColor = "#9FB0C7",
                backgroundColor = "#00000000",
                layerOrder = 2
            ),
            WidgetElement(
                id = "battery_bar",
                name = "battery_bar",
                type = ElementType.PROGRESS_BAR,
                x = 34f,
                y = 148f,
                width = 232f,
                height = 24f,
                backgroundColor = "#232D42",
                borderColor = "#00E676",
                textColor = "#00E676",
                progressValue = 0.86f,
                cornerRadius = 12f,
                layerOrder = 3
            ),
            WidgetElement(
                id = "battery",
                name = "battery",
                type = ElementType.BATTERY,
                x = 34f,
                y = 180f,
                width = 232f,
                height = 34f,
                textValue = "Battery: 86% • RAM 3.4GB",
                fontSize = 14f,
                textColor = "#FFFFFF",
                backgroundColor = "#00000000",
                layerOrder = 4
            ),
            WidgetElement(
                id = "boost_btn",
                name = "boost_btn",
                type = ElementType.BUTTON,
                x = 34f,
                y = 224f,
                width = 232f,
                height = 42f,
                textValue = "⚡ Імпульс + Лічильник",
                fontSize = 14f,
                textColor = "#00262E",
                backgroundColor = "#00E5FF",
                cornerRadius = 14f,
                layerOrder = 5,
                tapActionType = TapActionType.INCREMENT_COUNTER,
                tapActionTarget = "taps"
            )
        )

        val systemDashboardScript = """
// Системна панель керування (WidgetScript v1.0)
widget "System Dashboard" {
    size: 2x2
    background: "#10131C"
    cornerRadius: 24

    var statusPrefix = "Battery: "
    const MAX_LEVEL = 100

    container "card_bg" {
        x: 16
        y: 16
        width: 268
        height: 268
        background: "#172033"
        borderColor: "#00E5FF"
        borderWidth: 2
        cornerRadius: 22
    }

    clock "clock" {
        x: 34
        y: 32
        width: 230
        height: 64
        value: system.time("HH:mm")
        fontSize: 42
        color: "#00E5FF"
        onTap: "anim:pulse"
    }

    date "date_label" {
        x: 36
        y: 98
        width: 220
        height: 32
        value: system.date("EEE, d MMM")
        fontSize: 15
        color: "#9FB0C7"
    }

    progress "battery_bar" {
        x: 34
        y: 148
        width: 232
        height: 24
        color: "#00E676"
        background: "#232D42"
        progress: 0.86
    }

    battery "battery" {
        x: 34
        y: 180
        width: 232
        height: 34
        value: statusPrefix + system.battery.level + "% • RAM " + system.memory.freeMb + "MB"
        fontSize: 14
        color: "#FFFFFF"
    }

    button "boost_btn" {
        x: 34
        y: 224
        width: 232
        height: 42
        value: "⚡ Імпульс (" + storage.get("taps", 0) + ")"
        fontSize: 14
        color: "#00262E"
        background: "#00E5FF"
        cornerRadius: 14
        onTap: "inc:taps"
    }

    on tap {
        storage.increment("taps")
        animation.play("pulse")
        debug.log("System Dashboard tapped! Battery=" + system.battery.level + "%")
    }
}
""".trimIndent()

        val weatherElements = listOf(
            WidgetElement(
                id = "weather_header",
                name = "weather_header",
                type = ElementType.TEXT,
                x = 24f,
                y = 24f,
                width = 250f,
                height = 38f,
                textValue = "КИЇВ • СОНЯЧНО",
                fontSize = 14f,
                textColor = "#00E5FF",
                backgroundColor = "#00000000",
                layerOrder = 0
            ),
            WidgetElement(
                id = "temp_big",
                name = "temp_big",
                type = ElementType.SYSTEM_METRIC,
                x = 24f,
                y = 62f,
                width = 160f,
                height = 70f,
                textValue = "21°C • Ясно",
                fontSize = 36f,
                textColor = "#FFFFFF",
                backgroundColor = "#00000000",
                layerOrder = 1
            ),
            WidgetElement(
                id = "calendar_card",
                name = "calendar_card",
                type = ElementType.CONTAINER,
                x = 22f,
                y = 144f,
                width = 256f,
                height = 76f,
                backgroundColor = "#1F2942",
                borderColor = "#8C62FF",
                borderWidth = 1.5f,
                cornerRadius = 16f,
                layerOrder = 2
            ),
            WidgetElement(
                id = "next_event",
                name = "next_event",
                type = ElementType.TEXT,
                x = 34f,
                y = 158f,
                width = 232f,
                height = 48f,
                textValue = "📅 14:00 Архітектурне рев'ю",
                fontSize = 14f,
                textColor = "#E5DBFF",
                backgroundColor = "#00000000",
                layerOrder = 3
            ),
            WidgetElement(
                id = "sync_btn",
                name = "sync_btn",
                type = ElementType.BUTTON,
                x = 22f,
                y = 234f,
                width = 256f,
                height = 42f,
                textValue = "Оновити кеш погоди",
                fontSize = 14f,
                textColor = "#FFFFFF",
                backgroundColor = "#8C62FF",
                cornerRadius = 14f,
                layerOrder = 4,
                tapActionType = TapActionType.PLAY_ANIMATION,
                tapActionTarget = "levitate"
            )
        )

        val weatherScript = """
// Погодна та календарна дека (WidgetScript v1.0)
widget "Weather & Agenda Deck" {
    size: 4x2
    background: "#121829"
    cornerRadius: 26

    text "weather_header" {
        x: 24
        y: 24
        value: "КИЇВ • МЕТЕОСТАНЦІЯ"
        fontSize: 14
        color: "#00E5FF"
    }

    metric "temp_big" {
        x: 24
        y: 62
        value: system.weather.temp + "°C • " + system.weather.condition
        fontSize: 34
        color: "#FFFFFF"
    }

    container "calendar_card" {
        x: 22
        y: 144
        width: 256
        height: 76
        background: "#1F2942"
        borderColor: "#8C62FF"
        borderWidth: 2
        cornerRadius: 16
    }

    text "next_event" {
        x: 34
        y: 162
        value: "📅 " + system.calendar.next
        fontSize: 14
        color: "#E5DBFF"
    }

    button "sync_btn" {
        x: 22
        y: 234
        width: 256
        height: 42
        value: "Оновити: " + network.fetchCached("weather")
        fontSize: 13
        color: "#FFFFFF"
        background: "#8C62FF"
        cornerRadius: 14
    }

    on tap {
        animation.play("levitate")
        debug.log("Weather cache verified: " + network.fetchCached("weather"))
    }
}
""".trimIndent()

        val pomodoroElements = listOf(
            WidgetElement(
                id = "focus_title",
                name = "focus_title",
                type = ElementType.TEXT,
                x = 24f,
                y = 24f,
                width = 252f,
                height = 34f,
                textValue = "ФОКУС-ТАЙМЕР • POMODORO",
                fontSize = 14f,
                textColor = "#00E676",
                backgroundColor = "#00000000",
                layerOrder = 0
            ),
            WidgetElement(
                id = "timer_readout",
                name = "timer_readout",
                type = ElementType.CLOCK,
                x = 24f,
                y = 64f,
                width = 252f,
                height = 68f,
                textValue = "25:00",
                fontSize = 44f,
                textColor = "#FFFFFF",
                backgroundColor = "#00000000",
                layerOrder = 1
            ),
            WidgetElement(
                id = "sprint_progress",
                name = "sprint_progress",
                type = ElementType.PROGRESS_BAR,
                x = 24f,
                y = 144f,
                width = 252f,
                height = 24f,
                backgroundColor = "#1E2638",
                textColor = "#00E676",
                progressValue = 0.72f,
                cornerRadius = 12f,
                layerOrder = 2
            ),
            WidgetElement(
                id = "task_button",
                name = "task_button",
                type = ElementType.BUTTON,
                x = 24f,
                y = 196f,
                width = 252f,
                height = 52f,
                textValue = "✓ Завершити спринт (+1)",
                fontSize = 15f,
                textColor = "#002911",
                backgroundColor = "#00E676",
                cornerRadius = 16f,
                layerOrder = 3,
                tapActionType = TapActionType.INCREMENT_COUNTER,
                tapActionTarget = "sprints"
            )
        )

        val pomodoroScript = """
// Продуктивність і Фокус-Спринти (WidgetScript v1.0)
widget "Focus Sprint Tracker" {
    size: 2x2
    background: "#0E171D"
    cornerRadius: 24

    text "focus_title" {
        x: 24
        y: 24
        value: "ФОКУС • СПРИНТ #" + storage.get("sprints", 1)
        fontSize: 14
        color: "#00E676"
    }

    clock "timer_readout" {
        x: 24
        y: 64
        value: system.time("HH:mm")
        fontSize: 44
        color: "#FFFFFF"
    }

    progress "sprint_progress" {
        x: 24
        y: 144
        width: 252
        height: 24
        color: "#00E676"
        background: "#1E2638"
        progress: 0.75
    }

    button "task_button" {
        x: 24
        y: 196
        width: 252
        height: 52
        value: "✓ Спринт виконано (" + storage.get("sprints", 1) + ")"
        fontSize: 15
        color: "#002911"
        background: "#00E676"
        cornerRadius: 16
        onTap: "inc:sprints"
    }

    on tap {
        storage.increment("sprints")
        animation.play("pulse")
    }
}
""".trimIndent()

        val arcadeElements = listOf(
            WidgetElement(
                id = "arcade_frame",
                name = "arcade_frame",
                type = ElementType.CONTAINER,
                x = 16f,
                y = 16f,
                width = 268f,
                height = 268f,
                backgroundColor = "#16122B",
                borderColor = "#8C62FF",
                borderWidth = 2f,
                cornerRadius = 24f,
                layerOrder = 0
            ),
            WidgetElement(
                id = "game_title",
                name = "game_title",
                type = ElementType.TEXT,
                x = 32f,
                y = 32f,
                width = 236f,
                height = 36f,
                textValue = "🚀 ASTRO DODGER 2D",
                fontSize = 18f,
                textColor = "#00E5FF",
                backgroundColor = "#00000000",
                layerOrder = 1
            ),
            WidgetElement(
                id = "ship_sprite",
                name = "ship_sprite",
                type = ElementType.GAME_OBJECT,
                x = 95f,
                y = 95f,
                width = 110f,
                height = 68f,
                textValue = "🛸 SCORE",
                fontSize = 18f,
                textColor = "#FFFFFF",
                backgroundColor = "#2A1B54",
                borderColor = "#00E5FF",
                borderWidth = 1.5f,
                cornerRadius = 18f,
                layerOrder = 2
            ),
            WidgetElement(
                id = "play_game_btn",
                name = "play_game_btn",
                type = ElementType.BUTTON,
                x = 32f,
                y = 212f,
                width = 236f,
                height = 48f,
                textValue = "🎮 Грати у Студії мініігор",
                fontSize = 14f,
                textColor = "#00262E",
                backgroundColor = "#00E5FF",
                cornerRadius = 14f,
                layerOrder = 3,
                tapActionType = TapActionType.LAUNCH_MINIGAME,
                tapActionTarget = "ASTRO_DODGER"
            )
        )

        val arcadeScript = """
// Аркадний віджет мінігри Astro Dodger 2D
widget "Astro Dodger Mini-Game" {
    size: 2x2
    background: "#110D24"
    cornerRadius: 24

    container "arcade_frame" {
        x: 16
        y: 16
        width: 268
        height: 268
        background: "#16122B"
        borderColor: "#8C62FF"
        borderWidth: 2
        cornerRadius: 24
    }

    text "game_title" {
        x: 32
        y: 32
        value: "🚀 ASTRO DODGER 2D"
        fontSize: 18
        color: "#00E5FF"
    }

    game_obj "ship_sprite" {
        x: 42
        y: 94
        width: 216
        height: 82
        value: "Рекорд: " + game.score() + " PTS"
        fontSize: 20
        color: "#00E676"
        background: "#2A1B54"
        cornerRadius: 18
    }

    button "play_game_btn" {
        x: 32
        y: 212
        width: 236
        height: 48
        value: "🎮 +10 Очок / Грати"
        fontSize: 14
        color: "#00262E"
        background: "#00E5FF"
        cornerRadius: 14
        onTap: "game"
    }

    on tap {
        game.addScore(10)
        animation.play("pulse")
    }
}
""".trimIndent()

        return listOf(
            WidgetTemplateItem(
                id = "tpl_system_dashboard",
                title = "System Dashboard Pro",
                category = "Системні панелі та Годинники",
                description = "Кібер-годинник із живим рівнем заряду батареї, обсягом вільної RAM, лічильником натискань і пружинною анімацією.",
                gridSize = WidgetGridSize.SIZE_2X2,
                backgroundColor = "#10131C",
                cornerRadius = 24f,
                requiredApi = listOf("system.time", "system.battery", "storage.*", "animation.play"),
                elements = systemDashboardElements,
                files = listOf(
                    ProjectFile("main.ws", "widgetscript", systemDashboardScript, isEntry = true),
                    ProjectFile(
                        "animations.ws",
                        "widgetscript",
                        "// Пресет ключових кадрів\n// Доріжка 'pulse': SCALE 1.0 -> 1.15 -> 1.0 (Spring)\n"
                    ),
                    ProjectFile(
                        "manifest.json",
                        "json",
                        "{\n  \"schemaVersion\": 1,\n  \"entry\": \"main.ws\",\n  \"permissions\": [\"SYSTEM_METRICS\", \"LOCAL_STORAGE\"]\n}"
                    )
                ),
                animations = listOf(AnimationEngine.getBuiltInPresets("clock").first()),
                gameConfig = MiniGameConfig(templateType = GameTemplateType.ASTRO_DODGER, highScore = 120)
            ),
            WidgetTemplateItem(
                id = "tpl_weather_agenda",
                title = "Weather & Agenda Deck",
                category = "Погода та Календар",
                description = "Широка інформаційна панель 4x2 з кешованим прогнозом погоди, температурою та наступною подією календаря.",
                gridSize = WidgetGridSize.SIZE_4X2,
                backgroundColor = "#121829",
                cornerRadius = 26f,
                requiredApi = listOf("system.weather", "system.calendar", "network.fetchCached"),
                elements = weatherElements,
                files = listOf(
                    ProjectFile("main.ws", "widgetscript", weatherScript, isEntry = true),
                    ProjectFile(
                        "manifest.json",
                        "json",
                        "{\n  \"schemaVersion\": 1,\n  \"entry\": \"main.ws\",\n  \"permissions\": [\"SAFE_NETWORK_CACHE\", \"CALENDAR_SUMMARY\"]\n}"
                    )
                ),
                animations = listOf(AnimationEngine.getBuiltInPresets("temp_big")[1]),
                gameConfig = MiniGameConfig(templateType = GameTemplateType.ORBIT_DEFENDER, highScore = 80)
            ),
            WidgetTemplateItem(
                id = "tpl_focus_pomodoro",
                title = "Focus Sprint & Pomodoro",
                category = "Продуктивність і Навчання",
                description = "Віджет контролю робочих спринтів із прогрес-баром, інтерактивним лічильником завершених сесій та збереженням стану.",
                gridSize = WidgetGridSize.SIZE_2X2,
                backgroundColor = "#0E171D",
                cornerRadius = 24f,
                requiredApi = listOf("system.time", "storage.increment", "ui.setProgress"),
                elements = pomodoroElements,
                files = listOf(
                    ProjectFile("main.ws", "widgetscript", pomodoroScript, isEntry = true)
                ),
                animations = listOf(
                    AnimationTrack(
                        id = "anim_focus_pulse",
                        name = "pulse",
                        targetElementId = "task_button",
                        property = AnimProperty.SCALE,
                        durationMs = 1000,
                        loop = true,
                        reverseOnRepeat = true,
                        trigger = TriggerType.ON_TAP,
                        particlePreset = ParticlePreset.PULSE_RINGS,
                        keyframes = listOf(
                            Keyframe(0, 0f, EasingCurve.EASE_IN_OUT),
                            Keyframe(500, 0.25f, EasingCurve.SPRING),
                            Keyframe(1000, 0f, EasingCurve.EASE_IN_OUT)
                        )
                    )
                ),
                gameConfig = MiniGameConfig(templateType = GameTemplateType.ORBIT_DEFENDER, highScore = 150)
            ),
            WidgetTemplateItem(
                id = "tpl_astro_arcade",
                title = "Astro Dodger 2D Arcade",
                category = "Мініігри та Інтерактив",
                description = "Інтерактивний ігровий віджет зі збереженням рекорду та швидким запуском 2D-аркади з фізикою та колізіями.",
                gridSize = WidgetGridSize.SIZE_2X2,
                backgroundColor = "#110D24",
                cornerRadius = 24f,
                requiredApi = listOf("game.score", "game.addScore", "animation.play"),
                elements = arcadeElements,
                files = listOf(
                    ProjectFile("main.ws", "widgetscript", arcadeScript, isEntry = true),
                    ProjectFile(
                        "game_logic.ws",
                        "widgetscript",
                        "// Конфігурація фізики Astro Dodger\nvar speed = 1.2\nvar targetScore = 150\n"
                    )
                ),
                animations = listOf(AnimationEngine.getBuiltInPresets("ship_sprite").first()),
                gameConfig = MiniGameConfig(
                    templateType = GameTemplateType.ASTRO_DODGER,
                    baseSpeed = 1.15f,
                    targetWinScore = 150,
                    highScore = 180
                )
            )
        )
    }

    fun getBuiltInPlugins(): List<StudioPlugin> = listOf(
        StudioPlugin(
            id = "plugin.weather.openmeteo",
            name = "OpenMeteo Safe Cache Bridge",
            version = "1.2.0",
            apiVersion = "1.0",
            author = "Widget Studio Core Team",
            description = "Надає безпечний кешований доступ до погодних показників без фонового виснаження батареї.",
            providedNamespace = "weather_ext",
            providedFunctions = listOf("weather_ext.uvIndex()", "weather_ext.windKmh()", "weather_ext.humidity()"),
            permissions = listOf("SAFE_NETWORK_CACHE"),
            signatureVerified = true,
            enabled = true,
            isBuiltIn = true
        ),
        StudioPlugin(
            id = "plugin.fx.shaders",
            name = "Neon Shader & Particle Pack",
            version = "1.0.4",
            apiVersion = "1.0",
            author = "Studio FX Labs",
            description = "Додає генератори кібер-частинок, неонового світіння та векторних сіток для Canvas-рендерера.",
            providedNamespace = "fx",
            providedFunctions = listOf("fx.neonGlow(radius)", "fx.scanlines(opacity)", "fx.cyberGrid()"),
            permissions = listOf("CANVAS_EFFECTS"),
            signatureVerified = true,
            enabled = true,
            isBuiltIn = true
        ),
        StudioPlugin(
            id = "plugin.telemetry.battery_guard",
            name = "Battery & Thermal Guard",
            version = "1.1.0",
            apiVersion = "1.0",
            author = "Android Optimization Guild",
            description = "Аналізує рівень заряду та автоматично знижує частоту кадрів анімації в режимі енергозбереження.",
            providedNamespace = "power",
            providedFunctions = listOf("power.isEcoMode()", "power.recommendedFps()"),
            permissions = listOf("SYSTEM_METRICS"),
            signatureVerified = true,
            enabled = true,
            isBuiltIn = true
        )
    )

    fun validatePluginManifestJson(
        manifestJson: String,
        existingNamespaces: Set<String>
    ): PluginValidationResult {
        return try {
            val obj = JSONObject(manifestJson)
            val id = obj.optString("id", "").trim()
            val name = obj.optString("name", "").trim()
            val version = obj.optString("version", "1.0.0").trim()
            val apiVersion = obj.optString("apiVersion", "").trim()
            val namespace = obj.optString("namespace", "").trim()
            val author = obj.optString("author", "External Developer").trim()
            val description = obj.optString("description", "Користувацьке розширення").trim()

            if (id.isEmpty() || name.isEmpty() || namespace.isEmpty()) {
                return PluginValidationResult(false, errorReason = "Маніфест повинен містити обов'язкові поля: id, name, namespace.")
            }
            if (apiVersion != STUDIO_API_VERSION) {
                return PluginValidationResult(
                    false,
                    errorReason = "Несумісна версія API плагіна '$apiVersion'. Підтримується версія '$STUDIO_API_VERSION'."
                )
            }
            if (namespace in existingNamespaces) {
                return PluginValidationResult(
                    false,
                    errorReason = "Конфлікт простору імен: модуль '$namespace' вже зареєстровано іншим плагіном або ядром."
                )
            }

            val permsArr = obj.optJSONArray("permissions")
            val perms = mutableListOf<String>()
            if (permsArr != null) {
                for (i in 0 until permsArr.length()) {
                    val p = permsArr.optString(i, "").trim().uppercase()
                    if (p in DANGEROUS_ANDROID_PERMISSIONS) {
                        return PluginValidationResult(
                            false,
                            errorReason = "Відхилено системою безпеки: плагін запитує небезпечний системний дозвіл '$p'."
                        )
                    }
                    if (p.isNotEmpty()) perms.add(p)
                }
            }

            val fnArr = obj.optJSONArray("functions")
            val fns = mutableListOf<String>()
            if (fnArr != null) {
                for (i in 0 until fnArr.length()) {
                    val f = fnArr.optString(i, "").trim()
                    if (f.isNotEmpty()) fns.add(f)
                }
            } else {
                fns.add("$namespace.run()")
            }

            val plugin = StudioPlugin(
                id = id,
                name = name,
                version = version,
                apiVersion = apiVersion,
                author = author,
                description = description,
                providedNamespace = namespace,
                providedFunctions = fns,
                permissions = perms.ifEmpty { listOf("SANDBOX_ISOLATED") },
                signatureVerified = obj.optBoolean("signatureVerified", true),
                enabled = true,
                isBuiltIn = false
            )
            PluginValidationResult(true, plugin = plugin)
        } catch (e: Exception) {
            PluginValidationResult(false, errorReason = "Некоректний формат JSON маніфесту: ${e.message}")
        }
    }
}
