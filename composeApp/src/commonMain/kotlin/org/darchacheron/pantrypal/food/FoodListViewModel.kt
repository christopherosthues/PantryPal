package org.darchacheron.pantrypal.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.food_list_error_loading

class FoodListViewModel(foodRepository: FoodRepository) : ViewModel() {

    val uiState: StateFlow<UiState<List<Food>>> = foodRepository.getAll()
        .map { foods -> UiState.success(foods) }
        .onStart { emit(UiState.loading()) }
        .catch { emit(UiState.error(Res.string.food_list_error_loading)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.loading()
        )
}
