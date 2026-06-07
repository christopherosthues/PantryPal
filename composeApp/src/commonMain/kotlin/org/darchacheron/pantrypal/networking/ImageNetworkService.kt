package org.darchacheron.pantrypal.networking

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.firstOrNull
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.utils.createHttpClient
import org.darthacheron.pantrypal.core.camera.ImageDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ImageNetworkService(private val preferencesRepository: AuthenticationPreferencesRepository) {

    suspend fun uploadFoodImage(
        foodId: Uuid,
        imageData: ByteArray,
        isPrimary: Boolean,
        serverUrl: String
    ): Result<ImageDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken ?: return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                client.post("$serverUrl/api/food/$foodId/images") {
                    parameter("isPrimary", isPrimary)
                    setBody(imageData)
                }.body()
            }
        }
    }

    suspend fun uploadInventoryImage(
        inventoryItemId: Uuid,
        imageData: ByteArray,
        isPrimary: Boolean,
        serverUrl: String
    ): Result<ImageDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken ?: return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                client.post("$serverUrl/api/inventory/$inventoryItemId/images") {
                    parameter("isPrimary", isPrimary)
                    setBody(imageData)
                }.body()
            }
        }
    }

    suspend fun deleteFoodImage(foodId: Uuid, imageId: Uuid, serverUrl: String): Result<Boolean> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken ?: return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                val response = client.delete("$serverUrl/api/food/$foodId/images/$imageId")
                response.status == HttpStatusCode.NoContent
            }
        }
    }

    suspend fun deleteInventoryImage(inventoryItemId: Uuid, imageId: Uuid, serverUrl: String): Result<Boolean> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken ?: return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                val response = client.delete("$serverUrl/api/inventory/$inventoryItemId/images/$imageId")
                response.status == HttpStatusCode.NoContent
            }
        }
    }
}
