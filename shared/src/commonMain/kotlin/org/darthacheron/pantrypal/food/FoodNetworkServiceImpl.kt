package org.darthacheron.pantrypal.food

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.firstOrNull
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.authentication.NotAuthenticatedException
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

    override suspend fun fetchAllFoods(serverUrl: String): Result<List<FoodDto>> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(NotAuthenticatedException("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                httpClient.get("$serverUrl/api/v1/food") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.body()
            }
        }
    }

    override suspend fun fetchFoodById(serverId: Uuid, serverUrl: String): Result<FoodDto?> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(NotAuthenticatedException("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.get("$serverUrl/api/v1/food/$serverId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                if (response.status == HttpStatusCode.OK) response.body<FoodDto>() else null
            }
        }
    }

    override suspend fun createFood(food: FoodDto, serverUrl: String): Result<FoodDto?> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(NotAuthenticatedException("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.post("$serverUrl/api/v1/food") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(food)
                }
                if (response.status == HttpStatusCode.Created) response.body<FoodDto>() else null
            }
        }
    }

    override suspend fun updateFood(food: FoodDto, serverUrl: String): Result<FoodDto?> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(NotAuthenticatedException("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.put("$serverUrl/api/v1/food") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(food)
                }
                if (response.status == HttpStatusCode.OK) response.body<FoodDto>() else null
            }
        }
    }

    override suspend fun pushFoods(foods: List<FoodDto>, serverUrl: String): Result<List<FoodDto>> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(NotAuthenticatedException("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                httpClient.post("$serverUrl/api/v1/food/batch") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(foods)
                }.body()
            }
        }
    }

    override suspend fun fetchChanges(lastSync: Instant, serverUrl: String): Result<List<FoodDto>> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(NotAuthenticatedException("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                httpClient.get("$serverUrl/api/v1/food/sync") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("since", lastSync.toString())
                }.body()
            }
        }
    }

    override suspend fun deleteFood(serverId: Uuid, serverUrl: String): Result<Boolean> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(NotAuthenticatedException("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.delete("$serverUrl/api/v1/food/$serverId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                response.status == HttpStatusCode.NoContent
            }
        }
    }
}
