package ir.jomyekta.jyforward.ui.viewmodel

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.jomyekta.jyforward.JYForwardApplication
import ir.jomyekta.jyforward.data.local.*
import ir.jomyekta.jyforward.data.repository.ForwardRepository
import ir.jomyekta.jyforward.service.SmsForwardingService
import ir.jomyekta.jyforward.util.BackupJsonManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: ForwardRepository,
    private val settingsDataStore: SettingsDataStore
) : AndroidViewModel(application) {

    val rulesState: StateFlow<List<FilterRuleEntity>> = repository.allRulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyLogsState: StateFlow<List<HistoryLogEntity>> = repository.allHistoryLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsState: StateFlow<UserSettingsState> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettingsState())

    val isServiceRunning: StateFlow<Boolean> = SmsForwardingService.isRunning

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    // --- Filter Rule CRUD Operations ---

    fun saveRule(rule: FilterRuleEntity) {
        viewModelScope.launch {
            repository.saveRule(rule)
        }
    }

    fun addSampleJomYektaRule() {
        viewModelScope.launch {
            repository.saveRule(
                FilterRuleEntity(
                    title = "JomYekta Card-to-Card (واریز)",
                    senderFilter = "*",
                    keyword = "واریز",
                    destinationUrl = AppDatabase.SAMPLE_JOMYEKTA_WEBHOOK,
                    httpMethod = "GET",
                    simSlotFilter = -1,
                    isEnabled = true
                )
            )
            _uiMessage.emit("Sample JomYekta Card-to-Card rule added")
        }
    }

    fun cloneRule(rule: FilterRuleEntity) {
        viewModelScope.launch {
            repository.cloneRule(rule)
            _uiMessage.emit("Rule cloned: ${rule.title}")
        }
    }

    fun deleteRule(rule: FilterRuleEntity) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }

    fun toggleRuleEnabled(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRule(ruleId, enabled)
        }
    }

    fun testWebhookForRule(rule: FilterRuleEntity) {
        viewModelScope.launch {
            val sampleSms = "بانک ملت\nواریز مبلغ 1,500,000 ریال به حساب شما\nمانده: 45,000,000 ریال"
            val result = repository.testRuleWebhook(rule, sampleSms)
            val statusMsg = if (result.isSuccess) {
                "Webhook Test OK (HTTP ${result.httpStatusCode})"
            } else {
                "Webhook Test Failed: ${result.responseBodySummary}"
            }
            _uiMessage.emit(statusMsg)
        }
    }

    // --- History Operations ---

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _uiMessage.emit("History cleared")
        }
    }

    // --- Settings & Localization Operations ---

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun updateLanguageMode(mode: AppLanguageMode) {
        viewModelScope.launch {
            settingsDataStore.setLanguageMode(mode)
            val localeList = when (mode) {
                AppLanguageMode.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                AppLanguageMode.ENGLISH -> LocaleListCompat.forLanguageTags("en")
                AppLanguageMode.PERSIAN -> LocaleListCompat.forLanguageTags("fa")
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    fun updateFontScale(scale: Float) {
        viewModelScope.launch {
            settingsDataStore.setFontScale(scale)
        }
    }

    fun updateAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAppLockEnabled(enabled)
        }
    }

    // --- Backup & Restore (Room DB JSON) ---

    suspend fun generateBackupJson(): String {
        val rules = repository.getAllRulesSnapshot()
        val currentSettings = settingsState.value
        return BackupJsonManager.exportToJson(rules, currentSettings)
    }

    fun importBackupJson(jsonContent: String, replaceExisting: Boolean = false) {
        viewModelScope.launch {
            try {
                val payload = BackupJsonManager.importFromJson(jsonContent)
                repository.replaceOrMergeRules(payload.rules, clearExisting = replaceExisting)
                settingsDataStore.setThemeMode(AppThemeMode.fromValue(payload.themeMode))
                settingsDataStore.setLanguageMode(AppLanguageMode.fromCode(payload.languageMode))
                settingsDataStore.setFontScale(payload.fontScale)
                settingsDataStore.setAppLockEnabled(payload.appLockEnabled)
                _uiMessage.emit("Imported ${payload.rules.size} rules successfully!")
            } catch (e: Exception) {
                _uiMessage.emit("Import error: ${e.localizedMessage ?: "Invalid JSON"}")
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as JYForwardApplication)
                MainViewModel(
                    application = app,
                    repository = app.repository,
                    settingsDataStore = SettingsDataStore(app)
                )
            }
        }
    }
}
