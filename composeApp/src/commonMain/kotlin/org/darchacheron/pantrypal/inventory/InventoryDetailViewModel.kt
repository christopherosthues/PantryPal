package org.darchacheron.pantrypal.inventory

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.navigation.OcrType
import org.darchacheron.pantrypal.ui.UiState
import org.jetbrains.compose.resources.StringResource
import pantrypal.composeapp.generated.resources.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryDetailViewModel(
    private val itemId: String?,
    private val inventoryRepository: InventoryRepository,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val id = if (itemId != null) Uuid.parse(itemId) else Uuid.generateV7()

    val canSave by derivedStateOf { item.name.isNotBlank() }
    val isAdding by derivedStateOf { itemId == null }

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

    private var item by mutableStateOf(
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
            imagePath = null,
            additionalImagePaths = emptyList()
        )
    )

    private val _uiState = MutableStateFlow(UiState<InventoryItem?>())
    val uiState: StateFlow<UiState<InventoryItem?>> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<StringResource?>(null)
    val snackbarMessage: StateFlow<StringResource?> = _snackbarMessage.asStateFlow()

    private val loggerTag = "InventoryDetail"

    init {
        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                val preferences = authenticationPreferencesRepository.authenticationPreferencesFlow.first()
                val profileId = if (preferences.localProfileId.isNotBlank()) Uuid.parse(preferences.localProfileId) else null

                if (profileId == null) {
                    _uiState.value = UiState.error(Res.string.food_detail_error_loading)
                    return@launch
                }

                if (itemId != null) {
                    val loaded = inventoryRepository.getById(id)
                    if (loaded != null) {
                        item = loaded
                        updateStringsFromItem(loaded)
                        _uiState.value = UiState.success(item)
                    } else {
                        _uiState.value = UiState.error(Res.string.food_detail_error_loading)
                    }
                } else {
                    item = item.copy(profileId = profileId)
                    _uiState.value = UiState.success(item)
                }
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error loading inventory item: ${e.message}" }
                _uiState.value = UiState.error(Res.string.food_detail_error_loading)
            }
        }
    }

    private fun updateStringsFromItem(item: InventoryItem) {
        kiloCaloriesStr = item.kiloCalories?.toString() ?: ""
        kiloJouleStr = item.kiloJoule?.toString() ?: ""
        fatInGramsStr = item.fatInGrams?.toString() ?: ""
        saturatedFattyAcidsInGramsStr = item.saturatedFattyAcidsInGrams?.toString() ?: ""
        carbsInGramsStr = item.carbsInGrams?.toString() ?: ""
        sugarInGramsStr = item.sugarInGrams?.toString() ?: ""
        dietaryFiberInGramsStr = item.dietaryFiberInGrams?.toString() ?: ""
        proteinInGramsStr = item.proteinInGrams?.toString() ?: ""
        saltInGramsStr = item.saltInGrams?.toString() ?: ""
        fillingQuantityStr = item.fillingQuantity?.toString() ?: ""
    }

    private fun syncItemFromStrings() {
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
        if (!canSave) return
        syncItemFromStrings()
        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                inventoryRepository.upsert(item.copy(lastModifiedAt = Clock.System.now()))
                navigator.goBack()
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error saving inventory item: ${e.message}" }
                _uiState.value = UiState.error(_uiState.value, Res.string.food_detail_error_saving)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                inventoryRepository.delete(id)
                navigator.goBack()
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error deleting inventory item: ${e.message}" }
                _uiState.value = UiState.error(_uiState.value, Res.string.food_detail_delete_error)
            }
        }
    }

    fun updateName(name: String) {
        item = item.copy(name = name)
        _uiState.value = UiState.success(item)
    }

    fun updateIsLiquid(isLiquid: Boolean) {
        item = item.copy(isLiquid = isLiquid)
        _uiState.value = UiState.success(item)
    }

    fun openCamera() {
        navigator.goToSimpleCamera { path ->
            item = item.copy(imagePath = path)
            _uiState.value = UiState.success(item)
        }
    }

    fun addAdditionalImage() {
        navigator.goToSimpleCamera { path ->
            item = item.copy(additionalImagePaths = item.additionalImagePaths + path)
            _uiState.value = UiState.success(item)
        }
    }

    fun removeAdditionalImage(path: String) {
        item = item.copy(additionalImagePaths = item.additionalImagePaths - path)
        _uiState.value = UiState.success(item)
    }

    fun openOcrCamera(type: OcrType) {
        navigator.goToOcrCamera(type) { text ->
            handleOcrResult(type, text)
        }
    }

    private val kcal = "kcal"
    private val kj = "kj"
    private val saturatedFatCandidates = listOf("sat.fat", "sat fat", "saturated", "gesättigt")
    private val fatCandidates = listOf("fat", "fett")
    private val sugarCandidates = listOf("sugar", "zucker")
    private val carbsCandidates = listOf("carb", "kohlenhydrat")
    private val proteinCandidates = listOf("protein", "eiweiß")
    private val saltCandidates = listOf("salt", "salz")
    private val fiberCandidates = listOf("fiber", "ballaststoff")

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
                syncItemFromStrings()
                _uiState.value = UiState.success(item)
            }
            OcrType.NUTRIENTS -> {
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
                            proteinCandidates.any { lowerLine.contains(it) } -> proteinInGramsStr = values.first()
                            saltCandidates.any { lowerLine.contains(it) } -> saltInGramsStr = values.last()
                            fiberCandidates.any { lowerLine.contains(it) } -> dietaryFiberInGramsStr = values.first()
                        }
                    }
                }
                syncItemFromStrings()
                _uiState.value = UiState.success(item)
            }
            else -> {}
        }
    }

    fun goBack() = navigator.goBack()
    fun clearSnackbar() { _snackbarMessage.value = null }
}
