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

    fun updateFood(foodDto: FoodDto, profileId: Uuid): Result<FoodDto?> = runCatching {
        foodRepository.updateFood(foodDto, profileId)
    }

    fun deleteFood(id: Uuid, profileId: Uuid): Result<Boolean> = runCatching {
        foodRepository.deleteFood(id, profileId)
    }

    fun saveImage(foodId: Uuid, profileId: Uuid, isPrimary: Boolean, imageData: ByteArray): Result<ImageDto> {
        return imageService.saveImage(foodId, null, profileId, isPrimary, imageData)
    }

    fun getImage(imageId: Uuid, profileId: Uuid): Result<ByteArray?> {
        return imageService.getImageBytes(imageId, profileId)
    }

    fun deleteImage(imageId: Uuid, profileId: Uuid): Result<Boolean> {
        return imageService.deleteImage(imageId, profileId)
    }
}
