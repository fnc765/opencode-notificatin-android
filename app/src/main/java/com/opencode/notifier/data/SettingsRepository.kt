package com.opencode.notifier.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_PASSWORD = stringPreferencesKey("password")
        private val KEY_WEB_UI_URL = stringPreferencesKey("web_ui_url")
        private val KEY_UI_TYPE = stringPreferencesKey("ui_type")
    }

    data class Settings(
        val serverUrl: String = "",
        val username: String = "opencode",
        val password: String = "",
        val webUiUrl: String = "",
        val uiType: String = "portal"  // "portal" | "opencode"
    ) {
        val isConfigured: Boolean
            get() = serverUrl.isNotBlank()
        val useAuth: Boolean
            get() = password.isNotBlank()
        val isPortal: Boolean
            get() = uiType == "portal"
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            serverUrl = prefs[KEY_SERVER_URL] ?: "",
            username = prefs[KEY_USERNAME] ?: "opencode",
            password = prefs[KEY_PASSWORD] ?: "",
            webUiUrl = prefs[KEY_WEB_UI_URL] ?: "",
            uiType = prefs[KEY_UI_TYPE] ?: "portal"
        )
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { it[KEY_USERNAME] = username }
    }

    suspend fun savePassword(password: String) {
        context.dataStore.edit { it[KEY_PASSWORD] = password }
    }

    suspend fun saveWebUiUrl(url: String) {
        context.dataStore.edit { it[KEY_WEB_UI_URL] = url }
    }

    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit {
            it[KEY_SERVER_URL] = settings.serverUrl
            it[KEY_USERNAME] = settings.username
            it[KEY_PASSWORD] = settings.password
            it[KEY_WEB_UI_URL] = settings.webUiUrl
            it[KEY_UI_TYPE] = settings.uiType
        }
    }
}
