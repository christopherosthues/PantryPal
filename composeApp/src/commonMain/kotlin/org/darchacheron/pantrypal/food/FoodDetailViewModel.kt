package org.darchacheron.pantrypal.food

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.darchacheron.pantrypal.ui.UiState
import pantrypal.composeapp.generated.resources.Res
import pantrypal.composeapp.generated.resources.food_detail_error_loading
import pantrypal.composeapp.generated.resources.food_detail_error_saving
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodDetailViewModel(private val foodRepository: FoodRepository) : ViewModel() {

    private var foodId: Uuid? = null

    var name by mutableStateOf("")
    var calories by mutableStateOf("")
    var carbs by mutableStateOf("")
    var fat by mutableStateOf("")
    var protein by mutableStateOf("")
    var weight by mutableStateOf("")
    var bestBeforeDate by mutableStateOf<LocalDate?>(null)
    var useByDate by mutableStateOf<LocalDate?>(null)
    var openedAt by mutableStateOf<LocalDate?>(null)

    private val _uiState = MutableStateFlow(UiState<Food?>())
    val uiState: StateFlow<UiState<Food?>> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun loadFood(id: String?) {
        if (id == null) {
            foodId = null
            resetFields()
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                val uuid = Uuid.parse(id)
                val food = foodRepository.getById(uuid)
                if (food != null) {
                    foodId = food.id
                    name = food.name
                    calories = food.calories.toString()
                    carbs = food.carbs.toString()
                    fat = food.fat.toString()
                    protein = food.protein.toString()
                    weight = food.weightInGrams.toString()
                    bestBeforeDate = food.bestBeforeDate
                    useByDate = food.useByDate
                    openedAt = food.openedAt
                    _uiState.value = UiState.success(null)
                } else {
                    _uiState.value = UiState.error(Res.string.food_detail_error_loading)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.error(Res.string.food_detail_error_loading)
            }
        }
    }

    private fun resetFields() {
        name = ""
        calories = ""
        carbs = ""
        fat = ""
        protein = ""
        weight = ""
        bestBeforeDate = null
        useByDate = null
        openedAt = null
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                val now = Clock.System.now()
                val food = Food(
                    id = foodId ?: Uuid.generateV7(),
                    name = name,
                    calories = calories.toIntOrNull() ?: 0,
                    carbs = carbs.toIntOrNull() ?: 0,
                    fat = fat.toIntOrNull() ?: 0,
                    protein = protein.toIntOrNull() ?: 0,
                    weightInGrams = weight.toIntOrNull() ?: 0,
                    bestBeforeDate = bestBeforeDate,
                    useByDate = useByDate,
                    openedAt = openedAt,
                    createdAt = now,
                    lastModifiedAt = now
                )
                foodRepository.upsert(food)
                _isSaved.value = true
                _uiState.value = UiState.success(null)
            } catch (e: Exception) {
                _uiState.value = UiState.error(Res.string.food_detail_error_saving)
            }
        }
    }

    fun delete() {
        val id = foodId ?: return
        viewModelScope.launch {
            _uiState.value = UiState.loading()
            try {
                foodRepository.delete(id)
                _isSaved.value = true
                _uiState.value = UiState.success(null)
            } catch (e: Exception) {
                _uiState.value = UiState.error(Res.string.food_detail_error_loading)
            }
        }
    }
}
