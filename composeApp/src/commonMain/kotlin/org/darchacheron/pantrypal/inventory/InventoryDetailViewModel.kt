package org.darchacheron.pantrypal.inventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.camera.Image
import org.darchacheron.pantrypal.navigation.InventoryNavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.navigation.OcrType
import org.darchacheron.pantrypal.common.ProductViewModel
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.food_detail_delete_error
import pantrypal.composeapp.generated.resources.food_detail_error_loading
import pantrypal.composeapp.generated.resources.food_detail_error_saving
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryDetailViewModel(
    val navigationRoute: InventoryNavRoute.InventoryDetail,
    private val inventoryRepository: InventoryRepository,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    navigator: Navigator,
) : ProductViewModel<InventoryItem>(navigator) {
    override var id = if (navigationRoute.itemId != null) Uuid.parse(navigationRoute.itemId) else Uuid.generateV7()

    override var item by mutableStateOf(
        InventoryItem(
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
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now(),
            image = null,
            additionalImages = emptyList()
        )
    )

    private val loggerTag = "InventoryDetail"

    init {
        viewModelScope.launch {
            internalUiState.value = UiState.loading()
            try {
                val preferences = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
                val profileId = if (preferences.localProfileId.isNotBlank()) Uuid.parse(preferences.localProfileId) else null

                if (profileId == null) {
                    internalUiState.value = UiState.error(Res.string.food_detail_error_loading)
                    return@launch
                }

                if (navigationRoute.itemId != null) {
                    val loaded = inventoryRepository.getById(id)
                    if (loaded != null) {
                        item = loaded
                        originalItem = loaded.copy()
                        updateStringsFrom(loaded)
                        internalUiState.value = UiState.success(item)
                    } else {
                        internalUiState.value = UiState.error(Res.string.food_detail_error_loading)
                    }
                } else {
                    item = item.copy(profileId = profileId)
                    internalUiState.value = UiState.success(item)
                }
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error loading inventory item: ${e.message}" }
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
        if (!canSave){
            return
        }

        syncItemFromStrings()

        viewModelScope.launch {
            internalUiState.value = UiState.loading()
            try {
                val createdAt = originalItem?.createdAt ?: Clock.System.now()
                val lastModifiedAt = Clock.System.now()
                inventoryRepository.upsert(item.copy(createdAt = createdAt, lastModifiedAt = lastModifiedAt))
                internalIsSaved.value = true

                if (originalItem == null) {
                    navigator.goToInventoryDetail(id.toString())
                } else {
                    originalItem = inventoryRepository.getById(id)
                    internalUiState.value = UiState.success(item)
                }
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error saving inventory item: ${e.message}" }
                internalUiState.value = UiState.error(internalUiState.value, Res.string.food_detail_error_saving)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            internalUiState.value = UiState.loading()
            try {
                inventoryRepository.delete(id)
                internalIsSaved.value = true
                goBack()
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error deleting inventory item: ${e.message}" }
                internalUiState.value = UiState.error(internalUiState.value, Res.string.food_detail_delete_error)
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

    fun openCamera() {
        navigator.goToSimpleCamera { path ->
            val now = Clock.System.now()
            item = item.copy(
                image = Image(
                    profileId = item.profileId,
                    localPath = path,
                    createdAt = now,
                    lastModifiedAt = now
                )
            )
            internalUiState.value = UiState.success(item)
        }
    }

    fun removePrimaryImage() {
        item = item.copy(image = null)
        internalUiState.value = UiState.success(item)
    }

    fun addAdditionalImage() {
        navigator.goToSimpleCamera { path ->
            val now = Clock.System.now()
            val newImage = Image(
                profileId = item.profileId,
                localPath = path,
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
}
