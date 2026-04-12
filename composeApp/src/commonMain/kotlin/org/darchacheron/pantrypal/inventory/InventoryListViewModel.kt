package org.darchacheron.pantrypal.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.food.Food
import org.darchacheron.pantrypal.food.FoodRepository
import org.darchacheron.pantrypal.food.Message
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.ui.UiState
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

    private val _sortOrder = MutableStateFlow(InventorySortOrder.Name)
    val sortOrder: StateFlow<InventorySortOrder> = _sortOrder.asStateFlow()

    private val _sortDirection = MutableStateFlow(InventorySortDirection.Ascending)
    val sortDirection: StateFlow<InventorySortDirection> = _sortDirection.asStateFlow()

    private val _messages = MutableStateFlow<Message?>(null)
    val messages: StateFlow<Message?> = _messages

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
                combine(
                    _searchQuery,
                    _sortOrder,
                    _sortDirection
                ) { query, sort, direction ->
                    DataParams(query, sort, direction, uuid)
                }.flatMapLatest { params ->
                    inventoryRepository.getFilteredAndSorted(
                        params.profileId,
                        params.query,
                        params.sort,
                        params.direction
                    )
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

    private data class DataParams(
        val query: String,
        val sort: InventorySortOrder,
        val direction: InventorySortDirection,
        val profileId: Uuid
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSort(order: InventorySortOrder, direction: InventorySortDirection) {
        _sortOrder.value = order
        _sortDirection.value = direction
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
                    image = item.image,
                    additionalImages = item.additionalImages
                )
                foodRepository.upsert(food)
                // Optionally navigate to food detail to let user set dates
                navigator.goToFoodDetail(food.id.toString())
            } catch (e: Exception) {
                Logger.withTag(loggerTag).e { "Error adding to food list: ${e.message}" }
            }
        }
    }

    fun clearMessage() {
        _messages.value = null
    }

    fun goToItemDetails(itemId: String? = null) {
        navigator.goToInventoryDetail(itemId)
    }

    fun goToSettings() {
        navigator.goToSettings()
    }
}
