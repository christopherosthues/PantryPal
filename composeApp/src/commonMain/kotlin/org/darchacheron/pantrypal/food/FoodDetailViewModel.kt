package org.darchacheron.pantrypal.food

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.byUnicodePattern
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.camera.Image
import org.darchacheron.pantrypal.navigation.FoodNavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.navigation.OcrType
import org.darchacheron.pantrypal.ui.NutrientViewModel
import org.darchacheron.pantrypal.ui.UiState
import org.jetbrains.compose.resources.StringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.food_detail_delete_error
import pantrypal.composeapp.generated.resources.food_detail_delete_success
import pantrypal.composeapp.generated.resources.food_detail_error_loading
import pantrypal.composeapp.generated.resources.food_detail_error_saving
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodDetailViewModel(
    val navigationRoute: FoodNavRoute.FoodDetail,
    private val foodRepository: FoodRepository,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val navigator: Navigator,
) : NutrientViewModel() {
    var foodId by mutableStateOf(if (navigationRoute.foodId != null) Uuid.parse(navigationRoute.foodId) else Uuid.generateV7())

    val canSave by derivedStateOf { food.name.isNotBlank() }

    val isAdding by derivedStateOf { originalFood == null }

    private var food by mutableStateOf(
        Food(
            id = foodId,
            profileId = Uuid.NIL,
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
            fillingQuantity = null,
            isLiquid = false,
            bestBeforeUsedByDate = null,
            isUseBy = false,
            openedAt = null,
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now(),
            image = null,
            additionalImages = emptyList()
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

    private val germanDateFormat = LocalDate.Format {
        byUnicodePattern("dd.MM.yyyy")
    }

    init {
        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                val preferences = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
                val currentProfileId = if (preferences.localProfileId.isNotBlank()) {
                    Uuid.parse(preferences.localProfileId)
                } else {
                    null
                }

                if (currentProfileId == null) {
                    _uiState.value = UiState.error(Res.string.food_detail_error_loading)
                    return@launch
                }

                if (navigationRoute.foodId != null) {
                    val loadedFood = foodRepository.getById(foodId)
                    if (loadedFood != null) {
                        food = loadedFood
                        originalFood = loadedFood.copy()
                        updateStringsFromFood(loadedFood)
                        _uiState.value = UiState.success(food)
                    } else {
                        Logger.withTag(foodDetailLoggerTag).e { "Error loading food: $foodId" }
                        _uiState.value = UiState.error(Res.string.food_detail_error_loading)
                    }
                } else {
                    food = food.copy(profileId = currentProfileId)
                    _uiState.value = UiState.success(food)
                }
            } catch (e: Exception) {
                Logger.withTag(foodDetailLoggerTag).e { "Error loading food: ${e.message}" }
                _uiState.value = UiState.error(Res.string.food_detail_error_loading)
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
        fillingQuantityStr = food.fillingQuantity?.toString() ?: ""
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
            fillingQuantity = fillingQuantityStr.replace(',', '.').toFloatOrNull()
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
                goBack()
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

    fun updateName(name: String) {
        food = food.copy(name = name)
        _uiState.value = UiState.success(food)
    }

    override fun updateKiloCalories(value: String) {
        kiloCaloriesStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    override fun updateKiloJoule(value: String) {
        kiloJouleStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    override fun updateFatInGrams(value: String) {
        fatInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    override fun updateSaturatedFattyAcidsInGrams(value: String) {
        saturatedFattyAcidsInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    override fun updateCarbsInGrams(value: String) {
        carbsInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    override fun updateSugarInGrams(value: String) {
        sugarInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    override fun updateDietaryFiberInGrams(value: String) {
        dietaryFiberInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    override fun updateProteinInGrams(value: String) {
        proteinInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    override fun updateSaltInGrams(value: String) {
        saltInGramsStr = value
        syncFoodFromStrings()
        _uiState.value = UiState.success(food)
    }

    override fun updateFillingQuantity(value: String) {
        fillingQuantityStr = value
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
        goBack()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun goBack() {
        navigator.goBackFood()
    }

    fun openCamera() {
        navigator.goToSimpleCamera(
            onSuccess = { imagePath ->
                val now = Clock.System.now()
                food = food.copy(
                    image = Image(
                        profileId = food.profileId,
                        localPath = imagePath,
                        createdAt = now,
                        lastModifiedAt = now
                    )
                )
                _uiState.value = UiState.success(food)
            }
        )
    }

    fun removePrimaryImage() {
        food = food.copy(image = null)
        _uiState.value = UiState.success(food)
    }

    fun addAdditionalImage() {
        navigator.goToSimpleCamera { imagePath ->
            val now = Clock.System.now()
            val newImage = Image(
                profileId = food.profileId,
                localPath = imagePath,
                createdAt = now,
                lastModifiedAt = now
            )
            food = food.copy(additionalImages = food.additionalImages + newImage)
            _uiState.value = UiState.success(food)
        }
    }

    fun removeAdditionalImage(image: Image) {
        food = food.copy(additionalImages = food.additionalImages - image)
        _uiState.value = UiState.success(food)
    }

    override fun openOcrCamera(type: OcrType) {
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
                val amountRegex = """(\d+[,.]?\d*)""".toRegex()
                val match = amountRegex.find(text)
                match?.value?.replace(',', '.')?.let { fillingQuantityStr = it }
                
                if (text.contains("l", ignoreCase = true) || text.contains("ml", ignoreCase = true)) {
                    updateIsLiquid(true)
                } else if (text.contains("g", ignoreCase = true) || text.contains("kg", ignoreCase = true)) {
                    updateIsLiquid(false)
                }
                syncFoodFromStrings()
                _uiState.value = UiState.success(food)
            }
            OcrType.NUTRIENTS -> {
                parseNutrientsFromOcr(text)
                syncFoodFromStrings()
                _uiState.value = UiState.success(food)
            }
            OcrType.DATE -> {
                val trimmedText = text.trim()
                val date = try {
                    LocalDate.parse(trimmedText)
                } catch (e: IllegalArgumentException) {
                    try {
                        LocalDate.parse(trimmedText, germanDateFormat)
                    } catch (e2: IllegalArgumentException) {
                        null
                    }
                }

                if (date != null) {
                    updateBestBeforeUsedByDate(date)
                } else {
                    Logger.withTag(foodDetailLoggerTag).w { "Error parsing date: $text" }
                }
            }
        }
    }
}
