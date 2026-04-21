package org.darthacheron.pantrypal.server.camera

import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.shared.camera.ImageDto
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ImageService(
    private val imageRepository: ImageRepository,
    private val configurationService: ConfigurationService
) {
    fun saveImage(
        foodId: Uuid?,
        inventoryItemId: Uuid?,
        profileId: Uuid,
        isPrimary: Boolean,
        imageData: ByteArray
    ): Result<ImageDto> = runCatching {
        val imageDto = imageRepository.createImage(profileId, foodId, inventoryItemId, isPrimary)
        val basePath = if (foodId != null) configurationService.foodImagesPath else configurationService.inventoryImagesPath
        val userDir = File(basePath, profileId.toString())
        if (!userDir.exists()) userDir.mkdirs()
        
        val imageFile = File(userDir, "${imageDto.serverId}.jpg")
        imageFile.writeBytes(imageData)
        imageDto
    }

    fun getImageBytes(imageId: Uuid, profileId: Uuid): Result<ByteArray?> = runCatching {
        val imageDto = imageRepository.getImageById(imageId) ?: return@runCatching null
        if (imageDto.profileId != profileId) return@runCatching null

        val basePath = if (imageDto.foodId != null) configurationService.foodImagesPath else configurationService.inventoryImagesPath
        val imageFile = File(File(basePath, profileId.toString()), "${imageId}.jpg")
        
        if (imageFile.exists()) imageFile.readBytes() else null
    }

    fun getImage(imageId: Uuid, profileId: Uuid): Result<ImageDto?> = runCatching {
        val imageDto = imageRepository.getImageById(imageId)
        if (imageDto?.profileId != profileId) null else imageDto
    }

    fun deleteImage(imageId: Uuid, profileId: Uuid): Result<Boolean> = runCatching {
        val imageDto = imageRepository.getImageById(imageId) ?: return@runCatching false
        if (imageDto.profileId != profileId) return@runCatching false

        val deleted = imageRepository.deleteImage(imageId, profileId)
        if (deleted) {
            val basePath = if (imageDto.foodId != null) configurationService.foodImagesPath else configurationService.inventoryImagesPath
            val imageFile = File(File(basePath, profileId.toString()), "${imageId}.jpg")
            if (imageFile.exists()) imageFile.delete()
        }
        deleted
    }
}
