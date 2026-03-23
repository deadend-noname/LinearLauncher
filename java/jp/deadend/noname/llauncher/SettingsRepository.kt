package jp.deadend.noname.llauncher

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        private val RECENT_APPS_LIMIT = intPreferencesKey("recent_apps_limit")
        private val EXIT_ON_LAUNCH = booleanPreferencesKey("exit_on_launch")
        private val ROW_HEIGHT = intPreferencesKey("row_height")
    }

    val recentAppsLimit: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[RECENT_APPS_LIMIT] ?: 5
    }

    val exitOnLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[EXIT_ON_LAUNCH] ?: true
    }

    val rowHeight: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ROW_HEIGHT] ?: 72
    }

    suspend fun setRecentAppsLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[RECENT_APPS_LIMIT] = limit
        }
    }

    suspend fun setExitOnLaunch(exit: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXIT_ON_LAUNCH] = exit
        }
    }

    suspend fun setRowHeight(height: Int) {
        context.dataStore.edit { preferences ->
            preferences[ROW_HEIGHT] = height.coerceIn(30, 150)
        }
    }
}