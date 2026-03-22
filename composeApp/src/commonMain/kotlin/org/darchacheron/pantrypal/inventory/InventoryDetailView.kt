package org.darchacheron.pantrypal.inventory

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.darchacheron.pantrypal.navigation.OcrType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pantrypal.composeapp.generated.resources.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun InventoryDetailView(
    viewModel: InventoryDetailViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (viewModel.isAdding) Res.string.inventory_detail_title_add
                            else Res.string.inventory_detail_title_edit
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
                    IconButton(onClick = { viewModel.openCamera() }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = stringResource(Res.string.food_detail_content_description_open_camera)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (viewModel.canSave) viewModel.save() },
                containerColor = if (viewModel.canSave) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_save),
                    contentDescription = stringResource(Res.string.food_detail_content_description_save)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val item = uiState.data ?: return@Scaffold

            if (item.imagePath != null) {
                AsyncImage(
                    model = item.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            OutlinedTextField(
                value = item.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text(stringResource(Res.string.food_detail_name)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { viewModel.openOcrCamera(OcrType.NAME) }) {
                        Icon(painter = painterResource(Res.drawable.ic_camera), contentDescription = null)
                    }
                }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isLiquid,
                    onCheckedChange = { viewModel.updateIsLiquid(it) }
                )
                Text(
                    text = stringResource(if (item.isLiquid) Res.string.food_detail_volume else Res.string.food_detail_weight),
                    modifier = Modifier.clickable { viewModel.updateIsLiquid(!item.isLiquid) }
                )
            }

            OutlinedTextField(
                value = viewModel.fillingQuantityStr,
                onValueChange = { viewModel.fillingQuantityStr = it },
                label = { Text(stringResource(if (item.isLiquid) Res.string.food_detail_volume else Res.string.food_detail_weight)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Text(
                text = stringResource(if (item.isLiquid) Res.string.food_detail_nutritional_header_volume else Res.string.food_detail_nutritional_header_weight),
                style = MaterialTheme.typography.titleSmall
            )

            NutrientFields(viewModel)

            Text(
                text = stringResource(Res.string.food_detail_additional_images),
                style = MaterialTheme.typography.labelMedium
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(item.additionalImagePaths) { path ->
                    Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))) {
                        AsyncImage(model = path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        IconButton(
                            onClick = { viewModel.removeAdditionalImage(path) },
                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(painter = painterResource(Res.drawable.ic_cancel), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { viewModel.addAdditionalImage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientFields(viewModel: InventoryDetailViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutrientField(value = viewModel.kiloCaloriesStr, onValueChange = { viewModel.kiloCaloriesStr = it }, label = stringResource(Res.string.food_detail_calories), modifier = Modifier.weight(1f))
            NutrientField(value = viewModel.kiloJouleStr, onValueChange = { viewModel.kiloJouleStr = it }, label = stringResource(Res.string.food_detail_kj), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutrientField(value = viewModel.fatInGramsStr, onValueChange = { viewModel.fatInGramsStr = it }, label = stringResource(Res.string.food_detail_fat), modifier = Modifier.weight(1f))
            NutrientField(value = viewModel.saturatedFattyAcidsInGramsStr, onValueChange = { viewModel.saturatedFattyAcidsInGramsStr = it }, label = stringResource(Res.string.food_detail_saturated_fat), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutrientField(value = viewModel.carbsInGramsStr, onValueChange = { viewModel.carbsInGramsStr = it }, label = stringResource(Res.string.food_detail_carbs), modifier = Modifier.weight(1f))
            NutrientField(value = viewModel.sugarInGramsStr, onValueChange = { viewModel.sugarInGramsStr = it }, label = stringResource(Res.string.food_detail_sugar), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutrientField(value = viewModel.proteinInGramsStr, onValueChange = { viewModel.proteinInGramsStr = it }, label = stringResource(Res.string.food_detail_protein), modifier = Modifier.weight(1f))
            NutrientField(value = viewModel.saltInGramsStr, onValueChange = { viewModel.saltInGramsStr = it }, label = stringResource(Res.string.food_detail_salt), modifier = Modifier.weight(1f))
        }
        NutrientField(value = viewModel.dietaryFiberInGramsStr, onValueChange = { viewModel.dietaryFiberInGramsStr = it }, label = stringResource(Res.string.food_detail_fiber))
    }
}

@Composable
fun NutrientField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}
