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

    fun save() {
        if (!canSave) {
            return
        }

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
                _uiState.value = UiState.error(Res.string.food_detail_error_saving)
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
                _uiState.value = UiState.error(Res.string.food_detail_error_loading)
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
        food = food.copy(kiloCalories = value.toIntOrNull())
        _uiState.value = UiState.success(food)
    }

    fun updateKiloJoule(value: String) {
        food = food.copy(kiloJoule = value.toIntOrNull())
        _uiState.value = UiState.success(food)
    }

    fun updateFatInGrams(value: String) {
        food = food.copy(fatInGrams = value.toFloatOrNull())
        _uiState.value = UiState.success(food)
    }

    fun updateSaturatedFattyAcidsInGrams(value: String) {
        food = food.copy(saturatedFattyAcidsInGrams = value)
        _uiState.value = UiState.success(food)
    }

    fun updateCarbsInGrams(value: String) {
        food = food.copy(carbsInGrams = value.toIntOrNull())
        _uiState.value = UiState.success(food)
    }

    fun updateSugarInGrams(value: String) {
        food = food.copy(sugarInGrams = value.toFloatOrNull())
        _uiState.value = UiState.success(food)
    }

    fun updateDietaryFiberInGrams(value: String) {
        food = food.copy(dietaryFiberInGrams = value.toFloatOrNull())
        _uiState.value = UiState.success(food)
    }

    fun updateProteinInGrams(value: String) {
        food = food.copy(proteinInGrams = value.toFloatOrNull())
        _uiState.value = UiState.success(food)
    }

    fun updateSaltInGrams(value: String) {
        food = food.copy(saltInGrams = value.toFloatOrNull())
        _uiState.value = UiState.success(food)
    }

    fun updateAmount(value: String) {
        food = food.copy(amount = value.toFloatOrNull())
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
        food = originalFood?.copy() ?: Food(
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
                // Heuristic for nutrition table parsing
                val lines = text.lines()
                lines.forEach { line ->
                    val lowerLine = line.lowercase()
                    val value = """(\d+[,.]?\d*)""".toRegex().find(line)?.value?.replace(',', '.') ?: ""
                    
                    when {
                        lowerLine.contains("kcal") || lowerLine.contains("energy") || lowerLine.contains("brennwert") -> {
                             // This might find kJ first or kcal. Usually kJ/kcal are both present.
                             if (lowerLine.contains("kcal")) updateKiloCalories(value)
                             else if (lowerLine.contains("kj")) updateKiloJoule(value)
                        }
                        lowerLine.contains("fat") || lowerLine.contains("fett") -> {
                            if (lowerLine.contains("saturat") || lowerLine.contains("gesättigt")) {
                                updateSaturatedFattyAcidsInGrams(value)
                            } else {
                                updateFatInGrams(value)
                            }
                        }
                        lowerLine.contains("carb") || lowerLine.contains("kohlenhydrat") -> {
                            if (lowerLine.contains("sugar") || lowerLine.contains("zucker")) {
                                updateSugarInGrams(value)
                            } else {
                                updateCarbsInGrams(value)
                            }
                        }
                        lowerLine.contains("protein") || lowerLine.contains("eiweiß") -> updateProteinInGrams(value)
                        lowerLine.contains("salt") || lowerLine.contains("salz") -> updateSaltInGrams(value)
                        lowerLine.contains("fiber") || lowerLine.contains("ballaststoff") -> updateDietaryFiberInGrams(value)
                    }
                }
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
