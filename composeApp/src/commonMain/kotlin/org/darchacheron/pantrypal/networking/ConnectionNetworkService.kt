package org.darchacheron.pantrypal.networking

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.darthacheron.pantrypal.shared.NetworkingConstants

class ConnectionNetworkService {

    suspend fun testConnection(serverUrl: String): Result<String> {
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
