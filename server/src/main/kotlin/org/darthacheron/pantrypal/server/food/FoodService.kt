package org.darthacheron.pantrypal.server.food

import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.shared.food.FoodDto
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodService(
    private val foodRepository: FoodRepository,
    private val configurationService: ConfigurationService
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
        val deleted = foodRepository.deleteFood(id, profileId)
        if (deleted) {
            deleteFoodImage(id, profileId)
        }
        deleted
    }

    fun saveFoodImage(foodId: Uuid, profileId: Uuid, imageData: ByteArray): Result<Unit> = runCatching {
        val userDir = File(configurationService.foodImagesPath, profileId.toString())
        if (!userDir.exists()) {
            userDir.mkdirs()
        }
        val imageFile = File(userDir, "$foodId.jpg")
        imageFile.writeBytes(imageData)
    }

    fun getFoodImage(foodId: Uuid, profileId: Uuid): Result<ByteArray?> = runCatching {
        val imageFile = File(File(configurationService.foodImagesPath, profileId.toString()), "$foodId.jpg")
        if (imageFile.exists()) {
            imageFile.readBytes()
        } else {
            null
        }
    }

    private fun deleteFoodImage(foodId: Uuid, profileId: Uuid) {
        val imageFile = File(File(configurationService.foodImagesPath, profileId.toString()), "$foodId.jpg")
        if (imageFile.exists()) {
            imageFile.delete()
        }
    }
}
