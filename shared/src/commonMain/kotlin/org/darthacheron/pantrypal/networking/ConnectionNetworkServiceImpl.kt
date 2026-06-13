package org.darthacheron.pantrypal.networking

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import org.darthacheron.pantrypal.core.NetworkingConstants
import org.darthacheron.pantrypal.utils.HttpClientFactory

class ConnectionNetworkServiceImpl(private val clientFactory: HttpClientFactory) : ConnectionNetworkService {
    override suspend fun testConnection(serverUrl: String): Result<String> {
        return try {
            clientFactory.create().use { httpClient ->
                val response = httpClient.get("$serverUrl/api/v1/connection/test") {
                    header(NetworkingConstants.APP_TOKEN_HEADER, NetworkingConstants.APP_TOKEN)
                    // Required for server CSRF/CORS validation
                    header("X-CSRF-Token", "PantryPal")
                }
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
