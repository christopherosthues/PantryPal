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

    suspend fun getById(id: Uuid): Result<InventoryItem?>

    suspend fun upsert(inventoryItem: InventoryItem): Result<Unit>

    suspend fun delete(id: Uuid): Result<Unit>

    suspend fun syncWithServer(): Result<Unit>
}

