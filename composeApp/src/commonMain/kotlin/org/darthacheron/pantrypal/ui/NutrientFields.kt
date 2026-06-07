package org.darthacheron.pantrypal.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.darthacheron.pantrypal.common.Product
import org.darthacheron.pantrypal.common.ProductViewModel
import org.jetbrains.compose.resources.stringResource
import pantrypal.composeapp.generated.resources.*

@Composable
fun <T: Product> NutrientFields(
    useTwoColumns: Boolean,
    viewModel: ProductViewModel<T>
) {
    AdaptiveRow(
        useTwoColumns = useTwoColumns,
        leftContent = {
            OutlinedTextField(
                value = viewModel.kiloCaloriesStr,
                onValueChange = { viewModel.updateKiloCalories(it) },
                label = { Text(stringResource(Res.string.nutrient_calories)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        rightContent = {
            OutlinedTextField(
                value = viewModel.kiloJouleStr,
                onValueChange = { viewModel.updateKiloJoule(it) },
                label = { Text(stringResource(Res.string.nutrient_kj)) },
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
                value = viewModel.fatInGramsStr,
                onValueChange = { viewModel.updateFatInGrams(it) },
                label = { Text(stringResource(Res.string.nutrient_fat)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        rightContent = {
            OutlinedTextField(
                value = viewModel.saturatedFattyAcidsInGramsStr,
                onValueChange = { viewModel.updateSaturatedFattyAcidsInGrams(it) },
                label = { Text(stringResource(Res.string.nutrient_saturated_fat)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }
    )

    AdaptiveRow(
        useTwoColumns = useTwoColumns,
        isDependent = true,
        leftContent = {
            OutlinedTextField(
                value = viewModel.carbsInGramsStr,
                onValueChange = { viewModel.updateCarbsInGrams(it) },
                label = { Text(stringResource(Res.string.nutrient_carbs)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        rightContent = {
            OutlinedTextField(
                value = viewModel.sugarInGramsStr,
                onValueChange = { viewModel.updateSugarInGrams(it) },
                label = { Text(stringResource(Res.string.nutrient_sugar)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        }
    )

    OutlinedTextField(
        value = viewModel.dietaryFiberInGramsStr,
        onValueChange = { viewModel.updateDietaryFiberInGrams(it) },
        label = { Text(stringResource(Res.string.nutrient_fiber)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )

    OutlinedTextField(
        value = viewModel.proteinInGramsStr,
        onValueChange = { viewModel.updateProteinInGrams(it) },
        label = { Text(stringResource(Res.string.nutrient_protein)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )

    OutlinedTextField(
        value = viewModel.saltInGramsStr,
        onValueChange = { viewModel.updateSaltInGrams(it) },
        label = { Text(stringResource(Res.string.nutrient_salt)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}
