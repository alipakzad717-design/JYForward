package ir.jomyekta.jyforward.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.jomyekta.jyforward.R
import ir.jomyekta.jyforward.data.local.AppDatabase
import ir.jomyekta.jyforward.data.local.FilterRuleEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(
    rules: List<FilterRuleEntity>,
    isServiceRunning: Boolean,
    hasSmsPermissions: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onSaveRule: (FilterRuleEntity) -> Unit,
    onCloneRule: (FilterRuleEntity) -> Unit,
    onDeleteRule: (FilterRuleEntity) -> Unit,
    onToggleRule: (Long, Boolean) -> Unit,
    onTestRule: (FilterRuleEntity) -> Unit,
    onAddSampleRule: () -> Unit
) {
    var showRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<FilterRuleEntity?>(null) }
    val isPersian = LocalLayoutDirection.current == LayoutDirection.Rtl

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingRule = null
                    showRuleDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.btn_add_filter)) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Sticky Foreground Service & Dual-SIM Status Card
            item {
                ServiceAndPermissionsHeaderCard(
                    isServiceRunning = isServiceRunning,
                    hasSmsPermissions = hasSmsPermissions,
                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                    onRequestPermissions = onRequestPermissions,
                    onRequestBatteryOptimization = onRequestBatteryOptimization,
                    onToggleService = onToggleService
                )
            }

            // 2. Empty state or Filter Rule cards
            if (rules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.filters_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.filters_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FilledTonalButton(onClick = onAddSampleRule) {
                                Icon(Icons.Default.Bolt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_add_sample_rule))
                            }
                        }
                    }
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    FilterRuleItemCard(
                        rule = rule,
                        isPersian = isPersian,
                        onToggle = { enabled -> onToggleRule(rule.id, enabled) },
                        onEdit = {
                            editingRule = rule
                            showRuleDialog = true
                        },
                        onClone = { onCloneRule(rule) },
                        onDelete = { onDeleteRule(rule) },
                        onTest = { onTestRule(rule) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(76.dp))
            }
        }
    }

    if (showRuleDialog) {
        FilterRuleEditorDialog(
            initialRule = editingRule,
            onDismiss = { showRuleDialog = false },
            onConfirm = { updatedRule ->
                onSaveRule(updatedRule)
                showRuleDialog = false
            }
        )
    }
}

@Composable
private fun ServiceAndPermissionsHeaderCard(
    isServiceRunning: Boolean,
    hasSmsPermissions: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onToggleService: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Default.VerifiedUser else Icons.Default.GppMaybe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isServiceRunning) {
                                stringResource(R.string.service_running)
                            } else {
                                stringResource(R.string.service_stopped)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.service_notification_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Switch(
                    checked = isServiceRunning,
                    onCheckedChange = onToggleService
                )
            }

            if (!hasSmsPermissions || !isBatteryOptimizationIgnored) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Text(
                    text = stringResource(R.string.permissions_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.permissions_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasSmsPermissions) {
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_grant_permissions))
                        }
                    }
                    if (!isBatteryOptimizationIgnored) {
                        FilledTonalButton(
                            onClick = onRequestBatteryOptimization,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_battery_optimization))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRuleItemCard(
    rule: FilterRuleEntity,
    isPersian: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = {},
                            label = { Text(rule.httpMethod.uppercase(), fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Http,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rule.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rule Summary Box
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${stringResource(R.string.rule_summary_prefix)} ${rule.toSummaryText(isPersian)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = rule.destinationUrl,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Test, Edit, Clone, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onTest) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_test_rule))
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.btn_edit))
                }
                IconButton(onClick = onClone) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.btn_clone))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRuleEditorDialog(
    initialRule: FilterRuleEntity?,
    onDismiss: () -> Unit,
    onConfirm: (FilterRuleEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialRule?.title ?: "JomYekta Card-to-Card") }
    var sender by remember { mutableStateOf(initialRule?.senderFilter ?: "*") }
    var keyword by remember { mutableStateOf(initialRule?.keyword ?: "واریز") }
    var destinationUrl by remember {
        mutableStateOf(initialRule?.destinationUrl ?: AppDatabase.SAMPLE_JOMYEKTA_WEBHOOK)
    }
    var httpMethod by remember { mutableStateOf(initialRule?.httpMethod ?: "GET") }
    var simSlotFilter by remember { mutableIntStateOf(initialRule?.simSlotFilter ?: -1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialRule == null) {
                    stringResource(R.string.dialog_add_filter)
                } else {
                    stringResource(R.string.dialog_edit_filter)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_rule_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text(stringResource(R.string.field_sender)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.field_keyword)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = destinationUrl,
                    onValueChange = { destinationUrl = it },
                    label = { Text(stringResource(R.string.field_url)) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                // HTTP Method Selector (GET / POST)
                Text(
                    text = stringResource(R.string.field_method),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GET", "POST").forEach { method ->
                        FilterChip(
                            selected = httpMethod.equals(method, ignoreCase = true),
                            onClick = { httpMethod = method },
                            label = { Text(method) }
                        )
                    }
                }

                // Dual SIM Slot Selector (-1 Both, 0 SIM 1, 1 SIM 2)
                Text(
                    text = stringResource(R.string.field_sim_slot),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val simOptions = listOf(
                        -1 to stringResource(R.string.sim_any),
                        0 to stringResource(R.string.sim_1),
                        1 to stringResource(R.string.sim_2)
                    )
                    simOptions.forEach { (slot, label) ->
                        FilterChip(
                            selected = simSlotFilter == slot,
                            onClick = { simSlotFilter = slot },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { "SMS Rule" }
                    val finalUrl = destinationUrl.trim()
                    if (finalUrl.isNotEmpty()) {
                        onConfirm(
                            FilterRuleEntity(
                                id = initialRule?.id ?: 0L,
                                title = finalTitle,
                                senderFilter = sender.ifBlank { "*" },
                                keyword = keyword.trim(),
                                destinationUrl = finalUrl,
                                httpMethod = httpMethod,
                                simSlotFilter = simSlotFilter,
                                isEnabled = initialRule?.isEnabled ?: true
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}
