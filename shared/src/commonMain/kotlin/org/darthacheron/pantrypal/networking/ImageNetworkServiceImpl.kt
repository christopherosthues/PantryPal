package org.darthacheron.pantrypal.networking

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.firstOrNull
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.camera.ImageDto
import org.darthacheron.pantrypal.utils.HttpClientFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ImageNetworkServiceImpl(
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val clientFactory: HttpClientFactory
) : ImageNetworkService {

    override suspend fun uploadFoodImage(
        foodId: Uuid,
        imageData: ByteArray,
        isPrimary: Boolean,
        serverUrl: String
    ): Result<ImageDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken ?: return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                httpClient.post("$serverUrl/api/food/$foodId/images") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("isPrimary", isPrimary)
                    setBody(imageData)
                }.body()
            }
        }
    }

    override suspend fun uploadInventoryImage(
        inventoryItemId: Uuid,
        imageData: ByteArray,
        isPrimary: Boolean,
        serverUrl: String
    ): Result<ImageDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken ?: return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                httpClient.post("$serverUrl/api/inventory/$inventoryItemId/images") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("isPrimary", isPrimary)
                    setBody(imageData)
                }.body()
            }
        }
    }

    override suspend fun deleteFoodImage(foodId: Uuid, imageId: Uuid, serverUrl: String): Result<Boolean> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken ?: return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.delete("$serverUrl/api/food/$foodId/images/$imageId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                response.status == HttpStatusCode.NoContent
            }
        }
    }

    override suspend fun deleteInventoryImage(
        inventoryItemId: Uuid,
        imageId: Uuid,
        serverUrl: String
    ): Result<Boolean> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken ?: return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.delete("$serverUrl/api/inventory/$inventoryItemId/images/$imageId") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                response.status == HttpStatusCode.NoContent
            }
        }
    }
}
