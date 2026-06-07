package org.darthacheron.pantrypal.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.darthacheron.pantrypal.navigation.OcrType
import org.darthacheron.pantrypal.ui.AdaptiveRow
import org.darthacheron.pantrypal.ui.ImageSection
import org.darthacheron.pantrypal.ui.NutrientFields
import org.darthacheron.pantrypal.ui.calculateWindowSizeClass
import org.darthacheron.pantrypal.utils.format
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.food_detail_best_before
import pantrypal.composeapp.generated.resources.food_detail_content_description_cancel_editing
import pantrypal.composeapp.generated.resources.food_detail_content_description_delete
import pantrypal.composeapp.generated.resources.food_detail_content_description_save
import pantrypal.composeapp.generated.resources.food_detail_name
import pantrypal.composeapp.generated.resources.food_detail_nutritional_header_volume
import pantrypal.composeapp.generated.resources.food_detail_nutritional_header_weight
import pantrypal.composeapp.generated.resources.food_detail_ocr_amount
import pantrypal.composeapp.generated.resources.food_detail_ocr_name
import pantrypal.composeapp.generated.resources.food_detail_opened_at
import pantrypal.composeapp.generated.resources.food_detail_title_add
import pantrypal.composeapp.generated.resources.food_detail_title_edit
import pantrypal.composeapp.generated.resources.food_detail_use_by
import pantrypal.composeapp.generated.resources.food_detail_volume
import pantrypal.composeapp.generated.resources.food_detail_weight
import pantrypal.composeapp.generated.resources.ic_back
import pantrypal.composeapp.generated.resources.ic_calendar
import pantrypal.composeapp.generated.resources.ic_camera
import pantrypal.composeapp.generated.resources.ic_cancel
import pantrypal.composeapp.generated.resources.ic_delete
import pantrypal.composeapp.generated.resources.ic_save
import pantrypal.composeapp.generated.resources.settings_content_description_back
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun FoodDetailView(
    viewModel: FoodDetailViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val windowSizeClass = calculateWindowSizeClass()
    val useTwoColumns = windowSizeClass.widthSizeClass > WindowWidthSizeClass.Medium

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { resource ->
            val message = getString(resource)
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
            if (isSaved) {
                viewModel.goBack()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isLoading) {
                        Text(
                            stringResource(
                                if (viewModel.isAdding) Res.string.food_detail_title_add
                                else Res.string.food_detail_title_edit
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.goBack() }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_back),
                            contentDescription = stringResource(Res.string.settings_content_description_back)
                        )
                    }
                },
                actions = {
                    if (!viewModel.isAdding) {
                        IconButton(onClick = {
                            if (!uiState.isLoading) {
                                viewModel.delete()
                                viewModel.goBack()
                            }
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_delete),
                                contentDescription = stringResource(Res.string.food_detail_content_description_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                FloatingActionButton(
                    onClick = { viewModel.cancelEditing() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_cancel),
                        contentDescription =
                            stringResource(Res.string.food_detail_content_description_cancel_editing)
                    )
                }

                FloatingActionButton(
                    onClick = { if (viewModel.canSave && !uiState.isLoading) viewModel.save() },
                    containerColor = if (viewModel.canSave && !uiState.isLoading) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (viewModel.canSave && !uiState.isLoading) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(Res.drawable.ic_save),
                            contentDescription = stringResource(Res.string.food_detail_content_description_save)
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.hasError) {
            val errorMessage = stringResource(uiState.error!!)
            LaunchedEffect(errorMessage) {
                snackbarHostState.showSnackbar(errorMessage)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val food = uiState.data ?: return@Scaffold

            ImageSection(
                primaryImage = food.image,
                additionalImages = food.additionalImages,
                onAddPrimaryImage = { viewModel.openCamera() },
                onRemovePrimaryImage = { viewModel.removePrimaryImage() },
                onAddAdditionalImage = { viewModel.addAdditionalImage() },
                onRemoveAdditionalImage = { viewModel.removeAdditionalImage(it) }
            )

            OutlinedTextField(
                value = food.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text(stringResource(Res.string.food_detail_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.openOcrCamera(OcrType.NAME) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = stringResource(Res.string.food_detail_ocr_name)
                        )
                    }
                }
            )

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                leftContent = {
                    DatePickerField(
                        viewModel = viewModel,
                        label = stringResource(if (food.isUseBy) Res.string.food_detail_use_by else Res.string.food_detail_best_before),
                        selectedDate = food.bestBeforeUsedByDate,
                        isDetectable = true,
                        onDateSelected = { viewModel.updateBestBeforeUsedByDate(it) },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Switch(
                                checked = food.isUseBy,
                                onCheckedChange = { viewModel.updateIsUseBy(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    )
                },
                rightContent = {
                    DatePickerField(
                        label = stringResource(Res.string.food_detail_opened_at),
                        selectedDate = food.openedAt,
                        onDateSelected = { viewModel.updateOpenedAt(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            )

            OutlinedTextField(
                value = viewModel.fillingQuantityStr,
                onValueChange = { viewModel.updateFillingQuantity(it) },
                label = {
                    Text(
                        stringResource(
                            if (food.isLiquid) Res.string.food_detail_volume
                            else Res.string.food_detail_weight
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Switch(
                        checked = food.isLiquid,
                        onCheckedChange = { viewModel.updateIsLiquid(it) },
                        modifier = Modifier.scale(0.8f)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { viewModel.openOcrCamera(OcrType.AMOUNT) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = stringResource(Res.string.food_detail_ocr_amount)
                        )
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        if (food.isLiquid) Res.string.food_detail_nutritional_header_volume
                        else Res.string.food_detail_nutritional_header_weight
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                TextButton(
                    onClick = { viewModel.openOcrCamera(OcrType.NUTRIENTS) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_camera),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Scan table", style = MaterialTheme.typography.labelMedium)
                }
            }

            NutrientFields(useTwoColumns, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    viewModel: FoodDetailViewModel? = null,
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    isDetectable: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate?.format() ?: "",
        onValueChange = { },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        readOnly = true,
        leadingIcon = leadingIcon,
        trailingIcon = {
            Row {
                if (viewModel != null && isDetectable) {
                    IconButton(onClick = { viewModel.openOcrCamera(OcrType.DATE) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = "OCR Date"
                        )
                    }
                }

                IconButton(onClick = { showDialog = true }, enabled = enabled) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_calendar),
                        contentDescription = null
                    )
                }
            }
        }
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.toEpochDays()?.times(86400000L)
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = LocalDate.fromEpochDays((it / 86400000L).toInt())
                        onDateSelected(date)
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDateSelected(null)
                    showDialog = false
                }) {
                    Text("Clear")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
