package org.darchacheron.pantrypal.food

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDate
import org.darchacheron.pantrypal.navigation.OcrType
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@ExperimentalMaterial3WindowSizeClassApi
fun calculateWindowSizeClass(): WindowSizeClass {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val size = with(density) { windowInfo.containerSize.toSize().toDpSize() }
    return WindowSizeClass.calculateFromSize(size)
}

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
                    if (viewModel.isEditing) {
                        IconButton(onClick = { viewModel.openCamera() }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_camera),
                                contentDescription = stringResource(Res.string.food_detail_content_description_open_camera)
                            )
                        }
                    }
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

                AnimatedVisibility(viewModel.isEditing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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

                AnimatedVisibility(!viewModel.isEditing) {
                    FloatingActionButton(
                        onClick = { viewModel.setIsEditing(true) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(Res.drawable.ic_edit),
                                contentDescription = stringResource(Res.string.food_detail_content_description_edit)
                            )
                        }
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val food = uiState.data ?: return@Scaffold

            if (food.imagePath != null) {
                AsyncImage(
                    model = food.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = stringResource(Res.string.food_detail_additional_images),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(food.additionalImagePaths) { path ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = path,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (viewModel.isEditing) {
                            IconButton(
                                onClick = { viewModel.removeAdditionalImage(path) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                        CircleShape
                                    )
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_cancel),
                                    contentDescription = stringResource(Res.string.food_detail_content_description_remove_image),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (viewModel.isEditing) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.addAdditionalImage() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_camera),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(Res.string.food_detail_add_image),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = food.name,
                onValueChange = { viewModel.updateName(it) },
                readOnly = !viewModel.isEditing,
                label = { Text(stringResource(Res.string.food_detail_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (viewModel.isEditing) {
                        IconButton(onClick = { viewModel.openOcrCamera(OcrType.NAME) }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_camera),
                                contentDescription = "OCR Name"
                            )
                        }
                    }
                }
            )

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                leftContent = {
                    DatePickerField(
                        label = stringResource(if (food.isUseBy) Res.string.food_detail_use_by else Res.string.food_detail_best_before),
                        selectedDate = food.bestBeforeUsedByDate,
                        onDateSelected = { viewModel.updateBestBeforeUsedByDate(it) },
                        enabled = viewModel.isEditing,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Switch(
                                checked = food.isUseBy,
                                enabled = viewModel.isEditing,
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
                        enabled = viewModel.isEditing,
                        modifier = Modifier.weight(1f)
                    )
                }
            )

            OutlinedTextField(
                value = food.amount?.toString() ?: "",
                onValueChange = { viewModel.updateAmount(it) },
                readOnly = !viewModel.isEditing,
                label = {
                    Text(
                        stringResource(
                            if (food.isLiquid) Res.string.food_detail_volume
                            else Res.string.food_detail_weight
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Switch(
                        checked = food.isLiquid,
                        enabled = viewModel.isEditing,
                        onCheckedChange = { viewModel.updateIsLiquid(it) },
                        modifier = Modifier.scale(0.8f)
                    )
                },
                trailingIcon = {
                    if (viewModel.isEditing) {
                        IconButton(onClick = { viewModel.openOcrCamera(OcrType.AMOUNT) }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_camera),
                                contentDescription = "OCR Amount"
                            )
                        }
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
                
                if (viewModel.isEditing) {
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
            }

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                leftContent = {
                    OutlinedTextField(
                        value = food.kiloCalories?.toString() ?: "",
                        onValueChange = { viewModel.updateKiloCalories(it) },
                        readOnly = !viewModel.isEditing,
                        label = { Text(stringResource(Res.string.food_detail_calories)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                },
                rightContent = {
                    OutlinedTextField(
                        value = food.kiloJoule?.toString() ?: "",
                        onValueChange = { viewModel.updateKiloJoule(it) },
                        readOnly = !viewModel.isEditing,
                        label = { Text(stringResource(Res.string.food_detail_kj)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            )

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                isDependent = true,
                leftContent = {
                    OutlinedTextField(
                        value = food.carbsInGrams?.toString() ?: "",
                        onValueChange = { viewModel.updateCarbsInGrams(it) },
                        readOnly = !viewModel.isEditing,
                        label = { Text(stringResource(Res.string.food_detail_carbs)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                },
                rightContent = {
                    OutlinedTextField(
                        value = food.sugarInGrams?.toString() ?: "",
                        onValueChange = { viewModel.updateSugarInGrams(it) },
                        readOnly = !viewModel.isEditing,
                        label = { Text(stringResource(Res.string.food_detail_sugar)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            )

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                isDependent = true,
                leftContent = {
                    OutlinedTextField(
                        value = food.fatInGrams?.toString() ?: "",
                        onValueChange = { viewModel.updateFatInGrams(it) },
                        readOnly = !viewModel.isEditing,
                        label = { Text(stringResource(Res.string.food_detail_fat)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                },
                rightContent = {
                    OutlinedTextField(
                        value = food.saturatedFattyAcidsInGrams ?: "",
                        onValueChange = { viewModel.updateSaturatedFattyAcidsInGrams(it) },
                        readOnly = !viewModel.isEditing,
                        label = { Text(stringResource(Res.string.food_detail_saturated_fat)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            )

            OutlinedTextField(
                value = food.dietaryFiberInGrams?.toString() ?: "",
                onValueChange = { viewModel.updateDietaryFiberInGrams(it) },
                readOnly = !viewModel.isEditing,
                label = { Text(stringResource(Res.string.food_detail_fiber)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = food.proteinInGrams?.toString() ?: "",
                onValueChange = { viewModel.updateProteinInGrams(it) },
                readOnly = !viewModel.isEditing,
                label = { Text(stringResource(Res.string.food_detail_protein)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = food.saltInGrams?.toString() ?: "",
                onValueChange = { viewModel.updateSaltInGrams(it) },
                readOnly = !viewModel.isEditing,
                label = { Text(stringResource(Res.string.food_detail_salt)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}

@Composable
fun AdaptiveRow(
    useTwoColumns: Boolean,
    isDependent: Boolean = false,
    leftContent: @Composable RowScope.() -> Unit,
    rightContent: @Composable RowScope.() -> Unit
) {
    if (useTwoColumns) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            leftContent()
            rightContent()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
               leftContent()
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = if (isDependent) 16.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rightContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate?.toString() ?: "",
        onValueChange = { },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        readOnly = true,
        leadingIcon = leadingIcon,
        trailingIcon = {
            IconButton(onClick = { showDialog = true }, enabled = enabled) {
                Icon(
                    painter = painterResource(Res.drawable.ic_calendar),
                    contentDescription = null
                )
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
