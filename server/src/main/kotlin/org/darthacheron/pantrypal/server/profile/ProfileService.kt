package org.darthacheron.pantrypal.server.profile

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.server.networking.createHttpClient
import org.darthacheron.pantrypal.shared.auth.TokenResponse
import org.darthacheron.pantrypal.shared.auth.UpdateUserDto
import org.darthacheron.pantrypal.shared.profile.ProfileDto
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val configurationService: ConfigurationService
) {
    private val httpClient = createHttpClient()

    fun getProfileByUsernameOrEmail(username: String): Result<ProfileDto?> = runCatching {
        profileRepository.getProfileByUsernameOrEmail(username)
    }

    fun getProfileById(id: Uuid): Result<ProfileDto?> = runCatching {
        profileRepository.getProfile(id)
    }

    fun createProfile(profileDto: ProfileDto): Result<ProfileDto> = runCatching {
        profileRepository.createProfile(profileDto)
    }

    suspend fun updateProfile(userId: Uuid, updateDto: UpdateUserDto): Result<ProfileDto> {
        val existingProfile = runCatching { profileRepository.getProfile(userId) }
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("Profile not found"))

        if (existingProfile.deletedAt != null) {
            return Result.failure(Exception("Profile is deleted"))
        }

        // 1. Get Admin Token
        val adminTokenResponse = runCatching {
            httpClient.submitForm(
                url = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "password")
                    append("client_id", "admin-cli")
                    append("username", configurationService.keycloakAdminUser)
                    append("password", configurationService.keycloakAdminPassword)
                }
            )
        }.getOrElse { return Result.failure(it) }

        if (adminTokenResponse.status != HttpStatusCode.OK) {
            return Result.failure(Exception("Failed to get admin token"))
        }
        val adminToken = runCatching { adminTokenResponse.body<TokenResponse>().accessToken }
            .getOrElse { return Result.failure(it) }

        // 2. Update Keycloak
        val updateKeycloakResponse = runCatching {
            httpClient.put("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users/$userId") {
                header(HttpHeaders.Authorization, "Bearer $adminToken")
                contentType(ContentType.Application.Json)
                val body = mutableMapOf<String, Any>()
                updateDto.username?.let { body["username"] = it }
                updateDto.email?.let { body["email"] = it }
                updateDto.password?.let {
                    body["credentials"] = listOf(
                        mapOf(
                            "type" to "password",
                            "value" to it,
                            "temporary" to false
                        )
                    )
                }
                setBody(body)
            }
        }.getOrElse { return Result.failure(it) }

        if (updateKeycloakResponse.status != HttpStatusCode.NoContent && updateKeycloakResponse.status != HttpStatusCode.OK) {
            return Result.failure(Exception("Failed to update Keycloak: ${updateKeycloakResponse.status}"))
        }

        // 3. Update Database
        val updatedProfile = existingProfile.copy(
            username = updateDto.username ?: existingProfile.username,
            email = updateDto.email ?: existingProfile.email,
            lastModifiedAt = Clock.System.now(),
            lastSyncedAt = Clock.System.now()
        )
        return runCatching { profileRepository.updateProfile(updatedProfile) }
    }

    suspend fun deleteProfile(userId: Uuid, deleteRemote: Boolean): Result<Boolean> {
        val profile = runCatching { profileRepository.getProfile(userId) }
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("Profile not found"))

        if (profile.deletedAt != null) {
            return Result.failure(Exception("Profile already deleted"))
        }

        if (deleteRemote) {
            // 1. Get Admin Token
            val adminTokenResponse = runCatching {
                httpClient.submitForm(
                    url = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/token",
                    formParameters = parameters {
                        append("grant_type", "password")
                        append("client_id", "admin-cli")
                        append("username", configurationService.keycloakAdminUser)
                        append("password", configurationService.keycloakAdminPassword)
                    }
                )
            }.getOrElse { return Result.failure(it) }

            if (adminTokenResponse.status != HttpStatusCode.OK) {
                return Result.failure(Exception("Failed to get admin token"))
            }
            val adminToken = runCatching { adminTokenResponse.body<TokenResponse>().accessToken }
                .getOrElse { return Result.failure(it) }

            // 2. Delete from Keycloak
            val deleteKeycloakResponse = runCatching {
                httpClient.delete("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users/$userId") {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                }
            }.getOrElse { return Result.failure(it) }

            if (deleteKeycloakResponse.status != HttpStatusCode.NoContent && deleteKeycloakResponse.status != HttpStatusCode.OK) {
                return Result.failure(Exception("Failed to delete from Keycloak: ${deleteKeycloakResponse.status}"))
            }
        }

        // 3. Soft Delete from Database
        return runCatching {
            profileRepository.deleteProfile(userId)
            true
        }
    }
}
