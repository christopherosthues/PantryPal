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
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(ProfileService::class.java)
    private val httpClient = createHttpClient()

    fun getProfileByUsernameOrEmail(username: String): Result<ProfileDto?> = runCatching {
        logger.debug("Getting profile by username or email: {}", username)
        profileRepository.getProfileByUsernameOrEmail(username)
    }

    fun getProfileById(id: Uuid): Result<ProfileDto?> = runCatching {
        logger.debug("Getting profile by ID: {}", id)
        profileRepository.getProfile(id)
    }

    fun createProfile(profileDto: ProfileDto): Result<ProfileDto> = runCatching {
        logger.info("Creating profile for user: {}", profileDto.username)
        profileRepository.createProfile(profileDto)
    }

    suspend fun updateProfile(userId: Uuid, updateDto: UpdateUserDto): Result<ProfileDto> {
        logger.info("Updating profile for user ID: {}", userId)
        val existingProfile = runCatching { profileRepository.getProfile(userId) }
            .onFailure { logger.error("Failed to retrieve profile for user ID: {}", userId, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Profile not found for user ID: {}", userId)
                return Result.failure(Exception("Profile not found"))
            }

        if (existingProfile.deletedAt != null) {
            logger.warn("Attempted to update a deleted profile for user ID: {}", userId)
            return Result.failure(Exception("Profile is deleted"))
        }

        // 1. Get Admin Token
        logger.debug("Getting Keycloak admin token")
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
        }.onFailure { logger.error("Failed to get Keycloak admin token", it) }
            .getOrElse { return Result.failure(it) }

        if (adminTokenResponse.status != HttpStatusCode.OK) {
            logger.error("Failed to get Keycloak admin token, status: {}", adminTokenResponse.status)
            return Result.failure(Exception("Failed to get admin token"))
        }
        val adminToken = runCatching { adminTokenResponse.body<TokenResponse>().accessToken }
            .onFailure { logger.error("Failed to parse admin token response", it) }
            .getOrElse { return Result.failure(it) }

        // 2. Update Keycloak
        logger.debug("Updating Keycloak user for user ID: {}", userId)
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
        }.onFailure { logger.error("Failed to update Keycloak for user ID: {}", userId, it) }
            .getOrElse { return Result.failure(it) }

        if (updateKeycloakResponse.status != HttpStatusCode.NoContent && updateKeycloakResponse.status != HttpStatusCode.OK) {
            logger.error("Failed to update Keycloak for user ID: {}, status: {}", userId, updateKeycloakResponse.status)
            return Result.failure(Exception("Failed to update Keycloak: ${updateKeycloakResponse.status}"))
        }

        // 3. Update Database
        logger.debug("Updating database profile for user ID: {}", userId)
        val updatedProfile = existingProfile.copy(
            username = updateDto.username ?: existingProfile.username,
            email = updateDto.email ?: existingProfile.email,
            lastModifiedAt = Clock.System.now(),
            lastSyncedAt = Clock.System.now()
        )
        return runCatching { profileRepository.updateProfile(updatedProfile) }
            .onSuccess { logger.info("Successfully updated profile for user ID: {}", userId) }
            .onFailure { logger.error("Failed to update database profile for user ID: {}", userId, it) }
    }

    suspend fun deleteProfile(userId: Uuid, deleteRemote: Boolean): Result<Boolean> {
        logger.info("Deleting profile for user ID: {}, deleteRemote: {}", userId, deleteRemote)
        val profile = runCatching { profileRepository.getProfile(userId) }
            .onFailure { logger.error("Failed to retrieve profile for user ID: {}", userId, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Profile not found for user ID: {}", userId)
                return Result.failure(Exception("Profile not found"))
            }

        if (profile.deletedAt != null) {
            logger.warn("Attempted to delete an already deleted profile for user ID: {}", userId)
            return Result.failure(Exception("Profile already deleted"))
        }

        if (deleteRemote) {
            // 1. Get Admin Token
            logger.debug("Getting Keycloak admin token for deletion")
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
            }.onFailure { logger.error("Failed to get Keycloak admin token for deletion", it) }
                .getOrElse { return Result.failure(it) }

            if (adminTokenResponse.status != HttpStatusCode.OK) {
                logger.error("Failed to get Keycloak admin token for deletion, status: {}", adminTokenResponse.status)
                return Result.failure(Exception("Failed to get admin token"))
            }
            val adminToken = runCatching { adminTokenResponse.body<TokenResponse>().accessToken }
                .onFailure { logger.error("Failed to parse admin token response for deletion", it) }
                .getOrElse { return Result.failure(it) }

            // 2. Delete from Keycloak
            logger.debug("Deleting user from Keycloak for user ID: {}", userId)
            val deleteKeycloakResponse = runCatching {
                httpClient.delete("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users/$userId") {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                }
            }.onFailure { logger.error("Failed to delete user from Keycloak for user ID: {}", userId, it) }
                .getOrElse { return Result.failure(it) }

            if (deleteKeycloakResponse.status != HttpStatusCode.NoContent && deleteKeycloakResponse.status != HttpStatusCode.OK) {
                logger.error("Failed to delete from Keycloak for user ID: {}, status: {}", userId, deleteKeycloakResponse.status)
                return Result.failure(Exception("Failed to delete from Keycloak: ${deleteKeycloakResponse.status}"))
            }
        }

        // 3. Soft Delete from Database
        logger.debug("Soft deleting database profile for user ID: {}", userId)
        return runCatching {
            profileRepository.deleteProfile(userId)
            true
        }.onSuccess { logger.info("Successfully soft deleted profile for user ID: {}", userId) }
            .onFailure { logger.error("Failed to soft delete database profile for user ID: {}", userId, it) }
    }
}
