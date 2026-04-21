package org.darthacheron.pantrypal.server.inventory

import org.darthacheron.pantrypal.server.camera.ImageService
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.shared.camera.ImageDto
import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import java.io.File
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

    fun updateInventoryItem(inventoryItemDto: InventoryItemDto, profileId: Uuid): Result<InventoryItemDto?> = runCatching {
        inventoryItemRepository.updateInventoryItem(inventoryItemDto, profileId)
    }

    fun deleteInventoryItem(id: Uuid, profileId: Uuid): Result<Boolean> = runCatching {
        inventoryItemRepository.deleteInventoryItem(id, profileId)
    }

    fun saveImage(inventoryItemId: Uuid, profileId: Uuid, isPrimary: Boolean, imageData: ByteArray): Result<ImageDto> {
        return imageService.saveImage(null, inventoryItemId, profileId, isPrimary, imageData)
    }

    fun getImage(imageId: Uuid, profileId: Uuid): Result<ByteArray?> {
        return imageService.getImageBytes(imageId, profileId)
    }

    fun getImageMetadata(imageId: Uuid, profileId: Uuid): Result<ImageDto?> {
        return imageService.getImage(imageId, profileId)
    }

    fun deleteImage(imageId: Uuid, profileId: Uuid): Result<Boolean> {
        return imageService.deleteImage(imageId, profileId)
    }
}
