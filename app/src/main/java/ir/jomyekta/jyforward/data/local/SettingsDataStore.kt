package ir.jomyekta.jyforward.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "jy_forward_settings")

enum class AppThemeMode(val value: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK");

    companion object {
        fun fromValue(v: String?): AppThemeMode =
            entries.firstOrNull { it.value == v } ?: SYSTEM
    }
}

enum class AppLanguageMode(val code: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    PERSIAN("fa");

    companion object {
        fun fromCode(c: String?): AppLanguageMode =
            entries.firstOrNull { it.code == c } ?: SYSTEM
    }
}

data class UserSettingsState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val languageMode: AppLanguageMode = AppLanguageMode.SYSTEM,
    val fontScale: Float = 1.0f, // 0.85f .. 1.35f
    val appLockEnabled: Boolean = false,
    val serviceEnabled: Boolean = true
)

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("language_mode")
        private val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
        private val KEY_APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        private val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
    }

    val settingsFlow: Flow<UserSettingsState> = context.dataStore.data.map { prefs ->
        UserSettingsState(
            themeMode = AppThemeMode.fromValue(prefs[KEY_THEME]),
            languageMode = AppLanguageMode.fromCode(prefs[KEY_LANGUAGE]),
            fontScale = (prefs[KEY_FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.35f),
            appLockEnabled = prefs[KEY_APP_LOCK] ?: false,
            serviceEnabled = prefs[KEY_SERVICE_ENABLED] ?: true
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.value }
    }

    suspend fun setLanguageMode(mode: AppLanguageMode) {
        context.dataStore.edit { it[KEY_LANGUAGE] = mode.code }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[KEY_FONT_SCALE] = scale.coerceIn(0.85f, 1.35f) }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_APP_LOCK] = enabled }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SERVICE_ENABLED] = enabled }
    }
}
