package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_studio_x_prefs")

data class StudioSettings(
    val darkTheme: Boolean = true,
    val codeFontSizeSp: Int = 13,
    val snapToGrid: Boolean = true,
    val showAlignmentGuides: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val lowPowerAnimationMode: Boolean = false,
    val sandboxInstructionLimit: Int = 2000
)

class SettingsPreferences(private val context: Context) {

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_CODE_FONT_SIZE = intPreferencesKey("code_font_size_sp")
        private val KEY_SNAP_TO_GRID = booleanPreferencesKey("snap_to_grid")
        private val KEY_SHOW_GUIDES = booleanPreferencesKey("show_alignment_guides")
        private val KEY_AUTO_SAVE = booleanPreferencesKey("auto_save_enabled")
        private val KEY_LOW_POWER = booleanPreferencesKey("low_power_anim_mode")
        private val KEY_SANDBOX_LIMIT = intPreferencesKey("sandbox_instruction_limit")
    }

    val settingsFlow: Flow<StudioSettings> = context.studioDataStore.data.map { prefs ->
        StudioSettings(
            darkTheme = prefs[KEY_DARK_THEME] ?: true,
            codeFontSizeSp = prefs[KEY_CODE_FONT_SIZE] ?: 13,
            snapToGrid = prefs[KEY_SNAP_TO_GRID] ?: true,
            showAlignmentGuides = prefs[KEY_SHOW_GUIDES] ?: true,
            autoSaveEnabled = prefs[KEY_AUTO_SAVE] ?: true,
            lowPowerAnimationMode = prefs[KEY_LOW_POWER] ?: false,
            sandboxInstructionLimit = prefs[KEY_SANDBOX_LIMIT] ?: 2000
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.studioDataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    suspend fun setCodeFontSize(sizeSp: Int) {
        context.studioDataStore.edit { it[KEY_CODE_FONT_SIZE] = sizeSp.coerceIn(10, 22) }
    }

    suspend fun setSnapToGrid(enabled: Boolean) {
        context.studioDataStore.edit { it[KEY_SNAP_TO_GRID] = enabled }
    }

    suspend fun setShowGuides(enabled: Boolean) {
        context.studioDataStore.edit { it[KEY_SHOW_GUIDES] = enabled }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.studioDataStore.edit { it[KEY_AUTO_SAVE] = enabled }
    }

    suspend fun setLowPowerMode(enabled: Boolean) {
        context.studioDataStore.edit { it[KEY_LOW_POWER] = enabled }
    }

    suspend fun setSandboxLimit(limit: Int) {
        context.studioDataStore.edit { it[KEY_SANDBOX_LIMIT] = limit.coerceIn(500, 10000) }
    }
}
