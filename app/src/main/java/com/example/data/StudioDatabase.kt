package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "widget_projects")
data class WidgetProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val gridSizeCode: String = "2x2",
    val backgroundColor: String = "#10131C",
    val cornerRadius: Float = 24f,
    val elementsJson: String,
    val filesJson: String,
    val animationsJson: String,
    val gameConfigJson: String,
    val localStorageJson: String = "{}",
    val schemaVersion: Int = 1,
    val isPinnedToHome: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_versions")
data class ProjectVersionEntity(
    @PrimaryKey(autoGenerate = true) val versionId: Long = 0L,
    val projectId: Long,
    val versionNumber: Int,
    val commitMessage: String,
    val elementsJson: String,
    val filesJson: String,
    val animationsJson: String,
    val gameConfigJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "installed_plugins")
data class InstalledPluginEntity(
    @PrimaryKey val pluginId: String,
    val name: String,
    val version: String,
    val apiVersion: String,
    val author: String,
    val description: String,
    val namespace: String,
    val functionsCsv: String,
    val permissionsCsv: String,
    val signatureVerified: Boolean,
    val enabled: Boolean,
    val isBuiltIn: Boolean
)

@Entity(tableName = "studio_logs")
data class StudioLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val level: String, // INFO, WARN, ERROR, SANDBOX
    val module: String, // Core, WidgetScript, VisualEditor, Runtime, Plugin, AndroidWidget
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface StudioDao {
    @Query("SELECT * FROM widget_projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<WidgetProjectEntity>>

    @Query("SELECT * FROM widget_projects ORDER BY updatedAt DESC")
    suspend fun getAllProjectsOnce(): List<WidgetProjectEntity>

    @Query("SELECT * FROM widget_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): WidgetProjectEntity?

    @Query("SELECT * FROM widget_projects WHERE isPinnedToHome = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getPinnedProject(): WidgetProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: WidgetProjectEntity): Long

    @Update
    suspend fun updateProject(project: WidgetProjectEntity)

    @Query("DELETE FROM widget_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("UPDATE widget_projects SET isPinnedToHome = CASE WHEN id = :projectId THEN 1 ELSE 0 END")
    suspend fun setActiveHomeProject(projectId: Long)

    // Versions / Snapshots
    @Query("SELECT * FROM project_versions WHERE projectId = :projectId ORDER BY versionNumber DESC")
    fun observeProjectVersions(projectId: Long): Flow<List<ProjectVersionEntity>>

    @Query("SELECT * FROM project_versions WHERE projectId = :projectId ORDER BY versionNumber DESC")
    suspend fun getProjectVersionsOnce(projectId: Long): List<ProjectVersionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectVersion(version: ProjectVersionEntity): Long

    // Plugins
    @Query("SELECT * FROM installed_plugins ORDER BY isBuiltIn DESC, name ASC")
    fun observePlugins(): Flow<List<InstalledPluginEntity>>

    @Query("SELECT * FROM installed_plugins ORDER BY isBuiltIn DESC, name ASC")
    suspend fun getPluginsOnce(): List<InstalledPluginEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: InstalledPluginEntity)

    @Query("UPDATE installed_plugins SET enabled = :enabled WHERE pluginId = :pluginId")
    suspend fun setPluginEnabled(pluginId: String, enabled: Boolean)

    @Query("DELETE FROM installed_plugins WHERE pluginId = :pluginId AND isBuiltIn = 0")
    suspend fun deleteCustomPlugin(pluginId: String)

    // Logs
    @Query("SELECT * FROM studio_logs ORDER BY timestamp DESC LIMIT 200")
    fun observeLogs(): Flow<List<StudioLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StudioLogEntity)

    @Query("DELETE FROM studio_logs")
    suspend fun clearLogs()
}

@Database(
    entities = [
        WidgetProjectEntity::class,
        ProjectVersionEntity::class,
        InstalledPluginEntity::class,
        StudioLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Зарезервовано для безпечних міграцій схеми без втрати користувацьких проєктів
            }
        }

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "widget_studio_x.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
