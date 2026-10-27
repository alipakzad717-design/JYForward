package ir.jomyekta.jyforward.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import ir.jomyekta.jyforward.data.local.AppLanguageMode
import ir.jomyekta.jyforward.data.local.AppThemeMode
import ir.jomyekta.jyforward.data.local.FilterRuleEntity
import ir.jomyekta.jyforward.data.local.UserSettingsState

data class BackupPayload(
    val appName: String = "JY Forward",
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val themeMode: String = AppThemeMode.SYSTEM.value,
    val languageMode: String = AppLanguageMode.SYSTEM.code,
    val fontScale: Float = 1.0f,
    val appLockEnabled: Boolean = false,
    val rules: List<FilterRuleEntity> = emptyList()
)

/**
 * Serializes and deserializes Room DB Filter Rules + App Settings to/from JSON.
 */
object BackupJsonManager {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    fun exportToJson(
        rules: List<FilterRuleEntity>,
        settings: UserSettingsState
    ): String {
        val payload = BackupPayload(
            themeMode = settings.themeMode.value,
            languageMode = settings.languageMode.code,
            fontScale = settings.fontScale,
            appLockEnabled = settings.appLockEnabled,
            rules = rules
        )
        return gson.toJson(payload)
    }

    fun importFromJson(jsonString: String): BackupPayload {
        return gson.fromJson(jsonString, BackupPayload::class.java)
    }
}
