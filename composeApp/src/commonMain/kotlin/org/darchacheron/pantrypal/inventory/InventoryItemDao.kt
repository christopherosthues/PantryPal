package org.darchacheron.pantrypal.inventory

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface InventoryItemDao {
    @Query("""
        SELECT * FROM inventory_item 
        WHERE profileId = :profileId
        AND (:query = '' OR name LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String
    ): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_item WHERE id = :id")
    suspend fun getById(id: Uuid): InventoryItemEntity?

    @Upsert
    suspend fun upsert(inventoryItem: InventoryItemEntity)

    @Query("DELETE FROM inventory_item WHERE id = :id")
    suspend fun delete(id: Uuid)
}
