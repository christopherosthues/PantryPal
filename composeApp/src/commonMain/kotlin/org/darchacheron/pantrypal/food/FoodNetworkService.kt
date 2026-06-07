package org.darchacheron.pantrypal.food

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.firstOrNull
import org.darthacheron.pantrypal.core.food.FoodDto
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.utils.createHttpClient
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodNetworkService(private val preferencesRepository: AuthenticationPreferencesRepository) {

    suspend fun pushFoods(foods: List<FoodDto>, serverUrl: String): List<FoodDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return emptyList()

        return createHttpClient(token).use { client ->
            client.post("$serverUrl/api/food/batch") {
                contentType(ContentType.Application.Json)
                setBody(foods)
            }.body()
        }
    }

    suspend fun fetchChanges(lastSync: Instant, serverUrl: String): List<FoodDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return emptyList()

        return createHttpClient(token).use { client ->
            client.get("$serverUrl/api/food/sync") {
                parameter("since", lastSync.toString())
            }.body()
        }
    }

    suspend fun deleteFood(serverId: Uuid, serverUrl: String) {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return

        createHttpClient(token).use { client ->
            client.delete("$serverUrl/api/food/$serverId")
        }
    }
}
