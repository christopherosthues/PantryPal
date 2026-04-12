package org.darchacheron.pantrypal.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.darchacheron.pantrypal.navigation.OcrType
import org.darchacheron.pantrypal.ui.AdaptiveRow
import org.darchacheron.pantrypal.ui.ImageSection
import org.darchacheron.pantrypal.ui.calculateWindowSizeClass
import org.darchacheron.pantrypal.utils.format
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
import pantrypal.composeapp.generated.resources.nutrient_calories
import pantrypal.composeapp.generated.resources.nutrient_carbs
import pantrypal.composeapp.generated.resources.nutrient_fat
import pantrypal.composeapp.generated.resources.nutrient_fiber
import pantrypal.composeapp.generated.resources.nutrient_kj
import pantrypal.composeapp.generated.resources.nutrient_protein
import pantrypal.composeapp.generated.resources.nutrient_salt
import pantrypal.composeapp.generated.resources.nutrient_saturated_fat
import pantrypal.composeapp.generated.resources.nutrient_sugar
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
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 72.dp),
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
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    )
                },
                rightContent = {
                    DatePickerField(
                        viewModel = viewModel,
                        label = stringResource(Res.string.food_detail_opened_at),
                        selectedDate = food.openedAt,
                        isDetectable = false,
                        onDateSelected = { viewModel.updateOpenedAt(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
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
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.food_detail_weight))
                    Switch(
                        checked = food.isLiquid,
                        onCheckedChange = { viewModel.updateIsLiquid(it) },
                        modifier = Modifier.padding(horizontal = 8.dp).scale(0.8f)
                    )
                    Text(stringResource(Res.string.food_detail_volume))
                }
            }

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                leftContent = {
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
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { viewModel.openOcrCamera(OcrType.AMOUNT) }) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_camera),
                                    contentDescription = stringResource(Res.string.food_detail_ocr_amount)
                                )
                            }
                        }
                    )
                },
                rightContent = {
                    OutlinedTextField(
                        value = viewModel.kiloCaloriesStr,
                        onValueChange = { viewModel.updateKiloCalories(it) },
                        label = { Text(stringResource(Res.string.nutrient_calories)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { viewModel.openOcrCamera(OcrType.NUTRIENTS) }) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_camera),
                                    contentDescription = stringResource(Res.string.nutrient_calories)
                                )
                            }
                        }
                    )
                }
            )

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                leftContent = {
                    OutlinedTextField(
                        value = viewModel.kiloJouleStr,
                        onValueChange = { viewModel.updateKiloJoule(it) },
                        label = { Text(stringResource(Res.string.nutrient_kj)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                },
                rightContent = {
                    OutlinedTextField(
                        value = viewModel.fatInGramsStr,
                        onValueChange = { viewModel.updateFatInGrams(it) },
                        label = { Text(stringResource(Res.string.nutrient_fat)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            )

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                leftContent = {
                    OutlinedTextField(
                        value = viewModel.saturatedFattyAcidsInGramsStr,
                        onValueChange = { viewModel.updateSaturatedFattyAcidsInGrams(it) },
                        label = { Text(stringResource(Res.string.nutrient_saturated_fat)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                },
                rightContent = {
                    OutlinedTextField(
                        value = viewModel.carbsInGramsStr,
                        onValueChange = { viewModel.updateCarbsInGrams(it) },
                        label = { Text(stringResource(Res.string.nutrient_carbs)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            )

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                leftContent = {
                    OutlinedTextField(
                        value = viewModel.sugarInGramsStr,
                        onValueChange = { viewModel.updateSugarInGrams(it) },
                        label = { Text(stringResource(Res.string.nutrient_sugar)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                },
                rightContent = {
                    OutlinedTextField(
                        value = viewModel.dietaryFiberInGramsStr,
                        onValueChange = { viewModel.updateDietaryFiberInGrams(it) },
                        label = { Text(stringResource(Res.string.nutrient_fiber)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            )

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                leftContent = {
                    OutlinedTextField(
                        value = viewModel.proteinInGramsStr,
                        onValueChange = { viewModel.updateProteinInGrams(it) },
                        label = { Text(stringResource(Res.string.nutrient_protein)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                },
                rightContent = {
                    OutlinedTextField(
                        value = viewModel.saltInGramsStr,
                        onValueChange = { viewModel.updateSaltInGrams(it) },
                        label = { Text(stringResource(Res.string.nutrient_salt)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    viewModel: FoodDetailViewModel,
    label: String,
    selectedDate: LocalDate?,
    isDetectable: Boolean,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.toEpochDays()?.times(24 * 60 * 60 * 1000)
    )

    OutlinedTextField(
        value = selectedDate?.format() ?: "",
        onValueChange = { },
        label = { Text(label) },
        modifier = modifier.clickable { showDatePicker = true },
        enabled = false,
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledIndicatorColor = MaterialTheme.colorScheme.outline,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = Color.Transparent
        ),
        leadingIcon = leadingIcon,
        trailingIcon = {
            Row {
                if (isDetectable) {
                    IconButton(onClick = { viewModel.openOcrCamera(OcrType.DATE) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = stringResource(Res.string.food_detail_ocr_name)
                        )
                    }
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_calendar),
                        contentDescription = null
                    )
                }
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = LocalDate.fromEpochDays((it / (24 * 60 * 60 * 1000)).toInt())
                        onDateSelected(date)
                    }
                    showDatePicker = false
                }) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
