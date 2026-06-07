package org.darthacheron.pantrypal.inventory

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.firstOrNull
import org.darthacheron.pantrypal.core.inventory.InventoryItemDto
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.utils.createHttpClient
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryNetworkService(private val preferencesRepository: AuthenticationPreferencesRepository) {

    suspend fun pushInventoryItems(items: List<InventoryItemDto>, serverUrl: String): List<InventoryItemDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return emptyList()

        return createHttpClient(token).use { client ->
            client.post("$serverUrl/api/inventory/batch") {
                contentType(ContentType.Application.Json)
                setBody(items)
            }.body()
        }
    }

    suspend fun fetchChanges(lastSync: Instant, serverUrl: String): List<InventoryItemDto> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return emptyList()

        return createHttpClient(token).use { client ->
            client.get("$serverUrl/api/inventory/sync") {
                parameter("since", lastSync.toString())
            }.body()
        }
    }

    suspend fun deleteInventoryItem(serverId: Uuid, serverUrl: String) {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return

        createHttpClient(token).use { client ->
            client.delete("$serverUrl/api/inventory/$serverId")
        }
    }
}
