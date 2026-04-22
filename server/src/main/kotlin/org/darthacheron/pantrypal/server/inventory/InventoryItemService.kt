package org.darthacheron.pantrypal.server.inventory

import org.darthacheron.pantrypal.server.camera.ImageService
import org.darthacheron.pantrypal.shared.camera.ImageDto
import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryItemService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val imageService: ImageService
) {
    fun getInventoryItemById(id: Uuid): Result<InventoryItemDto?> = runCatching {
        inventoryItemRepository.getInventoryItemById(id)
    }

    fun getAllInventoryItemsByProfileId(profileId: Uuid): Result<List<InventoryItemDto>> = runCatching {
        inventoryItemRepository.getAllInventoryItemsByProfileId(profileId)
    }

    fun createInventoryItem(inventoryItemDto: InventoryItemDto, profileId: Uuid): Result<InventoryItemDto> = runCatching {
        inventoryItemRepository.createInventoryItem(inventoryItemDto, profileId)
    }

    fun updateInventoryItem(inventoryItemDto: InventoryItemDto, profileId: Uuid): Result<InventoryItemDto> {
        val id = inventoryItemDto.serverId ?: return Result.failure(Exception("Missing inventory item server ID"))
        val existing = runCatching { inventoryItemRepository.getInventoryItemById(id) }
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("Inventory item not found"))

        if (existing.profileId != profileId) {
            return Result.failure(Exception("Forbidden"))
        }

        if (existing.deletedAt != null) {
            return Result.failure(Exception("Inventory item deleted"))
        }

        return runCatching {
            inventoryItemRepository.updateInventoryItem(inventoryItemDto, profileId)
                ?: throw Exception("Failed to update inventory item")
        }
    }

    fun deleteInventoryItem(id: Uuid, profileId: Uuid): Result<Boolean> {
        val existing = runCatching { inventoryItemRepository.getInventoryItemById(id) }
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("Inventory item not found"))

        if (existing.profileId != profileId) {
            return Result.failure(Exception("Forbidden"))
        }

        if (existing.deletedAt != null) {
            return Result.failure(Exception("Inventory item already deleted"))
        }

        return runCatching {
            inventoryItemRepository.deleteInventoryItem(id, profileId)
        }
    }

    fun saveImage(inventoryItemId: Uuid, profileId: Uuid, isPrimary: Boolean, imageData: ByteArray): Result<ImageDto> {
        val item = runCatching { inventoryItemRepository.getInventoryItemById(inventoryItemId) }
            .getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("Inventory item not found"))

        if (item.deletedAt != null) {
            return Result.failure(Exception("Inventory item deleted"))
        }

        return imageService.saveImage(null, inventoryItemId, profileId, isPrimary, imageData)
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
