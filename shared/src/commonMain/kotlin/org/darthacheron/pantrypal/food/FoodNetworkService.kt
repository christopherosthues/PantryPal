package org.darthacheron.pantrypal.food

import org.darthacheron.pantrypal.core.food.FoodDto
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface FoodNetworkService {
    suspend fun pushFoods(foods: List<FoodDto>, serverUrl: String): List<FoodDto>

    suspend fun fetchChanges(lastSync: Instant, serverUrl: String): List<FoodDto>

    suspend fun deleteFood(serverId: Uuid, serverUrl: String)
}

