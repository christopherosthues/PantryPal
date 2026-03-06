package org.darchacheron.pantrypal.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.ui.UiState
import org.jetbrains.compose.resources.StringResource
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.food_list_card_consume_error
import pantrypal.composeapp.generated.resources.food_list_card_consume_success
import pantrypal.composeapp.generated.resources.food_list_card_copy_error
import pantrypal.composeapp.generated.resources.food_list_card_copy_success
import pantrypal.composeapp.generated.resources.food_list_card_delete_error
import pantrypal.composeapp.generated.resources.food_list_card_delete_success
import pantrypal.composeapp.generated.resources.food_list_error_loading
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Message(
    val messageResource: StringResource,
    val parameter: String? = null
)

@OptIn(ExperimentalUuidApi::class)
class FoodListViewModel(
    private val foodRepository: FoodRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(FoodSortOrder.Name)
    val sortOrder: StateFlow<FoodSortOrder> = _sortOrder.asStateFlow()

    private val _sortDirection = MutableStateFlow(FoodSortDirection.Ascending)
    val sortDirection: StateFlow<FoodSortDirection> = _sortDirection.asStateFlow()

    private val _filter = MutableStateFlow(FoodFilter.All)
    val filter: StateFlow<FoodFilter> = _filter.asStateFlow()

    private val _messages = MutableStateFlow<Message?>(null)
    val messages: StateFlow<Message?> = _messages

    private val loggerTag = "FoodList"

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<List<Food>>> = combine(
        _searchQuery,
        _sortOrder,
        _sortDirection,
        _filter
    ) { query, sort, direction, filter ->
        DataParams(query, sort, direction, filter)
    }.flatMapLatest { params ->
        foodRepository.getFilteredAndSorted(params.query, params.filter, params.sort, params.direction)
            .map { foods -> UiState.success(foods) }
            .onStart { emit(UiState.loading()) }
            .catch { e ->
                Logger.withTag(loggerTag).e { "Error loading foods: ${e.message}" }
                emit(UiState.error(Res.string.food_list_error_loading))
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.loading()
    )

    private data class DataParams(
        val query: String,
        val sort: FoodSortOrder,
        val direction: FoodSortDirection,
        val filter: FoodFilter
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSort(order: FoodSortOrder, direction: FoodSortDirection) {
        _sortOrder.value = order
        _sortDirection.value = direction
    }

    fun setFilter(filter: FoodFilter) {
        _filter.value = filter
    }

    fun goToFoodDetail(foodId: String? = null) {
        navigator.goToFoodDetail(foodId)
    }

    fun goToSettings() {
        navigator.goToSettings()
    }

    fun clearMessage() {
        _messages.value = null
    }

    fun copyFood(food: Food) {
        viewModelScope.launch {
            try {
                foodRepository.upsert(
                    food.copy(
                        id = Uuid.generateV7(),
                        createdAt = Clock.System.now(),
                        lastModifiedAt = Clock.System.now()
                    )
                )
                _messages.value = Message(Res.string.food_list_card_copy_success, food.name)
            } catch (exception: Exception) {
                Logger.withTag(loggerTag).e { "Error copying food: ${exception.message}" }
                _messages.value = Message(Res.string.food_list_card_copy_error, food.name)
            }
        }
    }

    fun deleteFood(food: Food) {
        viewModelScope.launch {
            try {
                foodRepository.delete(food.id)
                _messages.value = Message(Res.string.food_list_card_delete_success, food.name)
            } catch (exception: Exception) {
                Logger.withTag(loggerTag).e { "Error deleting food: ${exception.message}" }
                _messages.value = Message(Res.string.food_list_card_delete_error, food.name)
            }
        }
    }

    fun consumeFood(food: Food) {
        val food = food.copy(amount = food.amount - 1)
        viewModelScope.launch {
            try {
                if (food.amount >= 1) {
                    foodRepository.upsert(food)
                } else {
                    foodRepository.delete(food.id)
                }
                _messages.value = Message(Res.string.food_list_card_consume_success, food.name)
            } catch (exception: Exception) {
                Logger.withTag(loggerTag).e { "Error deleting food: ${exception.message}" }
                _messages.value = Message(Res.string.food_list_card_consume_error, food.name)
            }
        }
    }
}
