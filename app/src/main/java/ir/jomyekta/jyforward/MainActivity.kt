package ir.jomyekta.jyforward

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import ir.jomyekta.jyforward.service.SmsForwardingService
import ir.jomyekta.jyforward.ui.screens.FiltersScreen
import ir.jomyekta.jyforward.ui.screens.HistoryScreen
import ir.jomyekta.jyforward.ui.screens.SettingsScreen
import ir.jomyekta.jyforward.ui.theme.JYForwardTheme
import ir.jomyekta.jyforward.ui.viewmodel.MainViewModel
import ir.jomyekta.jyforward.util.BiometricLockHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

enum class BottomNavTab {
    FILTERS,
    HISTORY,
    SETTINGS
}

/**
 * MainActivity extends [AppCompatActivity] (which is a FragmentActivity) so that:
 * 1. AndroidX BiometricPrompt (PIN/Pattern/Fingerprint) works seamlessly.
 * 2. AppCompatDelegate.setApplicationLocales() dynamically switches English / Persian (Farsi).
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start sticky Foreground Service if permissions allow
        if (hasRequiredSmsPermissions(this)) {
            SmsForwardingService.startStickyService(this)
        }

        setContent {
            val settings by viewModel.settingsState.collectAsState()
            val rules by viewModel.rulesState.collectAsState()
            val historyLogs by viewModel.historyLogsState.collectAsState()
            val isServiceRunning by viewModel.isServiceRunning.collectAsState()

            var isUnlocked by remember { mutableStateOf(!settings.appLockEnabled) }
            var hasSmsPerms by remember { mutableStateOf(hasRequiredSmsPermissions(this)) }
            var isBatterySafe by remember { mutableStateOf(isIgnoringBatteryOptimizations(this)) }
            var currentTab by remember { mutableStateOf(BottomNavTab.FILTERS) }

            val snackbarHostState = remember { SnackbarHostState() }

            // Collect one-off UI feedback messages
            LaunchedEffect(Unit) {
                viewModel.uiMessage.collectLatest { msg ->
                    snackbarHostState.showSnackbar(msg)
                }
            }

            // Trigger Biometric / Device Credential prompt on launch when App Lock is enabled
            LaunchedEffect(settings.appLockEnabled) {
                if (settings.appLockEnabled && !isUnlocked) {
                    triggerBiometricAuth(
                        onAuthenticated = { isUnlocked = true },
                        onFailed = { err ->
                            lifecycleScope.launch {
                                snackbarHostState.showSnackbar(err)
                            }
                        }
                    )
                } else if (!settings.appLockEnabled) {
                    isUnlocked = true
                }
            }

            // Runtime Permissions Launcher (RECEIVE_SMS, READ_SMS, READ_PHONE_STATE, POST_NOTIFICATIONS)
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                hasSmsPerms = hasRequiredSmsPermissions(this)
                if (hasSmsPerms) {
                    SmsForwardingService.startStickyService(this)
                }
            }

            // SAF JSON Backup Export Launcher
            val exportBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri: Uri? ->
                if (uri != null) {
                    lifecycleScope.launch {
                        try {
                            val json = viewModel.generateBackupJson()
                            contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(json.toByteArray(Charsets.UTF_8))
                            }
                            snackbarHostState.showSnackbar(getString(R.string.backup_success))
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}")
                        }
                    }
                }
            }

            // SAF JSON Backup Import Launcher
            val importBackupLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    lifecycleScope.launch {
                        try {
                            val text = contentResolver.openInputStream(uri)?.use { input ->
                                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                            }.orEmpty()
                            if (text.isNotBlank()) {
                                viewModel.importBackupJson(text, replaceExisting = false)
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Import failed: ${e.localizedMessage}")
                        }
                    }
                }
            }

            JYForwardTheme(
                themeMode = settings.themeMode,
                languageMode = settings.languageMode,
                fontScale = settings.fontScale
            ) {
                if (settings.appLockEnabled && !isUnlocked) {
                    AppLockGateScreen(
                        onAuthenticateClick = {
                            triggerBiometricAuth(
                                onAuthenticated = { isUnlocked = true },
                                onFailed = { err ->
                                    lifecycleScope.launch {
                                        snackbarHostState.showSnackbar(err)
                                    }
                                }
                            )
                        }
                    )
                } else {
                    MainScaffold(
                        currentTab = currentTab,
                        onSelectTab = { currentTab = it },
                        snackbarHostState = snackbarHostState
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                BottomNavTab.FILTERS -> FiltersScreen(
                                    rules = rules,
                                    isServiceRunning = isServiceRunning,
                                    hasSmsPermissions = hasSmsPerms,
                                    isBatteryOptimizationIgnored = isBatterySafe,
                                    onRequestPermissions = {
                                        permissionsLauncher.launch(buildRequiredPermissionsArray())
                                    },
                                    onRequestBatteryOptimization = {
                                        requestIgnoreBatteryOptimizations()
                                        isBatterySafe = isIgnoringBatteryOptimizations(this@MainActivity)
                                    },
                                    onToggleService = { enable ->
                                        if (enable) {
                                            SmsForwardingService.startStickyService(this@MainActivity)
                                        } else {
                                            SmsForwardingService.stopStickyService(this@MainActivity)
                                        }
                                    },
                                    onSaveRule = { viewModel.saveRule(it) },
                                    onCloneRule = { viewModel.cloneRule(it) },
                                    onDeleteRule = { viewModel.deleteRule(it) },
                                    onToggleRule = { id, enabled -> viewModel.toggleRuleEnabled(id, enabled) },
                                    onTestRule = { viewModel.testWebhookForRule(it) },
                                    onAddSampleRule = { viewModel.addSampleJomYektaRule() }
                                )

                                BottomNavTab.HISTORY -> HistoryScreen(
                                    logs = historyLogs,
                                    onClearAll = { viewModel.clearAllHistory() }
                                )

                                BottomNavTab.SETTINGS -> SettingsScreen(
                                    settings = settings,
                                    onThemeChanged = { viewModel.updateThemeMode(it) },
                                    onLanguageChanged = { viewModel.updateLanguageMode(it) },
                                    onFontScaleChanged = { viewModel.updateFontScale(it) },
                                    onAppLockChanged = { enabled ->
                                        viewModel.updateAppLockEnabled(enabled)
                                        if (enabled) isUnlocked = true
                                    },
                                    onExportBackupClick = {
                                        exportBackupLauncher.launch("jy_forward_backup.json")
                                    },
                                    onImportBackupClick = {
                                        importBackupLauncher.launch(arrayOf("application/json", "text/*"))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerBiometricAuth(
        onAuthenticated: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        BiometricLockHelper.authenticate(
            activity = this,
            title = getString(R.string.biometric_title),
            subtitle = getString(R.string.biometric_subtitle),
            onSuccess = onAuthenticated,
            onError = onFailed
        )
    }

    private fun buildRequiredPermissionsArray(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    private fun hasRequiredSmsPermissions(context: Context): Boolean {
        val receiveGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return receiveGranted && readGranted && notifGranted
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizations() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(fallback)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    currentTab: BottomNavTab,
    onSelectTab: (BottomNavTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == BottomNavTab.FILTERS,
                    onClick = { onSelectTab(BottomNavTab.FILTERS) },
                    icon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_filters)) }
                )
                NavigationBarItem(
                    selected = currentTab == BottomNavTab.HISTORY,
                    onClick = { onSelectTab(BottomNavTab.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_history)) }
                )
                NavigationBarItem(
                    selected = currentTab == BottomNavTab.SETTINGS,
                    onClick = { onSelectTab(BottomNavTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
            }
        },
        content = content
    )
}

@Composable
private fun AppLockGateScreen(
    onAuthenticateClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.biometric_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.biometric_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAuthenticateClick) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_unlock_now))
            }
        }
    }
}
