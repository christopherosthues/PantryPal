package org.darthacheron.pantrypal.server.food

import org.darthacheron.pantrypal.server.camera.ImageService
import org.darthacheron.pantrypal.shared.camera.ImageDto
import org.darthacheron.pantrypal.shared.food.FoodDto
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodService(
    private val foodRepository: FoodRepository,
    private val imageService: ImageService
) {
    private val logger = LoggerFactory.getLogger(FoodService::class.java)

    fun getFoodById(id: Uuid): Result<FoodDto?> = runCatching {
        logger.debug("Getting food by ID: {}", id)
        foodRepository.getFoodById(id)
    }

    fun getAllFoodByProfileId(profileId: Uuid): Result<List<FoodDto>> = runCatching {
        logger.debug("Getting all food for profile ID: {}", profileId)
        foodRepository.getAllFoodByProfileId(profileId)
    }

    fun createFood(foodDto: FoodDto, profileId: Uuid): Result<FoodDto> = runCatching {
        logger.info("Creating food item: {} for profile ID: {}", foodDto.name, profileId)
        foodRepository.createFood(foodDto, profileId)
    }

    fun updateFood(foodDto: FoodDto, profileId: Uuid): Result<FoodDto> {
        val id = foodDto.serverId ?: run {
            logger.warn("Attempted to update food item with missing server ID")
            return Result.failure(FoodMissingIdException("Missing food item server ID"))
        }
        logger.info("Updating food item ID: {} for profile ID: {}", id, profileId)
        val existing = runCatching { foodRepository.getFoodById(id) }
            .onFailure { logger.error("Failed to retrieve food item ID: {}", id, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Food item not found for ID: {}", id)
                return Result.failure(FoodNotFoundException("Food not found"))
            }

        if (existing.profileId != profileId) {
            logger.warn("Forbidden update attempt on food item ID: {} by profile ID: {}", id, profileId)
            return Result.failure(FoodNoAccessException("Forbidden"))
        }

        if (existing.deletedAt != null) {
            logger.warn("Attempted to update a deleted food item ID: {}", id)
            return Result.failure(FoodDeletedException("Food deleted"))
        }

        return runCatching {
            foodRepository.updateFood(foodDto, profileId)
                ?: throw Exception("Failed to update food item")
        }.onSuccess { logger.info("Successfully updated food item ID: {}", id) }
            .onFailure { logger.error("Failed to update database for food item ID: {}", id, it) }
    }

    fun deleteFood(id: Uuid, profileId: Uuid): Result<Boolean> {
        logger.info("Deleting food item ID: {} for profile ID: {}", id, profileId)
        val existing = runCatching { foodRepository.getFoodById(id) }
            .onFailure { logger.error("Failed to retrieve food item ID: {}", id, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Food item not found for deletion, ID: {}", id)
                return Result.failure(FoodNotFoundException("Food not found"))
            }

        if (existing.profileId != profileId) {
            logger.warn("Forbidden deletion attempt on food item ID: {} by profile ID: {}", id, profileId)
            return Result.failure(FoodNoAccessException("Forbidden"))
        }

        if (existing.deletedAt != null) {
            logger.warn("Attempted to delete an already deleted food item ID: {}", id)
            return Result.failure(FoodDeletedException("Food already deleted"))
        }

        return runCatching {
            foodRepository.deleteFood(id, profileId)
        }.onSuccess { logger.info("Successfully soft deleted food item ID: {}", id) }
            .onFailure { logger.error("Failed to soft delete food item ID: {}", id, it) }
    }

    fun saveImage(foodId: Uuid, profileId: Uuid, isPrimary: Boolean, imageData: ByteArray): Result<ImageDto> {
        logger.info("Saving image for food ID: {}, profile ID: {}, isPrimary: {}", foodId, profileId, isPrimary)
        val food = runCatching { foodRepository.getFoodById(foodId) }
            .onFailure { logger.error("Failed to retrieve food item ID: {}", foodId, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Food item not found for image attachment, ID: {}", foodId)
                return Result.failure(FoodNotFoundException("Food not found"))
            }

        if (food.deletedAt != null) {
            logger.warn("Attempted to add image to a deleted food item ID: {}", foodId)
            return Result.failure(FoodDeletedException("Food deleted"))
        }

        return imageService.saveImage(foodId, null, profileId, isPrimary, imageData)
            .onSuccess { logger.info("Successfully saved image for food ID: {}", foodId) }
            .onFailure { logger.error("Failed to save image for food ID: {}", foodId, it) }
    }

    fun getImage(imageId: Uuid, profileId: Uuid): Result<ByteArray?> {
        logger.debug("Getting image bytes for image ID: {}, profile ID: {}", imageId, profileId)
        return imageService.getImageBytes(imageId, profileId)
    }

    fun getImageMetadata(imageId: Uuid, profileId: Uuid): Result<ImageDto?> {
        logger.debug("Getting image metadata for image ID: {}, profile ID: {}", imageId, profileId)
        return imageService.getImage(imageId, profileId)
    }

    fun deleteImage(imageId: Uuid, profileId: Uuid): Result<Boolean> {
        logger.info("Deleting image ID: {} for profile ID: {}", imageId, profileId)
        val image = imageService.getImage(imageId, profileId)
            .onFailure { logger.error("Failed to retrieve image metadata for image ID: {}", imageId, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Image not found for deletion, ID: {}", imageId)
                return Result.failure(Exception("Image not found"))
            }

        if (image.deletedAt != null) {
            logger.warn("Attempted to delete an already deleted image ID: {}", imageId)
            return Result.failure(Exception("Image already deleted"))
        }

        return imageService.deleteImage(imageId, profileId)
            .onSuccess { logger.info("Successfully deleted image ID: {}", imageId) }
            .onFailure { logger.error("Failed to delete image ID: {}", imageId, it) }
    }
}

