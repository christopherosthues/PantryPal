package org.darthacheron.pantrypal.server.inventory

import org.darthacheron.pantrypal.shared.inventory.InventoryItemDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryItemService(
    private val inventoryItemRepository: InventoryItemRepository
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
}
