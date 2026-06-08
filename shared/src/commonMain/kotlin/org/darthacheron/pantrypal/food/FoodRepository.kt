package org.darthacheron.pantrypal.food

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface FoodRepository {
    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        filter: FoodFilter = FoodFilter.All,
        sort: FoodSortOrder = FoodSortOrder.Name,
        direction: FoodSortDirection = FoodSortDirection.Ascending
    ): Flow<List<Food>>

    suspend fun getById(id: Uuid): Food?
    suspend fun upsert(food: Food)
    suspend fun delete(id: Uuid)
    suspend fun syncWithServer()
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
