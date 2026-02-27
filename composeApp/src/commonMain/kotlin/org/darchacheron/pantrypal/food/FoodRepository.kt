package org.darchacheron.pantrypal.food

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodRepository(
    private val foodDao: FoodDao
) {
    fun getFilteredAndSorted(
        query: String,
        filter: FoodFilter = FoodFilter.All,
        sort: FoodSortOrder = FoodSortOrder.Name,
        direction: FoodSortDirection = FoodSortDirection.Ascending
    ): Flow<List<Food>> {
        val currentDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return foodDao.getFilteredAndSorted(
            query = query,
            filter = filter.name.uppercase(),
            sort = sort.name.uppercase(),
            direction = direction.name.uppercase(),
            currentDate = currentDate
        ).map { entities -> entities.map { it.toFood() } }
    }

    suspend fun getById(id: Uuid): Food? =
        foodDao.getById(id)?.toFood()

    suspend fun upsert(food: Food) =
        foodDao.upsert(food.toFoodEntity())

    suspend fun delete(id: Uuid) =
        foodDao.delete(id)
}

enum class FoodSortOrder {
    Name,
    Date
}

enum class FoodSortDirection {
    Ascending,
    Descending
}

enum class FoodFilter {
    All,
    Opened,
    Unopened,
    Overdue
}
