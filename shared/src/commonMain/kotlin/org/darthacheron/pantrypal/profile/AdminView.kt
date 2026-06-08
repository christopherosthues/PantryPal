package org.darthacheron.pantrypal.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.darthacheron.pantrypal.core.configuration.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.shared.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminView(
    viewModel: AdminViewModel = koinInject(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.admin_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = org.jetbrains.compose.resources.painterResource(Res.drawable.ic_back), contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reloadConfigFromServer() }) {
                        Icon(painter = org.jetbrains.compose.resources.painterResource(Res.drawable.ic_sync), contentDescription = stringResource(Res.string.admin_reload_button))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val data = uiState.data
            if (data != null) {
                AdminConfigForm(
                    config = data,
                    isLoading = uiState.isLoading,
                    onSave = { viewModel.updateConfig(it) }
                )
            }

            if (uiState.isLoading && data == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.error?.let {
                Text(
                    text = stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConfigForm(
    config: ServerDynamicConfiguration,
    isLoading: Boolean,
    onSave: (ServerDynamicConfiguration) -> Unit
) {
    var gracePeriod by remember(config) { mutableStateOf(config.deletion.gracePeriodDays.toString()) }
    var maintenanceMode by remember(config) { mutableStateOf(config.features.maintenanceModeEnabled) }
    var registrationEnabled by remember(config) { mutableStateOf(config.features.registrationEnabled) }
    var syncEnabled by remember(config) { mutableStateOf(config.features.remoteSyncEnabled) }
    var maxImageSize by remember(config) { mutableStateOf(config.storage.maxImageUploadSizeMB.toString()) }
    var supportedTypes by remember(config) { mutableStateOf(config.storage.supportedImageTypes.joinToString(", ")) }
    var rateLimitCapacity by remember(config) { mutableStateOf(config.rateLimiting.rateLimitCapacity.toString()) }
    var telemetrySampling by remember(config) { mutableStateOf(config.diagnostics.telemetrySamplingRate.toString()) }
    var logLevel by remember(config) { mutableStateOf(config.logging.serverLogLevel) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Deletion
        AdminSection(stringResource(Res.string.admin_section_deletion)) {
            OutlinedTextField(
                value = gracePeriod,
                onValueChange = { gracePeriod = it },
                label = { Text(stringResource(Res.string.admin_grace_period_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Features
        AdminSection(stringResource(Res.string.admin_section_features)) {
            SwitchListItem(stringResource(Res.string.admin_maintenance_mode_label), maintenanceMode) { maintenanceMode = it }
            SwitchListItem(stringResource(Res.string.admin_registration_enabled_label), registrationEnabled) { registrationEnabled = it }
            SwitchListItem(stringResource(Res.string.admin_sync_enabled_label), syncEnabled) { syncEnabled = it }
        }

        // Storage
        AdminSection(stringResource(Res.string.admin_section_storage)) {
            OutlinedTextField(
                value = maxImageSize,
                onValueChange = { maxImageSize = it },
                label = { Text(stringResource(Res.string.admin_max_image_size_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = supportedTypes,
                onValueChange = { supportedTypes = it },
                label = { Text(stringResource(Res.string.admin_supported_image_types_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Rate Limiting
        AdminSection(stringResource(Res.string.admin_section_rate_limiting)) {
            OutlinedTextField(
                value = rateLimitCapacity,
                onValueChange = { rateLimitCapacity = it },
                label = { Text(stringResource(Res.string.admin_rate_limit_capacity_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Diagnostics
        AdminSection(stringResource(Res.string.admin_section_diagnostics)) {
            OutlinedTextField(
                value = telemetrySampling,
                onValueChange = { telemetrySampling = it },
                label = { Text(stringResource(Res.string.admin_telemetry_sampling_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Logging
        AdminSection(stringResource(Res.string.admin_section_logging)) {
            val levels = listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR")
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = logLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.admin_log_level_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    levels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                logLevel = level
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val updated = ServerDynamicConfiguration(
                    deletion = DeletionConfiguration(gracePeriod.toIntOrNull() ?: 30),
                    features = FeatureToggles(maintenanceMode, registrationEnabled, syncEnabled),
                    storage = StorageLimits(maxImageSize.toIntOrNull() ?: 10, supportedTypes.split(",").map { it.trim() }),
                    rateLimiting = RateLimitConfiguration(rateLimitCapacity.toIntOrNull() ?: 100),
                    diagnostics = DiagnosticsConfiguration(telemetrySampling.toDoubleOrNull() ?: 1.0),
                    logging = LoggingConfiguration(logLevel)
                )
                onSave(updated)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(Res.string.admin_save_button))
            }
        }
    }
}

@Composable
fun AdminSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun SwitchListItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
