package org.darthacheron.pantrypal.profile

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.firstOrNull
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.utils.createHttpClient
import org.darthacheron.pantrypal.core.configuration.ServerDynamicConfiguration
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class AdminNetworkService(private val preferencesRepository: AuthenticationPreferencesRepository) {

    suspend fun fetchConfig(serverUrl: String): Result<ServerDynamicConfiguration> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                client.get("$serverUrl/admin/config").body<ServerDynamicConfiguration>()
            }
        }
    }

    suspend fun updateConfig(serverUrl: String, config: ServerDynamicConfiguration): Result<ServerDynamicConfiguration> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                val response = client.put("$serverUrl/admin/config") {
                    contentType(ContentType.Application.Json)
                    setBody(config)
                }
                if (response.status != HttpStatusCode.OK) {
                    throw Exception("Failed to update config: ${response.status}")
                }
                response.body<ServerDynamicConfiguration>()
            }
        }
    }

    suspend fun reloadConfig(serverUrl: String): Result<ServerDynamicConfiguration> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                client.post("$serverUrl/admin/config/reload").body<ServerDynamicConfiguration>()
            }
        }
    }
}
