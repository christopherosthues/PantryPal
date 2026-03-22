package org.darchacheron.pantrypal.inventory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryRepository(
    private val inventoryItemDao: InventoryItemDao
) {
    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String
    ): Flow<List<InventoryItem>> =
        inventoryItemDao.getFilteredAndSorted(profileId, query)
            .map { entities -> entities.map { it.toInventoryItem() } }

    suspend fun getById(id: Uuid): InventoryItem? =
        inventoryItemDao.getById(id)?.toInventoryItem()

    suspend fun upsert(inventoryItem: InventoryItem) =
        inventoryItemDao.upsert(inventoryItem.toInventoryItemEntity())

    suspend fun delete(id: Uuid) =
        inventoryItemDao.delete(id)
}
