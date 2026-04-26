package org.darthacheron.pantrypal.server.profile

import org.darthacheron.pantrypal.server.keycloak.KeycloakService
import org.darthacheron.pantrypal.shared.auth.UpdateUserDto
import org.darthacheron.pantrypal.shared.profile.ProfileDto
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val keycloakService: KeycloakService
) {
    private val logger = LoggerFactory.getLogger(ProfileService::class.java)

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
                return Result.failure(ProfileNotFoundException("Profile not found"))
            }

        if (existingProfile.deletedAt != null) {
            logger.warn("Attempted to update a deleted profile for user ID: {}", userId)
            return Result.failure(ProfileDeletedException("Profile is deleted"))
        }

        // 1. Update Keycloak if credentials changed
        if (updateDto.username != null || updateDto.email != null || updateDto.password != null) {
            logger.debug("Updating Keycloak user for user ID: {}", userId)
            keycloakService.updateUser(
                userId = userId,
                username = updateDto.username,
                email = updateDto.email,
                password = updateDto.password
            ).onFailure { return Result.failure(it) }
        }

        // 2. Update Database
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

    suspend fun syncProfile(userId: Uuid, profileDto: ProfileDto): Result<ProfileDto> {
        logger.info("Syncing profile for user ID: {}", userId)
        val existingProfile = runCatching { profileRepository.getProfile(userId) }
            .onFailure { logger.error("Failed to retrieve profile for user ID: {}", userId, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Profile not found for user ID: {}", userId)
                return Result.failure(ProfileNotFoundException("Profile not found"))
            }

        if (existingProfile.deletedAt != null) {
            logger.warn("Attempted to sync a deleted profile for user ID: {}", userId)
            return Result.failure(ProfileDeletedException("Profile is deleted"))
        }

        // 1. Update Keycloak if username or email changed
        if (existingProfile.username != profileDto.username || existingProfile.email != profileDto.email) {
            logger.debug("Updating Keycloak user during sync for user ID: {}", userId)
            keycloakService.updateUser(
                userId = userId,
                username = profileDto.username,
                email = profileDto.email
            ).onFailure { return Result.failure(it) }
        }

        // 2. Update Database
        logger.debug("Updating database profile during sync for user ID: {}", userId)
        val profileToUpdate = profileDto.copy(
            serverId = userId,
            lastSyncedAt = Clock.System.now()
        )
        return runCatching { profileRepository.updateProfile(profileToUpdate) }
            .onSuccess { logger.info("Successfully synced profile for user ID: {}", userId) }
            .onFailure { logger.error("Failed to update database profile during sync for user ID: {}", userId, it) }
    }

    suspend fun deleteProfile(userId: Uuid, deleteRemote: Boolean): Result<Boolean> {
        logger.info("Deleting profile for user ID: {}, deleteRemote: {}", userId, deleteRemote)
        val profile = runCatching { profileRepository.getProfile(userId) }
            .onFailure { logger.error("Failed to retrieve profile for user ID: {}", userId, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Profile not found for user ID: {}", userId)
                return Result.failure(ProfileNotFoundException("Profile not found"))
            }

        if (profile.deletedAt != null) {
            logger.warn("Attempted to delete an already deleted profile for user ID: {}", userId)
            return Result.failure(ProfileDeletedException("Profile already deleted"))
        }

        if (deleteRemote) {
            logger.debug("Deleting user from Keycloak for user ID: {}", userId)
            keycloakService.deleteUser(userId).onFailure { return Result.failure(it) }
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
