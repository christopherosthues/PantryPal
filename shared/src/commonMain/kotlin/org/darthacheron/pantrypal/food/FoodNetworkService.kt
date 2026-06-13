package org.darthacheron.pantrypal.food

import org.darthacheron.pantrypal.core.food.FoodDto
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface FoodNetworkService {
    suspend fun fetchAllFoods(serverUrl: String): Result<List<FoodDto>>

    suspend fun fetchFoodById(serverId: Uuid, serverUrl: String): Result<FoodDto?>

    suspend fun createFood(food: FoodDto, serverUrl: String): Result<FoodDto?>

    suspend fun updateFood(food: FoodDto, serverUrl: String): Result<FoodDto?>

    suspend fun pushFoods(foods: List<FoodDto>, serverUrl: String): Result<List<FoodDto>>

    suspend fun fetchChanges(lastSync: Instant, serverUrl: String): Result<List<FoodDto>>

    suspend fun deleteFood(serverId: Uuid, serverUrl: String): Result<Boolean>
}

