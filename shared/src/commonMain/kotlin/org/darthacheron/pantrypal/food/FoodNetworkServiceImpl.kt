package org.darthacheron.pantrypal.food

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.firstOrNull
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.food.FoodDto
import org.darthacheron.pantrypal.utils.HttpClientFactory
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodNetworkServiceImpl(
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val clientFactory: HttpClientFactory
) : FoodNetworkService {

    override suspend fun pushFoods(foods: List<FoodDto>, serverUrl: String): List<FoodDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return emptyList()

        return clientFactory.create().use { httpClient ->
            httpClient.post("$serverUrl/api/v1/food/batch") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(foods)
            }.body()
        }
    }

    override suspend fun fetchChanges(lastSync: Instant, serverUrl: String): List<FoodDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return emptyList()

        return clientFactory.create().use { httpClient ->
            httpClient.get("$serverUrl/api/v1/food/sync") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("since", lastSync.toString())
            }.body()
        }
    }

    override suspend fun deleteFood(serverId: Uuid, serverUrl: String) {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return

        clientFactory.create().use { httpClient ->
            httpClient.delete("$serverUrl/api/v1/food/$serverId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}
