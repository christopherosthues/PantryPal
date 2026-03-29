package org.darchacheron.pantrypal.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.darchacheron.pantrypal.navigation.OcrType
import org.darchacheron.pantrypal.ui.AdaptiveRow
import org.darchacheron.pantrypal.ui.ImageSection
import org.darchacheron.pantrypal.ui.calculateWindowSizeClass
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun InventoryDetailView(
    viewModel: InventoryDetailViewModel = koinInject()
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
                                if (viewModel.isAdding) Res.string.inventory_detail_title_add
                                else Res.string.inventory_detail_title_edit
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
                                contentDescription = stringResource(Res.string.inventory_detail_content_description_delete),
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
                            stringResource(Res.string.inventory_detail_content_description_cancel_editing)
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
                            contentDescription = stringResource(Res.string.inventory_detail_content_description_save)
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
            val item = uiState.data ?: return@Scaffold

            ImageSection(
                primaryImage = item.image,
                additionalImages = item.additionalImages,
                onAddPrimaryImage = { viewModel.openCamera() },
                onRemovePrimaryImage = { viewModel.removePrimaryImage() },
                onAddAdditionalImage = { viewModel.addAdditionalImage() },
                onRemoveAdditionalImage = { viewModel.removeAdditionalImage(it) }
            )

            OutlinedTextField(
                value = item.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text(stringResource(Res.string.inventory_detail_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.openOcrCamera(OcrType.NAME) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = stringResource(Res.string.inventory_detail_ocr_name)
                        )
                    }
                }
            )

            AdaptiveRow(
                useTwoColumns = useTwoColumns,
                leftContent = {
                    OutlinedTextField(
                        value = viewModel.fillingQuantityStr,
                        onValueChange = { viewModel.updateFillingQuantity(it) },
                        label = {
                            Text(
                                stringResource(
                                    if (item.isLiquid) Res.string.food_detail_volume
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
