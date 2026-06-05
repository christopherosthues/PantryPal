package org.darthacheron.pantrypal.server.profile

import org.darthacheron.pantrypal.server.camera.ImageService
import org.darthacheron.pantrypal.server.configuration.DynamicConfigurationService
import org.darthacheron.pantrypal.server.food.FoodService
import org.darthacheron.pantrypal.server.inventory.InventoryItemService
import org.darthacheron.pantrypal.server.keycloak.InvalidCredentialsException
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
    private val foodService: FoodService,
    private val inventoryItemService: InventoryItemService,
    private val imageService: ImageService,
    private val keycloakService: KeycloakService,
    private val dynamicConfigurationService: DynamicConfigurationService
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

        // TODO: update still not working. Current password is a requirement for username and email which is not correct

        // 0. Verify current password if password is being changed
        if (updateDto.password != null) {
            if (updateDto.currentPassword == null) {
                return Result.failure(InvalidCredentialsException("Current password is required to change password"))
            }
            keycloakService.getAccessToken(existingProfile.username, updateDto.currentPassword!!)
                .onFailure { return Result.failure(InvalidCredentialsException("Invalid current password")) }
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
        if (!dynamicConfigurationService.config.features.remoteSyncEnabled) {
            return Result.failure(Exception("Remote synchronization is currently disabled"))
        }
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

        // Delete all associated data
        logger.debug("Soft deleting all associated data for user ID: {}", userId)
        foodService.deleteAllFoodForProfile(userId)
        inventoryItemService.deleteAllInventoryItemsForProfile(userId)
        imageService.deleteAllImagesForProfile(userId)

        // 3. Soft Delete from Database
        logger.debug("Soft deleting database profile for user ID: {}", userId)
        return runCatching {
            profileRepository.deleteProfile(userId)
            true
        }.onSuccess { logger.info("Successfully soft deleted profile for user ID: {}", userId) }
            .onFailure { logger.error("Failed to soft delete database profile for user ID: {}", userId, it) }
    }
}
