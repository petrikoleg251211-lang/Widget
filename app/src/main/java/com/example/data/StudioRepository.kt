package com.example.data

import com.example.model.StudioJsonSerializer
import com.example.model.StudioPlugin
import com.example.model.WidgetGridSize
import com.example.model.WidgetTemplateItem
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

data class ArchiveImportResult(
    val success: Boolean,
    val projectId: Long = 0L,
    val message: String
)

class StudioRepository(private val dao: StudioDao) {

    val allProjects: Flow<List<WidgetProjectEntity>> = dao.observeAllProjects()
    val allPlugins: Flow<List<InstalledPluginEntity>> = dao.observePlugins()
    val allLogs: Flow<List<StudioLogEntity>> = dao.observeLogs()

    fun observeVersions(projectId: Long): Flow<List<ProjectVersionEntity>> =
        dao.observeProjectVersions(projectId)

    suspend fun ensureSeedData() {
        val existingProjects = dao.getAllProjectsOnce()
        if (existingProjects.isEmpty()) {
            val templates = BuiltInCatalog.getTemplates()
            templates.forEachIndexed { index, tpl ->
                val projId = createProjectFromTemplate(tpl, pinFirst = (index == 0))
                dao.insertProjectVersion(
                    ProjectVersionEntity(
                        projectId = projId,
                        versionNumber = 1,
                        commitMessage = "Початковий реліз із шаблону '${tpl.title}'",
                        elementsJson = StudioJsonSerializer.elementsToJson(tpl.elements),
                        filesJson = StudioJsonSerializer.filesToJson(tpl.files),
                        animationsJson = StudioJsonSerializer.animationsToJson(tpl.animations),
                        gameConfigJson = StudioJsonSerializer.gameConfigToJson(tpl.gameConfig)
                    )
                )
            }
            logEvent("INFO", "Core", "Ініціалізовано базу даних Widget Studio X та стартові проєкти.")
        }

        val existingPlugins = dao.getPluginsOnce()
        if (existingPlugins.isEmpty()) {
            for (p in BuiltInCatalog.getBuiltInPlugins()) {
                dao.insertPlugin(p.toEntity())
            }
        }
    }

    suspend fun createProjectFromTemplate(
        template: WidgetTemplateItem,
        customName: String? = null,
        pinFirst: Boolean = false
    ): Long {
        val entity = WidgetProjectEntity(
            name = customName?.ifBlank { template.title } ?: template.title,
            description = template.description,
            gridSizeCode = WidgetGridSize.toCode(template.gridSize),
            backgroundColor = template.backgroundColor,
            cornerRadius = template.cornerRadius,
            elementsJson = StudioJsonSerializer.elementsToJson(template.elements),
            filesJson = StudioJsonSerializer.filesToJson(template.files),
            animationsJson = StudioJsonSerializer.animationsToJson(template.animations),
            gameConfigJson = StudioJsonSerializer.gameConfigToJson(template.gameConfig),
            localStorageJson = "{\"taps\": 0, \"sprints\": 1, \"game_score\": ${template.gameConfig.highScore}}",
            schemaVersion = 1,
            isPinnedToHome = pinFirst
        )
        return dao.insertProject(entity)
    }

    suspend fun getProjectById(id: Long): WidgetProjectEntity? = dao.getProjectById(id)

    suspend fun getPinnedOrLatestProject(): WidgetProjectEntity? {
        return dao.getPinnedProject() ?: dao.getAllProjectsOnce().firstOrNull()
    }

    suspend fun saveProject(
        project: WidgetProjectEntity,
        createVersionSnapshot: Boolean = false,
        commitNote: String = "Автозбереження стану"
    ) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        dao.updateProject(updated)
        if (createVersionSnapshot) {
            val versions = dao.getProjectVersionsOnce(project.id)
            val nextVer = (versions.maxOfOrNull { it.versionNumber } ?: 0) + 1
            dao.insertProjectVersion(
                ProjectVersionEntity(
                    projectId = project.id,
                    versionNumber = nextVer,
                    commitMessage = commitNote,
                    elementsJson = updated.elementsJson,
                    filesJson = updated.filesJson,
                    animationsJson = updated.animationsJson,
                    gameConfigJson = updated.gameConfigJson
                )
            )
            logEvent("INFO", "ProjectManager", "Створено контрольний знімок v$nextVer для '${project.name}': $commitNote")
        }
    }

    suspend fun renameProject(projectId: Long, newName: String) {
        val proj = dao.getProjectById(projectId) ?: return
        dao.updateProject(proj.copy(name = newName.trim().ifEmpty { proj.name }, updatedAt = System.currentTimeMillis()))
        logEvent("INFO", "ProjectManager", "Проєкт перейменовано на '$newName'")
    }

    suspend fun duplicateProject(projectId: Long): Long? {
        val orig = dao.getProjectById(projectId) ?: return null
        val copy = orig.copy(
            id = 0L,
            name = "${orig.name} (Копія)",
            isPinnedToHome = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val newId = dao.insertProject(copy)
        dao.insertProjectVersion(
            ProjectVersionEntity(
                projectId = newId,
                versionNumber = 1,
                commitMessage = "Дублікат проєкту '${orig.name}'",
                elementsJson = copy.elementsJson,
                filesJson = copy.filesJson,
                animationsJson = copy.animationsJson,
                gameConfigJson = copy.gameConfigJson
            )
        )
        logEvent("INFO", "ProjectManager", "Дубльовано проєкт '${orig.name}' -> ID $newId")
        return newId
    }

    suspend fun deleteProject(projectId: Long) {
        dao.deleteProjectById(projectId)
        logEvent("WARN", "ProjectManager", "Видалено проєкт ID=$projectId")
    }

    suspend fun setActiveHomeProject(projectId: Long) {
        dao.setActiveHomeProject(projectId)
        logEvent("INFO", "AndroidWidget", "Проєкт ID=$projectId призначено активним для Android AppWidget.")
    }

    suspend fun restoreVersion(projectId: Long, version: ProjectVersionEntity): WidgetProjectEntity? {
        val current = dao.getProjectById(projectId) ?: return null
        val restored = current.copy(
            elementsJson = version.elementsJson,
            filesJson = version.filesJson,
            animationsJson = version.animationsJson,
            gameConfigJson = version.gameConfigJson,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateProject(restored)
        logEvent("INFO", "ProjectManager", "Відновлено версію v${version.versionNumber} проєкту '${current.name}'")
        return restored
    }

    suspend fun exportProjectArchiveJson(projectId: Long): String {
        val proj = dao.getProjectById(projectId) ?: return "{}"
        return JSONObject().apply {
            put("archiveFormat", "WidgetStudioX.Bundle")
            put("schemaVersion", proj.schemaVersion)
            put("exportedAt", System.currentTimeMillis())
            put("name", proj.name)
            put("description", proj.description)
            put("gridSizeCode", proj.gridSizeCode)
            put("backgroundColor", proj.backgroundColor)
            put("cornerRadius", proj.cornerRadius.toDouble())
            put("elements", JSONArray(proj.elementsJson))
            put("files", JSONArray(proj.filesJson))
            put("animations", JSONArray(proj.animationsJson))
            put("gameConfig", JSONObject(proj.gameConfigJson))
        }.toString(2)
    }

    suspend fun importProjectArchiveJson(bundleJson: String): ArchiveImportResult {
        return try {
            val obj = JSONObject(bundleJson)
            val format = obj.optString("archiveFormat", "")
            if (format != "WidgetStudioX.Bundle") {
                return ArchiveImportResult(
                    false,
                    message = "Помилка перевірки пакета: невідомий archiveFormat '$format' (очікується 'WidgetStudioX.Bundle')."
                )
            }
            val schemaVersion = obj.optInt("schemaVersion", 1)
            if (schemaVersion !in 1..2) {
                return ArchiveImportResult(
                    false,
                    message = "Несумісна версія схеми проєкту ($schemaVersion)."
                )
            }
            val name = obj.optString("name", "Імпортований проєкт").trim().ifEmpty { "Імпортований проєкт" }
            val desc = obj.optString("description", "Імпортовано з архіву Widget Studio X")
            val gridSize = obj.optString("gridSizeCode", "2x2")
            val bg = obj.optString("backgroundColor", "#10131C")
            val radius = obj.optDouble("cornerRadius", 24.0).toFloat()
            val elementsArr = obj.optJSONArray("elements") ?: JSONArray()
            val filesArr = obj.optJSONArray("files") ?: JSONArray()
            val animsArr = obj.optJSONArray("animations") ?: JSONArray()
            val gameObj = obj.optJSONObject("gameConfig") ?: JSONObject()

            // Verify no forbidden script tokens inside imported files
            val filesStr = filesArr.toString()
            if (filesStr.contains("Runtime.getRuntime") || filesStr.contains("ProcessBuilder")) {
                logEvent("SANDBOX", "Security", "Заблоковано імпорт проєкту '$name': виявлено заборонені системні виклики.")
                return ArchiveImportResult(
                    false,
                    message = "Імпорт заблоковано системою безпеки: виявлено заборонені виклики у коді."
                )
            }

            val entity = WidgetProjectEntity(
                name = "$name (Імпорт)",
                description = desc,
                gridSizeCode = gridSize,
                backgroundColor = bg,
                cornerRadius = radius,
                elementsJson = elementsArr.toString(),
                filesJson = filesArr.toString(),
                animationsJson = animsArr.toString(),
                gameConfigJson = gameObj.toString(),
                schemaVersion = schemaVersion
            )
            val newId = dao.insertProject(entity)
            dao.insertProjectVersion(
                ProjectVersionEntity(
                    projectId = newId,
                    versionNumber = 1,
                    commitMessage = "Імпортовано з перевіреного архіву",
                    elementsJson = entity.elementsJson,
                    filesJson = entity.filesJson,
                    animationsJson = entity.animationsJson,
                    gameConfigJson = entity.gameConfigJson
                )
            )
            logEvent("INFO", "ProjectManager", "Успішно імпортовано проєкт '$name' (ID=$newId)")
            ArchiveImportResult(true, projectId = newId, message = "Проєкт '$name' успішно перевірено та імпортовано!")
        } catch (e: Exception) {
            logEvent("ERROR", "ProjectManager", "Помилка імпорту архіву: ${e.message}")
            ArchiveImportResult(false, message = "Помилка розбору архіву JSON: ${e.message}")
        }
    }

    suspend fun togglePlugin(pluginId: String, enabled: Boolean) {
        dao.setPluginEnabled(pluginId, enabled)
        logEvent("INFO", "PluginSystem", "Плагін '$pluginId' ${if (enabled) "увімкнено" else "вимкнено"}")
    }

    suspend fun importCustomPluginManifest(manifestJson: String): PluginValidationResult {
        val current = dao.getPluginsOnce()
        val existingNamespaces = current.map { it.namespace }.toSet() +
            setOf("ui", "graphics", "animation", "input", "system", "storage", "timer", "game", "audio", "network", "debug")
        val validation = BuiltInCatalog.validatePluginManifestJson(manifestJson, existingNamespaces)
        if (validation.isValid && validation.plugin != null) {
            dao.insertPlugin(validation.plugin.toEntity())
            logEvent("INFO", "PluginSystem", "Встановлено плагін '${validation.plugin.name}' (модуль '${validation.plugin.providedNamespace}')")
        } else {
            logEvent("ERROR", "PluginSystem", "Відхилено маніфест плагіна: ${validation.errorReason}")
        }
        return validation
    }

    suspend fun removeCustomPlugin(pluginId: String) {
        dao.deleteCustomPlugin(pluginId)
        logEvent("WARN", "PluginSystem", "Видалено користувацький плагін '$pluginId'")
    }

    suspend fun logEvent(level: String, module: String, message: String) {
        dao.insertLog(StudioLogEntity(level = level, module = module, message = message))
    }

    suspend fun clearLogs() {
        dao.clearLogs()
    }

    private fun StudioPlugin.toEntity() = InstalledPluginEntity(
        pluginId = id,
        name = name,
        version = version,
        apiVersion = apiVersion,
        author = author,
        description = description,
        namespace = providedNamespace,
        functionsCsv = providedFunctions.joinToString(","),
        permissionsCsv = permissions.joinToString(","),
        signatureVerified = signatureVerified,
        enabled = enabled,
        isBuiltIn = isBuiltIn
    )
}
