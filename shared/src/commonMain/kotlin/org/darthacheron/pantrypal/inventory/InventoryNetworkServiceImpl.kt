package org.darthacheron.pantrypal.inventory

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
import org.darthacheron.pantrypal.core.inventory.InventoryItemDto
import org.darthacheron.pantrypal.utils.HttpClientFactory
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryNetworkServiceImpl(
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val clientFactory: HttpClientFactory
) :
    InventoryNetworkService {

    override suspend fun fetchAllInventoryItems(serverUrl: String): List<InventoryItemDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return emptyList()

        return clientFactory.create().use { httpClient ->
            httpClient.get("$serverUrl/api/v1/inventory") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body()
        }
    }

    override suspend fun fetchInventoryItemById(serverId: Uuid, serverUrl: String): InventoryItemDto? {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return null

        return clientFactory.create().use { httpClient ->
            val response = httpClient.get("$serverUrl/api/v1/inventory/$serverId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        }
    }

    override suspend fun createInventoryItem(item: InventoryItemDto, serverUrl: String): InventoryItemDto? {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return null

        return clientFactory.create().use { httpClient ->
            val response = httpClient.post("$serverUrl/api/v1/inventory") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(item)
            }
            if (response.status == HttpStatusCode.Created) response.body() else null
        }
    }

    override suspend fun updateInventoryItem(item: InventoryItemDto, serverUrl: String): InventoryItemDto? {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return null

        return clientFactory.create().use { httpClient ->
            val response = httpClient.put("$serverUrl/api/v1/inventory") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(item)
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        }
    }

    override suspend fun pushInventoryItems(items: List<InventoryItemDto>, serverUrl: String): List<InventoryItemDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return emptyList()

        return clientFactory.create().use { httpClient ->
            httpClient.post("$serverUrl/api/v1/inventory/batch") {
                header(HttpHeaders.Authorization, "Bearer $token")
                setBody(items)
            }.body()
        }
    }

    override suspend fun fetchChanges(lastSync: Instant, serverUrl: String): List<InventoryItemDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return emptyList()

        return clientFactory.create().use { httpClient ->
            httpClient.get("$serverUrl/api/v1/inventory/sync") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("since", lastSync.toString())
            }.body()
        }
    }

    override suspend fun deleteInventoryItem(serverId: Uuid, serverUrl: String) {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return

        clientFactory.create().use { httpClient ->
            httpClient.delete("$serverUrl/api/v1/inventory/$serverId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}
