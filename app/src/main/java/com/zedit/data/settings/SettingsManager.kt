package com.zedit.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zedit_settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_DEFAULT_ZOOM_LEVEL = floatPreferencesKey("default_zoom_level")
        private val KEY_LAST_OPENED_PROJECT_ID = longPreferencesKey("last_opened_project_id")
        private val KEY_DEFAULT_EXPORT_RESOLUTION = stringPreferencesKey("default_export_resolution")
    }

    val defaultZoomLevel: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_ZOOM_LEVEL] ?: 10f
    }

    val lastOpenedProjectId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_OPENED_PROJECT_ID]
    }

    val defaultExportResolution: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_EXPORT_RESOLUTION] ?: "source"
    }

    suspend fun setDefaultZoomLevel(zoom: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_ZOOM_LEVEL] = zoom
        }
    }

    suspend fun setLastOpenedProjectId(projectId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_OPENED_PROJECT_ID] = projectId
        }
    }

    suspend fun setDefaultExportResolution(resolution: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_EXPORT_RESOLUTION] = resolution
        }
    }
}
