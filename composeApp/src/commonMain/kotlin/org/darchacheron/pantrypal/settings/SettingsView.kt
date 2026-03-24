package org.darchacheron.pantrypal.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.*
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.arrow_drop_down

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    viewModel: SettingsViewModel = koinInject(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.settingsFlow.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.revertChanges()
                        onBack()
                    }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back),
                            contentDescription = stringResource(Res.string.settings_content_description_back)
                        )
                    }
                },
                actions = {
                    if (uiState.hasData && uiState.data!!.dataSynchronization != DataSynchronization.NO_SYNCHRONIZATION) {
                        IconButton(
                            onClick = { viewModel.triggerSync() },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_sync),
                                    contentDescription = stringResource(Res.string.settings_content_description_sync)
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.resetToDefaults() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_reset),
                        contentDescription = stringResource(Res.string.settings_content_description_reset)
                    )
                }
                FloatingActionButton(
                    onClick = {
                        viewModel.saveSettings { onBack() }
                    }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_save),
                        contentDescription = stringResource(Res.string.settings_content_description_save)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.hasError) {
                val errorMessage = stringResource(uiState.error!!)
                LaunchedEffect(errorMessage) {
                    snackbarHostState.showSnackbar(message = errorMessage)
                }
            } else if (uiState.hasData) {
                SettingsContent(
                    settings = uiState.data!!,
                    onDataSynchronizationSelected = viewModel::onDataSynchronizationSelected,
                    onThemeModeSelected = viewModel::onThemeModeSelected,
                    onServerUrlChanged = viewModel::onServerUrlChanged
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: Settings,
    onDataSynchronizationSelected: (DataSynchronization) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onServerUrlChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column {
            Text(
                text = stringResource(Res.string.settings_data_synchronization),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            DataSynchronizationDropdown(
                selectedDataSynchronization = settings.dataSynchronization,
                onDataSynchronizationSelected = onDataSynchronizationSelected
            )
            
            if (settings.dataSynchronization != DataSynchronization.NO_SYNCHRONIZATION) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = settings.serverUrl,
                    onValueChange = onServerUrlChanged,
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com") },
                    singleLine = true
                )
            }
        }

        HorizontalDivider()

        // Theme Section
        Column {
            Text(
                text = stringResource(Res.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ThemeSelection(
                selectedTheme = settings.themeMode,
                onThemeSelected = onThemeModeSelected
            )
        }
    }
}

@Composable
private fun DataSynchronizationDropdown(
    selectedDataSynchronization: DataSynchronization,
    onDataSynchronizationSelected: (DataSynchronization) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = stringResource(selectedDataSynchronization.toStringResource()),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.settings_default_data_synchronization)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_drop_down),
                        contentDescription = stringResource(Res.string.settings_select_data_synchronization)
                    )
                }
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            DataSynchronization.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(stringResource(unit.toStringResource())) },
                    onClick = {
                        onDataSynchronizationSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeSelection(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    Column {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeSelected(mode) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (mode == selectedTheme),
                    onClick = { onThemeSelected(mode) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(mode.toStringResource()))
            }
        }
    }
}
