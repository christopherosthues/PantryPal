package org.darchacheron.pantrypal.inventory

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryNetworkService(private val preferencesRepository: AuthenticationPreferencesRepository) {

    suspend fun pushInventoryItems(items: List<InventoryItem>, serverUrl: String): List<InventoryItem> {
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

    suspend fun fetchChanges(lastSync: Instant, serverUrl: String): List<InventoryItem> {
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

    private fun createHttpClient(accessToken: String): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        expectSuccess = true
        headersOf(HttpHeaders.Authorization, "Bearer $accessToken")
    }
}
