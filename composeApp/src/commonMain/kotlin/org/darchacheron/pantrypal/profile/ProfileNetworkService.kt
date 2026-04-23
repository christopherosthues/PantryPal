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
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ProfileNetworkService(private val preferencesRepository: AuthenticationPreferencesRepository) {

    suspend fun fetchProfile(serverUrl: String): Profile? {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return null

        createHttpClient(token).use { client ->
            val response = client.get("$serverUrl/users/me")
            if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Gone) {
                throw RemoteAccountDeletedException()
            }
            return response.body()
        }
    }

    suspend fun updateProfile(profile: Profile, serverUrl: String): Profile? {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return null

        createHttpClient(token).use { client ->
            val response = client.put("$serverUrl/users/me") {
                contentType(ContentType.Application.Json)
                setBody(profile)
            }
            if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Gone) {
                throw RemoteAccountDeletedException()
            }
            return response.body()
        }
    }

    class RemoteAccountDeletedException : Exception("Remote account has been deleted.")

    suspend fun deleteProfile(serverUrl: String, remote: Boolean): Boolean {
        val auth = preferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val token = auth?.accessToken
        if (token.isNullOrBlank()) return false

        return try {
            createHttpClient(token).use { client ->
                val response = client.delete("$serverUrl/users/me") {
                    parameter("remote", remote)
                }
                if (response.status == HttpStatusCode.NotFound || response.status == HttpStatusCode.Gone) {
                    throw RemoteAccountDeletedException()
                }
                response.status == HttpStatusCode.NoContent
            }
        } catch (e: RemoteAccountDeletedException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }
}
