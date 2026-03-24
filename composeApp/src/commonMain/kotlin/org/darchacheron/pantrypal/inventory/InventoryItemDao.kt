package org.darchacheron.pantrypal.inventory

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface InventoryItemDao {
    @Query("""
        SELECT * FROM inventory_item 
        WHERE profileId = :profileId
        AND (:query = '' OR name LIKE '%' || :query || '%')
        ORDER BY 
        CASE WHEN :sort = 'NAME' AND :direction = 'ASCENDING' THEN name END ASC,
        CASE WHEN :sort = 'NAME' AND :direction = 'DESCENDING' THEN name END DESC
    """)
    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        sort: String,
        direction: String,
    ): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_item WHERE id = :id")
    suspend fun getById(id: Uuid): InventoryItemEntity?

    @Upsert
    suspend fun upsert(inventoryItem: InventoryItemEntity)

    @Query("DELETE FROM inventory_item WHERE id = :id")
    suspend fun delete(id: Uuid)

    @Query("SELECT * FROM inventory_item WHERE serverId IS NULL OR lastModifiedAt > :lastSyncTime")
    suspend fun getDirtyRecords(lastSyncTime: Instant): List<InventoryItemEntity>

    @Query("UPDATE inventory_item SET serverId = :serverId WHERE id = :id")
    suspend fun updateServerId(id: Uuid, serverId: Uuid)
}
