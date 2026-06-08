package org.darthacheron.pantrypal.inventory

import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface InventoryRepository {
    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        sort: InventorySortOrder = InventorySortOrder.Name,
        direction: InventorySortDirection = InventorySortDirection.Ascending,
    ): Flow<List<InventoryItem>>

    suspend fun getById(id: Uuid): InventoryItem?

    suspend fun upsert(inventoryItem: InventoryItem)

    suspend fun delete(id: Uuid)

    suspend fun syncWithServer()
}

