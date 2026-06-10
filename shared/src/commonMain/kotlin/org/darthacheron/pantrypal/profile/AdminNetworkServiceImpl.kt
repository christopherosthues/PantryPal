package org.darthacheron.pantrypal.profile

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.firstOrNull
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.configuration.ServerDynamicConfiguration
import org.darthacheron.pantrypal.utils.HttpClientFactory
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class AdminNetworkServiceImpl(
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val clientFactory: HttpClientFactory
) : AdminNetworkService {

    override suspend fun fetchConfig(serverUrl: String): Result<ServerDynamicConfiguration> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                httpClient.get("$serverUrl/admin/config") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.body<ServerDynamicConfiguration>()
            }
        }
    }

    override suspend fun updateConfig(
        serverUrl: String,
        config: ServerDynamicConfiguration
    ): Result<ServerDynamicConfiguration> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.put("$serverUrl/admin/config") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(config)
                }
                if (response.status != HttpStatusCode.OK) {
                    throw Exception("Failed to update config: ${response.status}")
                }
                response.body<ServerDynamicConfiguration>()
            }
        }
    }

    override suspend fun reloadConfig(serverUrl: String): Result<ServerDynamicConfiguration> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                httpClient.post("$serverUrl/admin/config/reload") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }.body<ServerDynamicConfiguration>()
            }
        }
    }
}
