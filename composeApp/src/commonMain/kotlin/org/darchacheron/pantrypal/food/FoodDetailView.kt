package org.darchacheron.pantrypal.food

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun FoodDetailView(
    viewModel: FoodDetailViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isSaved) {
        if (isSaved && snackbarMessage == null) {
            viewModel.goBack()
        }
    }

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
                    Text(
                        stringResource(
                            if (viewModel.navigationRoute.foodId == null) Res.string.food_detail_title_add
                            else Res.string.food_detail_title_edit
                        )
                    )
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
                    if (viewModel.navigationRoute.foodId != null) {
                        IconButton(onClick = { viewModel.delete() }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_delete),
                                contentDescription = stringResource(Res.string.food_detail_delete)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !uiState.isLoading && viewModel.name.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(Res.string.food_detail_save))
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (viewModel.imagePath != null) {
                AsyncImage(
                    model = viewModel.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text(stringResource(Res.string.food_detail_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = viewModel.calories,
                    onValueChange = { viewModel.calories = it },
                    label = { Text(stringResource(Res.string.food_detail_calories)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.weight,
                    onValueChange = { viewModel.weight = it },
                    label = { Text(stringResource(Res.string.food_detail_weight)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = viewModel.carbs,
                    onValueChange = { viewModel.carbs = it },
                    label = { Text(stringResource(Res.string.food_detail_carbs)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.protein,
                    onValueChange = { viewModel.protein = it },
                    label = { Text(stringResource(Res.string.food_detail_protein)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.fat,
                    onValueChange = { viewModel.fat = it },
                    label = { Text(stringResource(Res.string.food_detail_fat)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            DatePickerField(
                label = stringResource(Res.string.food_detail_best_before),
                selectedDate = viewModel.bestBeforeDate,
                onDateSelected = { viewModel.bestBeforeDate = it }
            )

            DatePickerField(
                label = stringResource(Res.string.food_detail_use_by),
                selectedDate = viewModel.useByDate,
                onDateSelected = { viewModel.useByDate = it }
            )

            DatePickerField(
                label = stringResource(Res.string.food_detail_opened_at),
                selectedDate = viewModel.openedAt,
                onDateSelected = { viewModel.openedAt = it }
            )

            IconButton(
                onClick = { viewModel.openCamera() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_camera),
                    contentDescription = stringResource(Res.string.food_detail_content_description_open_camera)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate?.toString() ?: "",
        onValueChange = { },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(
                    painter = painterResource(Res.drawable.arrow_drop_down),
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
