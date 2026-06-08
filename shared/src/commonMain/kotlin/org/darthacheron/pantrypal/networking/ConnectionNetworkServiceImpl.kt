package org.darthacheron.pantrypal.networking

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.darthacheron.pantrypal.core.NetworkingConstants

class ConnectionNetworkServiceImpl : ConnectionNetworkService {
    override suspend fun testConnection(serverUrl: String): Result<String> {
        return try {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                    })
                }
                defaultRequest {
                    header(NetworkingConstants.APP_TOKEN_HEADER, NetworkingConstants.APP_TOKEN)
                    // Required for server CSRF/CORS validation
                    header(HttpHeaders.Origin, "http://localhost:8081")
                    header("X-CSRF-Token", "PantryPal")
                    contentType(ContentType.Application.Json)
                }
                expectSuccess = true
            }.use { client ->
                val response = client.get("$serverUrl/api/connection/test")
                if (response.status == HttpStatusCode.OK) {
                    Result.success("Connection successful")
                } else {
                    Result.failure(Exception("Server returned ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}