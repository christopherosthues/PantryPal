package org.darchacheron.pantrypal.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.food.Food
import org.darchacheron.pantrypal.food.FoodRepository
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.ui.UiState
import org.jetbrains.compose.resources.StringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.food_list_error_loading
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryListViewModel(
    private val inventoryRepository: InventoryRepository,
    private val foodRepository: FoodRepository,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val loggerTag = "InventoryList"

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<List<InventoryItem>>> = authenticationPreferencesRepository.authenticationPreferencesFlow
        .map { it.localProfileId }
        .distinctUntilChanged()
        .flatMapLatest { profileId ->
            if (profileId.isBlank()) {
                flowOf(UiState.success(emptyList()))
            } else {
                val uuid = Uuid.parse(profileId)
                _searchQuery.flatMapLatest { query ->
                    inventoryRepository.getFilteredAndSorted(uuid, query)
                        .map { items -> UiState.success(items) }
                        .onStart { emit(UiState.loading()) }
                        .catch { e ->
                            Logger.withTag(loggerTag).e { "Error loading inventory: ${e.message}" }
                            emit(UiState.error(Res.string.food_list_error_loading))
                        }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.loading()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                inventoryRepository.delete(item.id)
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error deleting inventory item: ${e.message}" }
            }
        }
    }

    fun addToFoodList(item: InventoryItem) {
        viewModelScope.launch {
            try {
                val food = Food(
                    profileId = item.profileId,
                    name = item.name,
                    kiloCalories = item.kiloCalories,
                    kiloJoule = item.kiloJoule,
                    fatInGrams = item.fatInGrams,
                    saturatedFattyAcidsInGrams = item.saturatedFattyAcidsInGrams,
                    carbsInGrams = item.carbsInGrams,
                    sugarInGrams = item.sugarInGrams,
                    dietaryFiberInGrams = item.dietaryFiberInGrams,
                    proteinInGrams = item.proteinInGrams,
                    saltInGrams = item.saltInGrams,
                    fillingQuantity = item.fillingQuantity,
                    isLiquid = item.isLiquid,
                    bestBeforeUsedByDate = null,
                    isUseBy = false,
                    openedAt = null,
                    createdAt = Clock.System.now(),
                    lastModifiedAt = Clock.System.now(),
                    imagePath = item.imagePath,
                    additionalImagePaths = item.additionalImagePaths
                )
                foodRepository.upsert(food)
                // Optionally navigate to food detail to let user set dates
                navigator.goToFoodDetail(food.id.toString())
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error adding to food list: ${e.message}" }
            }
        }
    }

    fun goToSettings() {
        navigator.goToSettings()
    }
}
