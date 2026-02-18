package org.darchacheron.pantrypal.food

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodRepository(
    private val foodDao: FoodDao
) {
    fun getAll(): Flow<List<Food>> =
        foodDao.getAll().map { entities -> entities.map { it.toFood() } }

    suspend fun getById(id: Uuid): Food? =
        foodDao.getById(id)?.toFood()

    suspend fun upsert(food: Food) =
        foodDao.upsert(food.toFoodEntity())

    suspend fun delete(id: Uuid) =
        foodDao.delete(id)
}
