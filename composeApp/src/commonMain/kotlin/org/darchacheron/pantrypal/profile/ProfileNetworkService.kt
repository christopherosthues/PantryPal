package org.darchacheron.pantrypal.profile

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ProfileNetworkService(private val preferencesRepository: AuthenticationPreferencesRepository) {

    suspend fun fetchProfile(serverUrl: String): Profile? {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return null

        return createHttpClient(token).use { client ->
            client.get("$serverUrl/api/profile").body()
        }
    }

    suspend fun updateProfile(profile: Profile, serverUrl: String): Profile? {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return null

        return createHttpClient(token).use { client ->
            client.put("$serverUrl/api/profile") {
                contentType(ContentType.Application.Json)
                setBody(profile)
            }.body()
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
