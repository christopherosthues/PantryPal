package org.darthacheron.pantrypal.server.inventory

import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryItemService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val configurationService: ConfigurationService
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
        val deleted = inventoryItemRepository.deleteInventoryItem(id, profileId)
        if (deleted) {
            deleteInventoryImage(id, profileId)
        }
        deleted
    }

    fun saveInventoryImage(inventoryItemId: Uuid, profileId: Uuid, imageData: ByteArray): Result<Unit> = runCatching {
        val userDir = File(configurationService.inventoryImagesPath, profileId.toString())
        if (!userDir.exists()) {
            userDir.mkdirs()
        }
        val imageFile = File(userDir, "$inventoryItemId.jpg")
        imageFile.writeBytes(imageData)
    }

    fun getInventoryImage(inventoryItemId: Uuid, profileId: Uuid): Result<ByteArray?> = runCatching {
        val imageFile = File(File(configurationService.inventoryImagesPath, profileId.toString()), "$inventoryItemId.jpg")
        if (imageFile.exists()) {
            imageFile.readBytes()
        } else {
            null
        }
    }

    private fun deleteInventoryImage(inventoryItemId: Uuid, profileId: Uuid) {
        val imageFile = File(File(configurationService.inventoryImagesPath, profileId.toString()), "$inventoryItemId.jpg")
        if (imageFile.exists()) {
            imageFile.delete()
        }
    }
}
