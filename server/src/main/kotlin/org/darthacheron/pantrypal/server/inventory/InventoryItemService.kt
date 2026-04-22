package org.darthacheron.pantrypal.server.inventory

import org.darthacheron.pantrypal.server.camera.ImageService
import org.darthacheron.pantrypal.shared.camera.ImageDto
import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryItemService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val imageService: ImageService
) {
    private val logger = LoggerFactory.getLogger(InventoryItemService::class.java)

    fun getInventoryItemById(id: Uuid): Result<InventoryItemDto?> = runCatching {
        logger.debug("Getting inventory item by ID: {}", id)
        inventoryItemRepository.getInventoryItemById(id)
    }

    fun getAllInventoryItemsByProfileId(profileId: Uuid): Result<List<InventoryItemDto>> = runCatching {
        logger.debug("Getting all inventory items for profile ID: {}", profileId)
        inventoryItemRepository.getAllInventoryItemsByProfileId(profileId)
    }

    fun createInventoryItem(inventoryItemDto: InventoryItemDto, profileId: Uuid): Result<InventoryItemDto> = runCatching {
        logger.info("Creating inventory item: {} for profile ID: {}", inventoryItemDto.name, profileId)
        inventoryItemRepository.createInventoryItem(inventoryItemDto, profileId)
    }

    fun updateInventoryItem(inventoryItemDto: InventoryItemDto, profileId: Uuid): Result<InventoryItemDto> {
        val id = inventoryItemDto.serverId ?: run {
            logger.warn("Attempted to update inventory item with missing server ID")
            return Result.failure(Exception("Missing inventory item server ID"))
        }
        logger.info("Updating inventory item ID: {} for profile ID: {}", id, profileId)
        val existing = runCatching { inventoryItemRepository.getInventoryItemById(id) }
            .onFailure { logger.error("Failed to retrieve inventory item ID: {}", id, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Inventory item not found for ID: {}", id)
                return Result.failure(Exception("Inventory item not found"))
            }

        if (existing.profileId != profileId) {
            logger.warn("Forbidden update attempt on inventory item ID: {} by profile ID: {}", id, profileId)
            return Result.failure(Exception("Forbidden"))
        }

        if (existing.deletedAt != null) {
            logger.warn("Attempted to update a deleted inventory item ID: {}", id)
            return Result.failure(Exception("Inventory item deleted"))
        }

        return runCatching {
            inventoryItemRepository.updateInventoryItem(inventoryItemDto, profileId)
                ?: throw Exception("Failed to update inventory item")
        }.onSuccess { logger.info("Successfully updated inventory item ID: {}", id) }
            .onFailure { logger.error("Failed to update database for inventory item ID: {}", id, it) }
    }

    fun deleteInventoryItem(id: Uuid, profileId: Uuid): Result<Boolean> {
        logger.info("Deleting inventory item ID: {} for profile ID: {}", id, profileId)
        val existing = runCatching { inventoryItemRepository.getInventoryItemById(id) }
            .onFailure { logger.error("Failed to retrieve inventory item ID: {}", id, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Inventory item not found for deletion, ID: {}", id)
                return Result.failure(Exception("Inventory item not found"))
            }

        if (existing.profileId != profileId) {
            logger.warn("Forbidden deletion attempt on inventory item ID: {} by profile ID: {}", id, profileId)
            return Result.failure(Exception("Forbidden"))
        }

        if (existing.deletedAt != null) {
            logger.warn("Attempted to delete an already deleted inventory item ID: {}", id)
            return Result.failure(Exception("Inventory item already deleted"))
        }

        return runCatching {
            inventoryItemRepository.deleteInventoryItem(id, profileId)
        }.onSuccess { logger.info("Successfully soft deleted inventory item ID: {}", id) }
            .onFailure { logger.error("Failed to soft delete inventory item ID: {}", id, it) }
    }

    fun saveImage(inventoryItemId: Uuid, profileId: Uuid, isPrimary: Boolean, imageData: ByteArray): Result<ImageDto> {
        logger.info("Saving image for inventory item ID: {}, profile ID: {}, isPrimary: {}", inventoryItemId, profileId, isPrimary)
        val item = runCatching { inventoryItemRepository.getInventoryItemById(inventoryItemId) }
            .onFailure { logger.error("Failed to retrieve inventory item ID: {}", inventoryItemId, it) }
            .getOrElse { return Result.failure(it) }
            ?: run {
                logger.warn("Inventory item not found for image attachment, ID: {}", inventoryItemId)
                return Result.failure(Exception("Inventory item not found"))
            }

        if (item.deletedAt != null) {
            logger.warn("Attempted to add image to a deleted inventory item ID: {}", inventoryItemId)
            return Result.failure(Exception("Inventory item deleted"))
        }

        return imageService.saveImage(null, inventoryItemId, profileId, isPrimary, imageData)
            .onSuccess { logger.info("Successfully saved image for inventory item ID: {}", inventoryItemId) }
            .onFailure { logger.error("Failed to save image for inventory item ID: {}", inventoryItemId, it) }
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
