package com.example.model

import org.json.JSONArray
import org.json.JSONObject

enum class StudioDestination(val route: String, val titleUk: String) {
    DASHBOARD("dashboard", "Головна панель"),
    PROJECTS("projects", "Мої проєкти"),
    VISUAL_EDITOR("visual_editor", "Візуальний редактор"),
    CODE_STUDIO("code_studio", "Редактор коду"),
    ANIMATION_STUDIO("animation_studio", "Анімаційна студія"),
    GAME_STUDIO("game_studio", "Студія мініігор"),
    TEMPLATE_LIBRARY("template_library", "Бібліотека ресурсів"),
    PLUGINS("plugins", "Плагіни"),
    API_DOCS("api_docs", "Документація API"),
    ERROR_LOG("error_log", "Журнал помилок"),
    SETTINGS("settings", "Налаштування")
}

enum class WidgetGridSize(val label: String, val cellsW: Int, val cellsH: Int, val aspect: Float) {
    SIZE_2X2("2x2 (Компактний)", 2, 2, 1.0f),
    SIZE_4X2("4x2 (Широкий)", 4, 2, 2.0f),
    SIZE_4X4("4x4 (Панель)", 4, 4, 1.0f);

    companion object {
        fun fromCode(code: String): WidgetGridSize = when (code.lowercase().trim()) {
            "4x2" -> SIZE_4X2
            "4x4" -> SIZE_4X4
            else -> SIZE_2X2
        }

        fun toCode(size: WidgetGridSize): String = "${size.cellsW}x${size.cellsH}"
    }
}

enum class ElementType(val labelUk: String) {
    TEXT("Текст"),
    SHAPE("Фігура"),
    CONTAINER("Контейнер"),
    BUTTON("Кнопка"),
    PROGRESS_BAR("Прогрес-бар"),
    CLOCK("Годинник"),
    DATE_CALENDAR("Дата / Календар"),
    BATTERY("Батарея"),
    SYSTEM_METRIC("Системний показник"),
    ICON("Іконка / Зображення"),
    DIVIDER_LINE("Лінія"),
    CUSTOM_GRAPHIC("Власна графіка"),
    INTERACTIVE_ZONE("Інтерактивна зона"),
    ANIMATED_PARTICLE("Частинки / Ефект"),
    GAME_OBJECT("Ігровий об'єкт")
}

enum class TapActionType(val labelUk: String) {
    NONE("Без дії"),
    PLAY_ANIMATION("Запустити анімацію"),
    INCREMENT_COUNTER("Збільшити лічильник"),
    RUN_SCRIPT_FN("Виконати функцію WidgetScript"),
    LAUNCH_MINIGAME("Відкрити мінігру"),
    OPEN_STUDIO("Відкрити редактор")
}

data class WidgetElement(
    val id: String,
    val name: String,
    val type: ElementType,
    val x: Float = 20f, // 0..300 virtual canvas coordinates
    val y: Float = 20f, // 0..300 virtual canvas coordinates
    val width: Float = 140f,
    val height: Float = 48f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val backgroundColor: String = "#1E2638",
    val borderColor: String = "#00E5FF",
    val borderWidth: Float = 0f,
    val cornerRadius: Float = 12f,
    val shadowElevation: Float = 0f,
    val textValue: String = "",
    val fontSize: Float = 18f,
    val textColor: String = "#FFFFFF",
    val progressValue: Float = 0.75f,
    val iconName: String = "bolt",
    val layerOrder: Int = 0,
    val groupId: String = "",
    val visible: Boolean = true,
    val locked: Boolean = false,
    val tapActionType: TapActionType = TapActionType.NONE,
    val tapActionTarget: String = "pulse"
)

data class ProjectFile(
    val fileName: String,
    val language: String = "widgetscript",
    val content: String,
    val isEntry: Boolean = false
)

enum class AnimProperty(val labelUk: String) {
    X("Позиція X"),
    Y("Позиція Y"),
    SCALE("Масштаб"),
    ROTATION("Поворот (°)"),
    OPACITY("Прозорість"),
    CORNER_RADIUS("Радіус кутів"),
    GLOW_BLUR("Розмиття / Світіння")
}

enum class EasingCurve(val labelUk: String) {
    LINEAR("Лінійна (Linear)"),
    EASE_IN("Плавний старт (Ease In)"),
    EASE_OUT("Плавний фініш (Ease Out)"),
    EASE_IN_OUT("Плавна (Ease In-Out)"),
    SPRING("Пружина (Spring)"),
    BOUNCE("Відскок (Bounce)")
}

enum class TriggerType(val labelUk: String) {
    ON_LOAD("При завантаженні"),
    ON_TAP("При натисканні"),
    ON_TIMER("За таймером"),
    ON_STATE_CHANGE("При зміні стану")
}

enum class ParticlePreset(val labelUk: String) {
    NONE("Вимкнено"),
    CYBER_SPARKS("Кібер-іскри"),
    NEBULA_ORBS("Туманність"),
    PULSE_RINGS("Імпульсні кільця"),
    MATRIX_FLOW("Матричний потік")
}

data class Keyframe(
    val timeMs: Int,
    val value: Float,
    val easing: EasingCurve = EasingCurve.EASE_IN_OUT
)

data class AnimationTrack(
    val id: String,
    val name: String,
    val targetElementId: String,
    val property: AnimProperty,
    val durationMs: Int = 1200,
    val loop: Boolean = true,
    val reverseOnRepeat: Boolean = true,
    val trigger: TriggerType = TriggerType.ON_LOAD,
    val particlePreset: ParticlePreset = ParticlePreset.NONE,
    val keyframes: List<Keyframe> = listOf(
        Keyframe(0, 0f, EasingCurve.EASE_IN_OUT),
        Keyframe(600, 1f, EasingCurve.SPRING),
        Keyframe(1200, 0f, EasingCurve.EASE_IN_OUT)
    )
)

enum class GameTemplateType(val labelUk: String, val descriptionUk: String) {
    ASTRO_DODGER("Astro Dodger 2D", "Космічний корабель ухиляється від астероїдів і збирає енергетичні кристали"),
    CYBER_PONG("Cyber Pong Arcade", "Неоновий аркадний пінг-понг із фізикою відбиття та прискоренням"),
    ORBIT_DEFENDER("Orbit Pulse Tap", "Інтерактивна гра-реакція: перехоплюй орбітальні імпульси в зоні резонансу")
}

data class MiniGameConfig(
    val templateType: GameTemplateType = GameTemplateType.ASTRO_DODGER,
    val gravity: Float = 0.0f,
    val baseSpeed: Float = 1.0f,
    val spawnRateMs: Int = 900,
    val targetWinScore: Int = 150,
    val highScore: Int = 0,
    val lastScore: Int = 0,
    val currentLevel: Int = 1,
    val soundEffectsEnabled: Boolean = true,
    val showDebugHitboxes: Boolean = false
)

enum class DiagnosticSeverity { INFO, WARNING, ERROR }

data class DiagnosticMessage(
    val fileName: String = "main.ws",
    val line: Int,
    val column: Int = 1,
    val severity: DiagnosticSeverity,
    val message: String,
    val suggestion: String = ""
)

data class StudioPlugin(
    val id: String,
    val name: String,
    val version: String,
    val apiVersion: String = "1.0",
    val author: String,
    val description: String,
    val providedNamespace: String,
    val providedFunctions: List<String>,
    val permissions: List<String>,
    val signatureVerified: Boolean = true,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = true
)

data class WidgetTemplateItem(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val gridSize: WidgetGridSize,
    val backgroundColor: String,
    val cornerRadius: Float,
    val requiredApi: List<String>,
    val elements: List<WidgetElement>,
    val files: List<ProjectFile>,
    val animations: List<AnimationTrack>,
    val gameConfig: MiniGameConfig
)

/**
 * Encapsulates visual styling attributes of a widget (background, corner radius, grid size, accent color).
 */
data class WidgetStyle(
    val gridSize: WidgetGridSize = WidgetGridSize.SIZE_2X2,
    val backgroundColor: String = "#10131C",
    val cornerRadius: Float = 24f,
    val accentColor: String = "#00E5FF"
)

/**
 * Represents a persisted widget configuration containing both metadata/elements and a [WidgetStyle].
 * Provides [toWidgetStyle] and direct [style] access so functions expecting [WidgetStyle] can accept either.
 */
data class SavedWidgetConfig(
    val projectId: Long = 0L,
    val name: String = "System Dashboard",
    val style: WidgetStyle = WidgetStyle(),
    val elements: List<WidgetElement> = emptyList()
) {
    fun toWidgetStyle(): WidgetStyle = style
}

object StudioJsonSerializer {

    fun elementsToJson(elements: List<WidgetElement>): String {
        val arr = JSONArray()
        for (e in elements) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("name", e.name)
                put("type", e.type.name)
                put("x", e.x.toDouble())
                put("y", e.y.toDouble())
                put("width", e.width.toDouble())
                put("height", e.height.toDouble())
                put("rotation", e.rotation.toDouble())
                put("opacity", e.opacity.toDouble())
                put("backgroundColor", e.backgroundColor)
                put("borderColor", e.borderColor)
                put("borderWidth", e.borderWidth.toDouble())
                put("cornerRadius", e.cornerRadius.toDouble())
                put("shadowElevation", e.shadowElevation.toDouble())
                put("textValue", e.textValue)
                put("fontSize", e.fontSize.toDouble())
                put("textColor", e.textColor)
                put("progressValue", e.progressValue.toDouble())
                put("iconName", e.iconName)
                put("layerOrder", e.layerOrder)
                put("groupId", e.groupId)
                put("visible", e.visible)
                put("locked", e.locked)
                put("tapActionType", e.tapActionType.name)
                put("tapActionTarget", e.tapActionTarget)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun elementsFromJson(json: String): List<WidgetElement> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<WidgetElement>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val type = runCatching { ElementType.valueOf(o.optString("type", "TEXT")) }
                    .getOrDefault(ElementType.TEXT)
                val tapType = runCatching { TapActionType.valueOf(o.optString("tapActionType", "NONE")) }
                    .getOrDefault(TapActionType.NONE)
                list.add(
                    WidgetElement(
                        id = o.optString("id", "el_$i"),
                        name = o.optString("name", "Element $i"),
                        type = type,
                        x = o.optDouble("x", 20.0).toFloat(),
                        y = o.optDouble("y", 20.0).toFloat(),
                        width = o.optDouble("width", 120.0).toFloat(),
                        height = o.optDouble("height", 44.0).toFloat(),
                        rotation = o.optDouble("rotation", 0.0).toFloat(),
                        opacity = o.optDouble("opacity", 1.0).toFloat(),
                        backgroundColor = o.optString("backgroundColor", "#1E2638"),
                        borderColor = o.optString("borderColor", "#00E5FF"),
                        borderWidth = o.optDouble("borderWidth", 0.0).toFloat(),
                        cornerRadius = o.optDouble("cornerRadius", 12.0).toFloat(),
                        shadowElevation = o.optDouble("shadowElevation", 0.0).toFloat(),
                        textValue = o.optString("textValue", ""),
                        fontSize = o.optDouble("fontSize", 16.0).toFloat(),
                        textColor = o.optString("textColor", "#FFFFFF"),
                        progressValue = o.optDouble("progressValue", 0.75).toFloat(),
                        iconName = o.optString("iconName", "bolt"),
                        layerOrder = o.optInt("layerOrder", i),
                        groupId = o.optString("groupId", ""),
                        visible = o.optBoolean("visible", true),
                        locked = o.optBoolean("locked", false),
                        tapActionType = tapType,
                        tapActionTarget = o.optString("tapActionTarget", "pulse")
                    )
                )
            }
            list.sortedBy { it.layerOrder }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun filesToJson(files: List<ProjectFile>): String {
        val arr = JSONArray()
        for (f in files) {
            arr.put(
                JSONObject().apply {
                    put("fileName", f.fileName)
                    put("language", f.language)
                    put("content", f.content)
                    put("isEntry", f.isEntry)
                }
            )
        }
        return arr.toString()
    }

    fun filesFromJson(json: String): List<ProjectFile> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ProjectFile>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ProjectFile(
                        fileName = o.optString("fileName", "main.ws"),
                        language = o.optString("language", "widgetscript"),
                        content = o.optString("content", ""),
                        isEntry = o.optBoolean("isEntry", i == 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun animationsToJson(tracks: List<AnimationTrack>): String {
        val arr = JSONArray()
        for (t in tracks) {
            val kfArr = JSONArray()
            for (kf in t.keyframes) {
                kfArr.put(
                    JSONObject().apply {
                        put("timeMs", kf.timeMs)
                        put("value", kf.value.toDouble())
                        put("easing", kf.easing.name)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("targetElementId", t.targetElementId)
                    put("property", t.property.name)
                    put("durationMs", t.durationMs)
                    put("loop", t.loop)
                    put("reverseOnRepeat", t.reverseOnRepeat)
                    put("trigger", t.trigger.name)
                    put("particlePreset", t.particlePreset.name)
                    put("keyframes", kfArr)
                }
            )
        }
        return arr.toString()
    }

    fun animationsFromJson(json: String): List<AnimationTrack> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<AnimationTrack>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val kfArr = o.optJSONArray("keyframes") ?: JSONArray()
                val kfs = mutableListOf<Keyframe>()
                for (j in 0 until kfArr.length()) {
                    val ko = kfArr.getJSONObject(j)
                    val easing = runCatching { EasingCurve.valueOf(ko.optString("easing", "EASE_IN_OUT")) }
                        .getOrDefault(EasingCurve.EASE_IN_OUT)
                    kfs.add(Keyframe(ko.optInt("timeMs", 0), ko.optDouble("value", 0.0).toFloat(), easing))
                }
                val prop = runCatching { AnimProperty.valueOf(o.optString("property", "SCALE")) }
                    .getOrDefault(AnimProperty.SCALE)
                val trig = runCatching { TriggerType.valueOf(o.optString("trigger", "ON_LOAD")) }
                    .getOrDefault(TriggerType.ON_LOAD)
                val part = runCatching { ParticlePreset.valueOf(o.optString("particlePreset", "NONE")) }
                    .getOrDefault(ParticlePreset.NONE)
                list.add(
                    AnimationTrack(
                        id = o.optString("id", "anim_$i"),
                        name = o.optString("name", "pulse"),
                        targetElementId = o.optString("targetElementId", "clock"),
                        property = prop,
                        durationMs = o.optInt("durationMs", 1200),
                        loop = o.optBoolean("loop", true),
                        reverseOnRepeat = o.optBoolean("reverseOnRepeat", true),
                        trigger = trig,
                        particlePreset = part,
                        keyframes = if (kfs.isEmpty()) listOf(Keyframe(0, 0f), Keyframe(1000, 1f)) else kfs
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun gameConfigToJson(cfg: MiniGameConfig): String {
        return JSONObject().apply {
            put("templateType", cfg.templateType.name)
            put("gravity", cfg.gravity.toDouble())
            put("baseSpeed", cfg.baseSpeed.toDouble())
            put("spawnRateMs", cfg.spawnRateMs)
            put("targetWinScore", cfg.targetWinScore)
            put("highScore", cfg.highScore)
            put("lastScore", cfg.lastScore)
            put("currentLevel", cfg.currentLevel)
            put("soundEffectsEnabled", cfg.soundEffectsEnabled)
            put("showDebugHitboxes", cfg.showDebugHitboxes)
        }.toString()
    }

    fun gameConfigFromJson(json: String): MiniGameConfig {
        if (json.isBlank()) return MiniGameConfig()
        return try {
            val o = JSONObject(json)
            val tpl = runCatching { GameTemplateType.valueOf(o.optString("templateType", "ASTRO_DODGER")) }
                .getOrDefault(GameTemplateType.ASTRO_DODGER)
            MiniGameConfig(
                templateType = tpl,
                gravity = o.optDouble("gravity", 0.0).toFloat(),
                baseSpeed = o.optDouble("baseSpeed", 1.0).toFloat(),
                spawnRateMs = o.optInt("spawnRateMs", 900),
                targetWinScore = o.optInt("targetWinScore", 150),
                highScore = o.optInt("highScore", 0),
                lastScore = o.optInt("lastScore", 0),
                currentLevel = o.optInt("currentLevel", 1),
                soundEffectsEnabled = o.optBoolean("soundEffectsEnabled", true),
                showDebugHitboxes = o.optBoolean("showDebugHitboxes", false)
            )
        } catch (_: Exception) {
            MiniGameConfig()
        }
    }
}
