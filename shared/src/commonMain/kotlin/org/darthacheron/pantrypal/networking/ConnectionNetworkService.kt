package org.darthacheron.pantrypal.networking

interface ConnectionNetworkService {
    suspend fun testConnection(serverUrl: String): Result<String>
}
