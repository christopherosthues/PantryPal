package org.darchacheron.pantrypal.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.byUnicodePattern
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.camera.Image
import org.darchacheron.pantrypal.navigation.FoodNavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.navigation.OcrType
import org.darchacheron.pantrypal.common.ProductViewModel
import org.darchacheron.pantrypal.ui.UiState
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
    navigator: Navigator,
) : ProductViewModel<Food>(navigator) {
    override var id by mutableStateOf(if (navigationRoute.foodId != null) Uuid.parse(navigationRoute.foodId) else Uuid.generateV7())

    override var item by mutableStateOf(
        Food(
            id = id,
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

    private val foodDetailLoggerTag = "FoodDetail"

    private val germanDateFormat = LocalDate.Format {
        byUnicodePattern("dd.MM.yyyy")
    }

    init {
        viewModelScope.launch {
            internalUiState.value = UiState.loading()
            try {
                val preferences = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
                val currentProfileId = if (preferences.localProfileId.isNotBlank()) {
                    Uuid.parse(preferences.localProfileId)
                } else {
                    null
                }

                if (currentProfileId == null) {
                    internalUiState.value = UiState.error(Res.string.food_detail_error_loading)
                    return@launch
                }

                if (navigationRoute.foodId != null) {
                    val loadedFood = foodRepository.getById(id)
                    if (loadedFood != null) {
                        item = loadedFood
                        originalItem = loadedFood.copy()
                        updateStringsFrom(loadedFood)
                        internalUiState.value = UiState.success(item)
                    } else {
                        Logger.withTag(foodDetailLoggerTag).e { "Error loading food: $id" }
                        internalUiState.value = UiState.error(Res.string.food_detail_error_loading)
                    }
                } else {
                    item = item.copy(profileId = currentProfileId)
                    internalUiState.value = UiState.success(item)
                }
            } catch (e: Exception) {
                Logger.withTag(foodDetailLoggerTag).e { "Error loading food: ${e.message}" }
                internalUiState.value = UiState.error(Res.string.food_detail_error_loading)
            }
        }
    }

    override fun syncItemFromStrings() {
        item = item.copy(
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

        syncItemFromStrings()

        viewModelScope.launch {
            internalUiState.value = UiState.loading()
            try {
                val createdAt = originalItem?.createdAt ?: Clock.System.now()
                val lastModifiedAt = Clock.System.now()
                foodRepository.upsert(item.copy(createdAt = createdAt, lastModifiedAt = lastModifiedAt))
                internalIsSaved.value = true

                if (originalItem == null) {
                    navigator.goToFoodDetail(id.toString())
                } else {
                    originalItem = foodRepository.getById(id)
                    internalUiState.value = UiState.success(item)
                }

            } catch (e: Exception) {
                Logger.withTag(foodDetailLoggerTag).e { "Error saving food: ${e.message}" }
                internalUiState.value = UiState.error(internalUiState.value, Res.string.food_detail_error_saving)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            internalUiState.value = UiState.loading()
            try {
                foodRepository.delete(id = id)
                internalSnackbarMessage.value = Res.string.food_detail_delete_success
                internalIsSaved.value = true
                internalUiState.value = UiState.success(null)
            } catch (e: Exception) {
                Logger.withTag(foodDetailLoggerTag).e { "Error deleting food: ${e.message}" }
                internalUiState.value = UiState.error(internalUiState.value, Res.string.food_detail_error_loading)
                internalSnackbarMessage.value = Res.string.food_detail_delete_error
            }
        }
    }

    override fun updateName(name: String) {
        item = item.copy(name = name)
        internalUiState.value = UiState.success(item)
    }

    override fun updateIsLiquid(isLiquid: Boolean) {
        item = item.copy(isLiquid = isLiquid)
        internalUiState.value = UiState.success(item)
    }

    fun updateBestBeforeUsedByDate(date: LocalDate?) {
        item = item.copy(bestBeforeUsedByDate = date)
        internalUiState.value = UiState.success(item)
    }

    fun updateIsUseBy(isUseBy: Boolean) {
        item = item.copy(isUseBy = isUseBy)
        internalUiState.value = UiState.success(item)
    }

    fun updateOpenedAt(date: LocalDate?) {
        item = item.copy(openedAt = date)
        internalUiState.value = UiState.success(item)
    }

    fun openCamera() {
        navigator.goToSimpleCamera(
            onSuccess = { imagePath ->
                val now = Clock.System.now()
                item = item.copy(
                    image = Image(
                        profileId = item.profileId,
                        localPath = imagePath,
                        createdAt = now,
                        lastModifiedAt = now
                    )
                )
                internalUiState.value = UiState.success(item)
            }
        )
    }

    fun removePrimaryImage() {
        item = item.copy(image = null)
        internalUiState.value = UiState.success(item)
    }

    fun addAdditionalImage() {
        navigator.goToSimpleCamera { imagePath ->
            val now = Clock.System.now()
            val newImage = Image(
                profileId = item.profileId,
                localPath = imagePath,
                createdAt = now,
                lastModifiedAt = now
            )
            item = item.copy(additionalImages = item.additionalImages + newImage)
            internalUiState.value = UiState.success(item)
        }
    }

    fun removeAdditionalImage(image: Image) {
        item = item.copy(additionalImages = item.additionalImages - image)
        internalUiState.value = UiState.success(item)
    }

    override fun updateOcrDate(text: String) {
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
