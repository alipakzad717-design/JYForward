package ir.jomyekta.jyforward.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ir.jomyekta.jyforward.BuildConfig
import ir.jomyekta.jyforward.R
import ir.jomyekta.jyforward.data.local.AppLanguageMode
import ir.jomyekta.jyforward.data.local.AppThemeMode
import ir.jomyekta.jyforward.data.local.UserSettingsState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: UserSettingsState,
    onThemeChanged: (AppThemeMode) -> Unit,
    onLanguageChanged: (AppLanguageMode) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Appearance & Localization Card (Light / Dark / System + EN / FA + Font Size)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_appearance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Theme Selection
                Text(text = stringResource(R.string.settings_theme), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themes = listOf(
                        AppThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        AppThemeMode.LIGHT to stringResource(R.string.theme_light),
                        AppThemeMode.DARK to stringResource(R.string.theme_dark)
                    )
                    themes.forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeChanged(mode) },
                            label = { Text(label) }
                        )
                    }
                }

                // Bilingual Language Selection (System Default, English, Persian/Farsi)
                Text(text = stringResource(R.string.settings_language), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val languages = listOf(
                        AppLanguageMode.SYSTEM to stringResource(R.string.lang_system),
                        AppLanguageMode.ENGLISH to stringResource(R.string.lang_english),
                        AppLanguageMode.PERSIAN to stringResource(R.string.lang_persian)
                    )
                    languages.forEach { (lang, label) ->
                        FilterChip(
                            selected = settings.languageMode == lang,
                            onClick = { onLanguageChanged(lang) },
                            label = { Text(label) }
                        )
                    }
                }

                // Adjustable Font Scale Slider
                val percentage = (settings.fontScale * 100).roundToInt()
                Text(
                    text = "${stringResource(R.string.settings_font_size)}: $percentage%",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = settings.fontScale,
                    onValueChange = onFontScaleChanged,
                    valueRange = 0.85f..1.35f,
                    steps = 4
                )
            }
        }

        // 2. Security Card (AndroidX Biometric App Lock)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_security),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_app_lock),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.settings_app_lock_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.appLockEnabled,
                        onCheckedChange = onAppLockChanged
                    )
                }
            }
        }

        // 3. Backup & Restore Card (Room DB JSON Export / Import)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_backup),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onExportBackupClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_export_json))
                }

                OutlinedButton(
                    onClick = onImportBackupClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_import_json))
                }
            }
        }

        // 4. About Us Section (App Name, Version, Clickable Links to www.jomyekta.ir & www.jomyekta.com)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_about),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.about_company),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                // Clickable Website Links
                WebsiteLinkRow(
                    label = "www.jomyekta.ir",
                    url = "https://www.jomyekta.ir",
                    onOpenUrl = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )

                WebsiteLinkRow(
                    label = "www.jomyekta.com",
                    url = "https://www.jomyekta.com",
                    onOpenUrl = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }
        }
    }
}

@Composable
private fun WebsiteLinkRow(
    label: String,
    url: String,
    onOpenUrl: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUrl(url) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Default.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}
