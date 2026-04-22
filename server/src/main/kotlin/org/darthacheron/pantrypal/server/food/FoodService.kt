package org.darthacheron.pantrypal.server.food

import org.darthacheron.pantrypal.server.camera.ImageService
import org.darthacheron.pantrypal.shared.camera.ImageDto
import org.darthacheron.pantrypal.shared.food.FoodDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodService(
    private val foodRepository: FoodRepository,
    private val imageService: ImageService
) {
    fun getFoodById(id: Uuid): Result<FoodDto?> = runCatching {
        foodRepository.getFoodById(id)
    }

    fun getAllFoodByProfileId(profileId: Uuid): Result<List<FoodDto>> = runCatching {
        foodRepository.getAllFoodByProfileId(profileId)
    }

    fun createFood(foodDto: FoodDto, profileId: Uuid): Result<FoodDto> = runCatching {
        foodRepository.createFood(foodDto, profileId)
    }

    fun updateFood(foodDto: FoodDto, profileId: Uuid): Result<FoodDto> {
        val id = foodDto.serverId ?: return Result.failure(Exception("Missing food item server ID"))
        val existing = runCatching { foodRepository.getFoodById(id) }
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("Food not found"))

        if (existing.profileId != profileId) {
            return Result.failure(Exception("Forbidden"))
        }

        if (existing.deletedAt != null) {
            return Result.failure(Exception("Food deleted"))
        }

        return runCatching {
            foodRepository.updateFood(foodDto, profileId)
                ?: throw Exception("Failed to update food item")
        }
    }

    fun deleteFood(id: Uuid, profileId: Uuid): Result<Boolean> {
        val existing = runCatching { foodRepository.getFoodById(id) }
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("Food not found"))

        if (existing.profileId != profileId) {
            return Result.failure(Exception("Forbidden"))
        }

        if (existing.deletedAt != null) {
            return Result.failure(Exception("Food already deleted"))
        }

        return runCatching {
            foodRepository.deleteFood(id, profileId)
        }
    }

    fun saveImage(foodId: Uuid, profileId: Uuid, isPrimary: Boolean, imageData: ByteArray): Result<ImageDto> {
        val food = runCatching { foodRepository.getFoodById(foodId) }
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("Food not found"))

        if (food.deletedAt != null) {
            return Result.failure(Exception("Food deleted"))
        }

        return imageService.saveImage(foodId, null, profileId, isPrimary, imageData)
    }

    fun getImage(imageId: Uuid, profileId: Uuid): Result<ByteArray?> {
        return imageService.getImageBytes(imageId, profileId)
    }

    fun getImageMetadata(imageId: Uuid, profileId: Uuid): Result<ImageDto?> {
        return imageService.getImage(imageId, profileId)
    }

    fun deleteImage(imageId: Uuid, profileId: Uuid): Result<Boolean> {
        val image = imageService.getImage(imageId, profileId).getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("Image not found"))

        if (image.deletedAt != null) {
            return Result.failure(Exception("Image already deleted"))
        }

        return imageService.deleteImage(imageId, profileId)
    }
}
