package org.darthacheron.pantrypal.common

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.navigation.OcrType
import org.darthacheron.pantrypal.ui.UiState
import org.jetbrains.compose.resources.StringResource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
abstract class ProductViewModel<T : Product>(protected val navigator: Navigator) : ViewModel() {
    protected val internalUiState = MutableStateFlow(UiState<T?>())
    val uiState: StateFlow<UiState<T?>> = internalUiState.asStateFlow()

    protected val internalSnackbarMessage = MutableStateFlow<StringResource?>(null)
    val snackbarMessage: StateFlow<StringResource?> = internalSnackbarMessage.asStateFlow()

    protected abstract var id : Uuid
    protected var originalItem by mutableStateOf<T?>(null)

    val canSave by derivedStateOf { item.name.isNotBlank() }
    val isAdding by derivedStateOf { originalItem == null }

    protected val internalIsSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = internalIsSaved.asStateFlow()

    protected abstract var item: T

    var kiloCaloriesStr by mutableStateOf("")
    var kiloJouleStr by mutableStateOf("")
    var fatInGramsStr by mutableStateOf("")
    var saturatedFattyAcidsInGramsStr by mutableStateOf("")
    var carbsInGramsStr by mutableStateOf("")
    var sugarInGramsStr by mutableStateOf("")
    var dietaryFiberInGramsStr by mutableStateOf("")
    var proteinInGramsStr by mutableStateOf("")
    var saltInGramsStr by mutableStateOf("")
    var fillingQuantityStr by mutableStateOf("")

    protected fun updateStringsFrom(product: Product) {
        kiloCaloriesStr = product.kiloCalories?.toString() ?: ""
        kiloJouleStr = product.kiloJoule?.toString() ?: ""
        fatInGramsStr = product.fatInGrams?.toString() ?: ""
        saturatedFattyAcidsInGramsStr = product.saturatedFattyAcidsInGrams?.toString() ?: ""
        carbsInGramsStr = product.carbsInGrams?.toString() ?: ""
        sugarInGramsStr = product.sugarInGrams?.toString() ?: ""
        dietaryFiberInGramsStr = product.dietaryFiberInGrams?.toString() ?: ""
        proteinInGramsStr = product.proteinInGrams?.toString() ?: ""
        saltInGramsStr = product.saltInGrams?.toString() ?: ""
        fillingQuantityStr = product.fillingQuantity?.toString() ?: ""
    }

    protected abstract fun syncItemFromStrings()

    abstract fun updateName(name: String)

    abstract fun updateIsLiquid(isLiquid: Boolean)

    fun updateKiloCalories(value: String) {
        kiloCaloriesStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun updateKiloJoule(value: String) {
        kiloJouleStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun updateFatInGrams(value: String) {
        fatInGramsStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun updateSaturatedFattyAcidsInGrams(value: String) {
        saturatedFattyAcidsInGramsStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun updateCarbsInGrams(value: String) {
        carbsInGramsStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun updateSugarInGrams(value: String) {
        sugarInGramsStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun updateDietaryFiberInGrams(value: String) {
        dietaryFiberInGramsStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun updateProteinInGrams(value: String) {
        proteinInGramsStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun updateSaltInGrams(value: String) {
        saltInGramsStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun updateFillingQuantity(value: String) {
        fillingQuantityStr = value
        syncItemFromStrings()
        internalUiState.value = UiState.Companion.success(item)
    }

    fun cancelEditing() {
        goBack()
    }

    fun clearSnackbar() {
        internalSnackbarMessage.value = null
    }

    fun goBack() {
        navigator.goBackFood()
    }

    fun openOcrCamera(type: OcrType) {
        navigator.goToOcrCamera(type = type) { text ->
            handleOcrResult(type, text)
        }
    }

    private fun handleOcrResult(type: OcrType, text: String) {
        when (type) {
            OcrType.NAME -> updateName(text.trim())
            OcrType.AMOUNT -> updateOcrAmount(text)
            OcrType.NUTRIENTS -> updateOcrNutrients(text)
            OcrType.DATE -> updateOcrDate(text)
        }
    }

    private fun updateOcrAmount(text: String) {
        val amountRegex = """(\d+[,.]?\d*)""".toRegex()
        val match = amountRegex.find(text)
        match?.value?.replace(',', '.')?.let { fillingQuantityStr = it }

        if (text.contains("l", ignoreCase = true) || text.contains("ml", ignoreCase = true)) {
            updateIsLiquid(true)
        } else if (text.contains("g", ignoreCase = true) || text.contains("kg", ignoreCase = true)) {
            updateIsLiquid(false)
        }
        syncItemFromStrings()
        internalUiState.value = UiState.success(item)
    }

    private fun updateOcrNutrients(text: String) {
        parseNutrientsFromOcr(text)
        syncItemFromStrings()
        internalUiState.value = UiState.success(item)
    }

    open fun updateOcrDate(text: String) {
    }

    protected val kcal = "kcal"
    protected val kj = "kj"
    protected val saturatedFatCandidates = listOf("sat.fat", "sat fat", "saturated", "gesättigt")
    protected val fatCandidates = listOf("fat", "fett")
    protected val sugarCandidates = listOf("sugar", "zucker")
    protected val carbsCandidates = listOf("carb", "kohlenhydrat")
    protected val proteinCandidates = listOf("protein", "eiweiß")
    protected val saltCandidates = listOf("salt", "salz")
    protected val fiberCandidates = listOf("fiber", "ballaststoff")

    protected fun parseNutrientsFromOcr(text: String) {
        val lines = text.lines()
        lines.forEach { line ->
            val lowerLine = line.lowercase()
            val values = """(\d+[,.]?\d*)""".toRegex().findAll(line).map { it.value.replace(',', '.') }.toList()

            if (values.isNotEmpty()) {
                when {
                    lowerLine.contains(kcal) -> kiloCaloriesStr = values.last()
                    lowerLine.contains(kj) -> kiloJouleStr = values.first()
                    saturatedFatCandidates.any { lowerLine.contains(it) } -> saturatedFattyAcidsInGramsStr = values.last()
                    fatCandidates.any { lowerLine.contains(it) } -> fatInGramsStr = values.first()
                    sugarCandidates.any { lowerLine.contains(it) } -> sugarInGramsStr = values.last()
                    carbsCandidates.any { lowerLine.contains(it) } -> carbsInGramsStr = values.first()
                    proteinCandidates.any { lowerLine.contains(it) } -> proteinInGramsStr = values.last()
                    saltCandidates.any { lowerLine.contains(it) } -> saltInGramsStr = values.last()
                    fiberCandidates.any { lowerLine.contains(it) } -> dietaryFiberInGramsStr = values.last()
                }
            }
        }
    }
}