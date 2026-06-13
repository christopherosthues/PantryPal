package org.darthacheron.pantrypal.profile

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.firstOrNull
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.profile.ProfileDto
import org.darthacheron.pantrypal.utils.HttpClientFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileNetworkServiceImpl(
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val clientFactory: HttpClientFactory
) : ProfileNetworkService {

    override suspend fun fetchProfile(serverUrl: String): Result<ProfileDto?> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.get("$serverUrl/api/v1/profile") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Gone) {
                    throw RemoteAccountDeletedException()
                }
                response.body<ProfileDto>()
            }
        }
    }

    override suspend fun updateProfile(
        profile: Profile,
        serverUrl: String,
        serverId: Uuid?,
        usernameOverride: String?,
        emailOverride: String?
    ): Result<ProfileDto?> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.put("$serverUrl/api/v1/profile") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody(profile.toDto(serverId, usernameOverride, emailOverride))
                }
                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Gone) {
                    throw RemoteAccountDeletedException()
                }
                response.body<ProfileDto>()
            }
        }
    }

    override suspend fun deleteProfile(serverUrl: String, remote: Boolean): Result<Boolean> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            clientFactory.create().use { httpClient ->
                val response = httpClient.delete("$serverUrl/api/v1/profile") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    parameter("remote", remote)
                }
                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Gone) {
                    throw RemoteAccountDeletedException()
                }
                response.status == HttpStatusCode.NoContent
            }
        }
    }
}
