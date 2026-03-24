package org.darchacheron.pantrypal.inventory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.darchacheron.pantrypal.food.FoodSortDirection
import org.darchacheron.pantrypal.food.FoodSortOrder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryRepository(
    private val inventoryItemDao: InventoryItemDao
) {
    // TODO: Sync
    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        sort: InventorySortOrder = InventorySortOrder.Name,
        direction: InventorySortDirection = InventorySortDirection.Ascending
    ): Flow<List<InventoryItem>> =
        inventoryItemDao.getFilteredAndSorted(
            profileId = profileId,
            query = query,
            sort = sort.name.uppercase(),
            direction = direction.name.uppercase(),
        ).map { entities -> entities.map { it.toInventoryItem() } }

    suspend fun getById(id: Uuid): InventoryItem? =
        inventoryItemDao.getById(id)?.toInventoryItem()

    suspend fun upsert(inventoryItem: InventoryItem) =
        inventoryItemDao.upsert(inventoryItem.toInventoryItemEntity())

    suspend fun delete(id: Uuid) =
        inventoryItemDao.delete(id)
}

enum class InventorySortOrder {
    Name,
}

enum class InventorySortDirection {
    Ascending,
    Descending
}