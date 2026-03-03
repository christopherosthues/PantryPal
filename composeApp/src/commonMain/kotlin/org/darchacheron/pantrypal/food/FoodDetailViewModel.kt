package org.darchacheron.pantrypal.food

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.darchacheron.pantrypal.navigation.NavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.navigation.OcrType
import org.darchacheron.pantrypal.ui.UiState
import org.jetbrains.compose.resources.StringResource
import pantrypal.composeapp.generated.resources.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodDetailViewModel(
    val navigationRoute: NavRoute.FoodDetail,
    private val foodRepository: FoodRepository,
    private val navigator: Navigator,
) : ViewModel() {
    var foodId by mutableStateOf(if (navigationRoute.foodId != null) Uuid.parse(navigationRoute.foodId) else Uuid.generateV7())

    var isEditing by mutableStateOf(navigationRoute.foodId == null)

    val canSave by derivedStateOf { isEditing && food.name.isNotBlank() }

    val isAdding by derivedStateOf { originalFood == null }

    var kiloCaloriesStr by mutableStateOf("")
    var kiloJouleStr by mutableStateOf("")
    var fatInGramsStr by mutableStateOf("")
    var saturatedFattyAcidsInGramsStr by mutableStateOf("")
    var carbsInGramsStr by mutableStateOf("")
    var sugarInGramsStr by mutableStateOf("")
    var dietaryFiberInGramsStr by mutableStateOf("")
    var proteinInGramsStr by mutableStateOf("")
    var saltInGramsStr by mutableStateOf("")
    var amountStr by mutableStateOf("")

    private var food by mutableStateOf(
        Food(
            id = foodId,
            name = "",
            kiloCalories = null,
            kiloJoule = null,
            carbsInGrams = null,
            sugarInGrams = null,
            fatInGrams = null,
            saturatedFattyAcidsInGrams = null,
            proteinInGrams = null,
            dietaryFiberInGrams = null,
            saltInGrams = null,
            amount = null,
            isLiquid = false,
            bestBeforeUsedByDate = null,
            isUseBy = false,
            openedAt = null,
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now(),
            imagePath = null,
            additionalImagePaths = emptyList()
        )
    )

    private var originalFood by mutableStateOf<Food?>(null)

    private val _uiState = MutableStateFlow(UiState<Food?>())
    val uiState: StateFlow<UiState<Food?>> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<StringResource?>(null)
    val snackbarMessage: StateFlow<StringResource?> = _snackbarMessage.asStateFlow()

    private val foodDetailLoggerTag = "FoodDetail"

    init {
        foodId.let {
            viewModelScope.launch {
                _uiState.value = UiState.loading()
                try {
                    if (navigationRoute.foodId != null) {
                        val loadedFood = foodRepository.getById(it)
                        if (loadedFood != null) {
                            food = loadedFood
                            originalFood = loadedFood.copy()
                            updateStringsFromFood(loadedFood)
                            _uiState.value = UiState.success(food)
                        } else {
                            Logger.withTag(foodDetailLoggerTag).e { "Error loading food: $it" }
                            _uiState.value = UiState.error(Res.string.food_detail_error_loading)
                        }
                    } else {
                        _uiState.value = UiState.success(food)
                    }
                } catch (e: Exception) {
                    Logger.withTag(foodDetailLoggerTag).e { "Error loading food: ${e.message}" }
                    _uiState.value = UiState.error(Res.string.food_detail_error_loading)
                }
            }
        }
    }

    private fun updateStringsFromFood(food: Food) {
        kiloCaloriesStr = food.kiloCalories?.toString() ?: ""
        kiloJouleStr = food.kiloJoule?.toString() ?: ""
        fatInGramsStr = food.fatInGrams?.toString() ?: ""
        saturatedFattyAcidsInGramsStr = food.saturatedFattyAcidsInGrams?.toString() ?: ""
        carbsInGramsStr = food.carbsInGrams?.toString() ?: ""
        sugarInGramsStr = food.sugarInGrams?.toString() ?: ""
        dietaryFiberInGramsStr = food.dietaryFiberInGrams?.toString() ?: ""
        proteinInGramsStr = food.proteinInGrams?.toString() ?: ""
        saltInGramsStr = food.saltInGrams?.toString() ?: ""
        amountStr = food.amount?.toString() ?: ""
    }

    private fun syncFoodFromStrings() {
        food = food.copy(
            kiloCalories = kiloCaloriesStr.toIntOrNull(),
            kiloJoule = kiloJouleStr.toIntOrNull(),
            fatInGrams = fatInGramsStr.replace(',', '.').toFloatOrNull(),
            saturatedFattyAcidsInGrams = saturatedFattyAcidsInGramsStr.replace(',', '.').toFloatOrNull(),
            carbsInGrams = carbsInGramsStr.replace(',', '.').toFloatOrNull(),
            sugarInGrams = sugarInGramsStr.replace(',', '.').toFloatOrNull(),
            dietaryFiberInGrams = dietaryFiberInGramsStr.replace(',', '.').toFloatOrNull(),
            proteinInGrams = proteinInGramsStr.replace(',', '.').toFloatOrNull(),
            saltInGrams = saltInGramsStr.replace(',', '.').toFloatOrNull(),
            amount = amountStr.replace(',', '.').toFloatOrNull()
        )
    }

    fun save() {
        if (!canSave) {
            return
        }

        syncFoodFromStrings()

        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                val createdAt = originalFood?.createdAt ?: Clock.System.now()
                val lastModifiedAt = Clock.System.now()
                foodRepository.upsert(food.copy(createdAt = createdAt, lastModifiedAt = lastModifiedAt))
                _isSaved.value = true
                setIsEditing(false)
                if (originalFood == null) {
                    navigator.goToFoodDetail(foodId.toString())
                } else {
                    originalFood = foodRepository.getById(foodId)
                    _uiState.value = UiState.success(food)
                }
            } catch (e: Exception) {
                Logger.withTag(foodDetailLoggerTag).e { "Error saving food: ${e.message}" }
                _uiState.value = UiState.error(_uiState.value, Res.string.food_detail_error_saving)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                foodRepository.delete(id = foodId)
                _snackbarMessage.value = Res.string.food_detail_delete_success
                _isSaved.value = true
                _uiState.value = UiState.success(null)
            } catch (e: Exception) {
                Logger.withTag(foodDetailLoggerTag).e { "Error deleting food: ${e.message}" }
                _uiState.value = UiState.error(_uiState.value, Res.string.food_detail_error_loading)
                _snackbarMessage.value = Res.string.food_detail_delete_error
            }
        }
    }

    fun setIsEditing(isEditing: Boolean) {
        this.isEditing = isEditing
    }

    fun updateName(name: String) {
        food = food.copy(name = name)
        _uiState.value = UiState.success(food)
    }

    fun updateKiloCalories(value: String) {
        kiloCaloriesStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateKiloJoule(value: String) {
        kiloJouleStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateFatInGrams(value: String) {
        fatInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateSaturatedFattyAcidsInGrams(value: String) {
        saturatedFattyAcidsInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateCarbsInGrams(value: String) {
        carbsInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateSugarInGrams(value: String) {
        sugarInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateDietaryFiberInGrams(value: String) {
        dietaryFiberInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateProteinInGrams(value: String) {
        proteinInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateSaltInGrams(value: String) {
        saltInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateAmount(value: String) {
        amountStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    fun updateIsLiquid(isLiquid: Boolean) {
        food = food.copy(isLiquid = isLiquid)
        _uiState.value = UiState.success(food)
    }

    fun updateBestBeforeUsedByDate(date: LocalDate?) {
        food = food.copy(bestBeforeUsedByDate = date)
        _uiState.value = UiState.success(food)
    }

    fun updateIsUseBy(isUseBy: Boolean) {
        food = food.copy(isUseBy = isUseBy)
        _uiState.value = UiState.success(food)
    }

    fun updateOpenedAt(date: LocalDate?) {
        food = food.copy(openedAt = date)
        _uiState.value = UiState.success(food)
    }

    fun cancelEditing() {
        val resetFood = originalFood?.copy() ?: Food(
            id = foodId,
            name = "",
            kiloCalories = null,
            kiloJoule = null,
            carbsInGrams = null,
            sugarInGrams = null,
            fatInGrams = null,
            saturatedFattyAcidsInGrams = null,
            proteinInGrams = null,
            dietaryFiberInGrams = null,
            saltInGrams = null,
            amount = null,
            isLiquid = false,
            bestBeforeUsedByDate = null,
            isUseBy = false,
            openedAt = null,
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now(),
            imagePath = null,
            additionalImagePaths = emptyList()
        )
        food = resetFood
        updateStringsFromFood(resetFood)
        _uiState.value = UiState.success(food)
        if (navigationRoute.foodId != null) {
            setIsEditing(false)
        } else {
            goBack()
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun goBack() {
        navigator.goBack()
    }

    fun openCamera() {
        navigator.goToSimpleCamera(
            onSuccess = { imagePath ->
                food = food.copy(imagePath = imagePath)
                _uiState.value = UiState.success(food)
            }
        )
    }

    fun openOcrCamera(type: OcrType) {
        navigator.goToOcrCamera(
            type = type,
            onRecognized = { text ->
                handleOcrResult(type, text)
            }
        )
    }

    private fun handleOcrResult(type: OcrType, text: String) {
        when (type) {
            OcrType.NAME -> updateName(text.trim())
            OcrType.AMOUNT -> {
                // Extract number from text like "500g" or "1.5L"
                val amountRegex = """(\d+[,.]?\d*)""".toRegex()
                val match = amountRegex.find(text)
                match?.value?.replace(',', '.')?.let { updateAmount(it) }
                
                if (text.contains("l", ignoreCase = true) || text.contains("ml", ignoreCase = true)) {
                    updateIsLiquid(true)
                } else if (text.contains("g", ignoreCase = true) || text.contains("kg", ignoreCase = true)) {
                    updateIsLiquid(false)
                }
            }
            OcrType.NUTRIENTS -> {
                val lines = text.lines()
                lines.forEach { line ->
                    val lowerLine = line.lowercase()
                    // Extract all numbers on the line
                    val values = """(\d+[,.]?\d*)""".toRegex().findAll(line).map { it.value.replace(',', '.') }.toList()
                    
                    if (values.isNotEmpty()) {
                        when {
                            // Specific tags from the OCR UI or common names
                            lowerLine.contains("kcal") -> updateKiloCalories(values.last())
                            lowerLine.contains("kj") -> updateKiloJoule(values.first())
                            lowerLine.contains("sat.fat") || lowerLine.contains("saturated") || lowerLine.contains("gesättigt") -> 
                                updateSaturatedFattyAcidsInGrams(values.last())
                            lowerLine.contains("fat") || lowerLine.contains("fett") -> 
                                updateFatInGrams(values.first())
                            lowerLine.contains("sugar") || lowerLine.contains("zucker") -> 
                                updateSugarInGrams(values.last())
                            lowerLine.contains("carb") || lowerLine.contains("kohlenhydrat") -> 
                                updateCarbsInGrams(values.first())
                            lowerLine.contains("protein") || lowerLine.contains("eiweiß") -> 
                                updateProteinInGrams(values.first())
                            lowerLine.contains("salt") || lowerLine.contains("salz") -> 
                                updateSaltInGrams(values.first())
                            lowerLine.contains("fiber") || lowerLine.contains("ballaststoff") -> 
                                updateDietaryFiberInGrams(values.first())
                        }
                    }
                }
            }
            OcrType.DATE -> {
                val date = LocalDate.parse(text)
                updateBestBeforeUsedByDate(date)
            }
        }
    }

    fun addAdditionalImage() {
        navigator.goToSimpleCamera(
            onSuccess = { imagePath ->
                food = food.copy(additionalImagePaths = food.additionalImagePaths + imagePath)
                _uiState.value = UiState.success(food)
            }
        )
    }

    fun removeAdditionalImage(imagePath: String) {
        food = food.copy(additionalImagePaths = food.additionalImagePaths - imagePath)
        _uiState.value = UiState.success(food)
    }
}
