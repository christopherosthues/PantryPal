package org.darthacheron.pantrypal.profile

import org.darthacheron.pantrypal.core.configuration.ServerDynamicConfiguration

interface AdminNetworkService {
    suspend fun fetchConfig(serverUrl: String): Result<ServerDynamicConfiguration>
    suspend fun updateConfig(serverUrl: String, config: ServerDynamicConfiguration): Result<ServerDynamicConfiguration>
    suspend fun reloadConfig(serverUrl: String): Result<ServerDynamicConfiguration>
}

