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

    suspend fun getById(id: Uuid): Result<Food?>
    suspend fun upsert(food: Food): Result<Unit>
    suspend fun delete(id: Uuid): Result<Unit>
    suspend fun syncWithServer(): Result<Unit>
}
