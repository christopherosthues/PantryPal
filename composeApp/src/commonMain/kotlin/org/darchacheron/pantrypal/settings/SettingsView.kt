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
                actions = {}
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
                    onMeasureUnitSelected = viewModel::onMeasureUnitSelected,
                    onThemeModeSelected = viewModel::onThemeModeSelected
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: Settings,
    onMeasureUnitSelected: (MeasureUnit) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Measurement Unit Section
        Column {
            Text(
                text = stringResource(Res.string.settings_measurement_unit),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            MeasureUnitDropdown(
                selectedUnit = settings.weightUnit,
                onUnitSelected = onMeasureUnitSelected
            )
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
private fun MeasureUnitDropdown(
    selectedUnit: MeasureUnit,
    onUnitSelected: (MeasureUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = stringResource(selectedUnit.toStringResource()),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.settings_default_unit)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        painter = painterResource(Res.drawable.arrow_drop_down),
                        contentDescription = stringResource(Res.string.settings_select_unit)
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
            MeasureUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(stringResource(unit.toStringResource())) },
                    onClick = {
                        onUnitSelected(unit)
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
