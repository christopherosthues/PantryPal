package org.darchacheron.pantrypal.profile

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.firstOrNull
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.utils.createHttpClient
import org.darthacheron.pantrypal.core.profile.ProfileDto
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ProfileNetworkService(private val preferencesRepository: AuthenticationPreferencesRepository) {

    suspend fun fetchProfile(serverUrl: String): Result<ProfileDto?> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                val response = client.get("$serverUrl/profile")
                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Gone) {
                    throw RemoteAccountDeletedException()
                }
                response.body<ProfileDto>()
            }
        }
    }

    suspend fun updateProfile(
        profile: Profile,
        serverUrl: String,
        usernameOverride: String? = null,
        emailOverride: String? = null
    ): Result<ProfileDto?> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                val response = client.put("$serverUrl/profile") {
                    contentType(ContentType.Application.Json)
                    setBody(profile.toDto(usernameOverride, emailOverride))
                }
                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Gone) {
                    throw RemoteAccountDeletedException()
                }
                response.body<ProfileDto>()
            }
        }
    }

    class RemoteAccountDeletedException : Exception("Remote account has been deleted.")

    suspend fun deleteProfile(serverUrl: String, remote: Boolean): Result<Boolean> {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return Result.failure(Exception("Not authenticated"))

        return runCatching {
            createHttpClient(token).use { client ->
                val response = client.delete("$serverUrl/profile") {
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
