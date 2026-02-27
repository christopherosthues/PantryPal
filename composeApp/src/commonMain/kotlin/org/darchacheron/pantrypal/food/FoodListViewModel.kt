package org.darchacheron.pantrypal.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.darchacheron.pantrypal.navigation.Navigator
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.food_list_error_loading

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
                Logger.withTag("FoodList").e { "Error loading foods: ${e.message}" }
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
}
