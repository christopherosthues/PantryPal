package org.darthacheron.pantrypal.server.camera

import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.shared.camera.ImageDto
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ImageService(
    private val imageRepository: ImageRepository,
    private val configurationService: ConfigurationService
) {
    private val logger = LoggerFactory.getLogger(ImageService::class.java)

    fun saveImage(
        foodId: Uuid?,
        inventoryItemId: Uuid?,
        profileId: Uuid,
        isPrimary: Boolean,
        imageData: ByteArray
    ): Result<ImageDto> = runCatching {
        logger.info("Saving image for profile: {}, foodId: {}, inventoryItemId: {}, isPrimary: {}", profileId, foodId, inventoryItemId, isPrimary)
        val imageDto = imageRepository.createImage(profileId, foodId, inventoryItemId, isPrimary)
        val basePath = if (foodId != null) configurationService.foodImagesPath else configurationService.inventoryImagesPath
        val userDir = File(basePath, profileId.toString())
        if (!userDir.exists()) {
            logger.debug("Creating user directory: {}", userDir.absolutePath)
            userDir.mkdirs()
        }
        
        val imageFile = File(userDir, "${imageDto.serverId}.jpg")
        logger.debug("Writing image data to file: {}", imageFile.absolutePath)
        imageFile.writeBytes(imageData)
        imageDto
    }.onFailure { logger.error("Failed to save image for profile: {}", profileId, it) }

    fun getImageBytes(imageId: Uuid, profileId: Uuid): Result<ByteArray?> = runCatching {
        logger.debug("Getting image bytes for image ID: {}, profile ID: {}", imageId, profileId)
        val imageDto = imageRepository.getImageById(imageId) ?: run {
            logger.warn("Image metadata not found for ID: {}", imageId)
            return@runCatching null
        }
        if (imageDto.profileId != profileId) {
            logger.warn("Forbidden access attempt for image ID: {} by profile ID: {}", imageId, profileId)
            return@runCatching null
        }

        val basePath = if (imageDto.foodId != null) configurationService.foodImagesPath else configurationService.inventoryImagesPath
        val imageFile = File(File(basePath, profileId.toString()), "${imageId}.jpg")
        
        if (imageFile.exists()) {
            imageFile.readBytes()
        } else {
            logger.warn("Image file not found on disk: {}", imageFile.absolutePath)
            null
        }
    }.onFailure { logger.error("Failed to get image bytes for image ID: {}", imageId, it) }

    fun getImage(imageId: Uuid, profileId: Uuid): Result<ImageDto?> = runCatching {
        logger.debug("Getting image metadata for image ID: {}, profile ID: {}", imageId, profileId)
        val imageDto = imageRepository.getImageById(imageId)
        if (imageDto?.profileId != profileId) {
            if (imageDto != null) logger.warn("Forbidden metadata access attempt for image ID: {} by profile ID: {}", imageId, profileId)
            null 
        } else imageDto
    }.onFailure { logger.error("Failed to get image metadata for image ID: {}", imageId, it) }

    fun deleteImage(imageId: Uuid, profileId: Uuid): Result<Boolean> = runCatching {
        logger.info("Deleting image ID: {} for profile ID: {}", imageId, profileId)
        val imageDto = imageRepository.getImageById(imageId) ?: run {
            logger.warn("Image metadata not found for deletion, ID: {}", imageId)
            return@runCatching false
        }
        if (imageDto.profileId != profileId) {
            logger.warn("Forbidden deletion attempt for image ID: {} by profile ID: {}", imageId, profileId)
            return@runCatching false
        }

        val deleted = imageRepository.deleteImage(imageId, profileId)
        if (deleted) {
            val basePath = if (imageDto.foodId != null) configurationService.foodImagesPath else configurationService.inventoryImagesPath
            val imageFile = File(File(basePath, profileId.toString()), "${imageId}.jpg")
            if (imageFile.exists()) {
                logger.debug("Deleting image file from disk: {}", imageFile.absolutePath)
                imageFile.delete()
            } else {
                logger.warn("Image file to delete not found on disk: {}", imageFile.absolutePath)
            }
        }
        deleted
    }.onFailure { logger.error("Failed to delete image ID: {}", imageId, it) }
}
