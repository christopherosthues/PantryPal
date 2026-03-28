package org.darchacheron.pantrypal.inventory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.darchacheron.pantrypal.camera.ImageEntity
import org.darchacheron.pantrypal.camera.toImageEntity
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface InventoryItemDao {
    @Transaction
    @Query("""
        SELECT * FROM inventory_item 
        WHERE profileId = :profileId
        AND (:query = '' OR name LIKE '%' || :query || '%')
        ORDER BY 
        CASE WHEN :sort = 'NAME' AND :direction = 'ASCENDING' THEN name END ASC,
        CASE WHEN :sort = 'NAME' AND :direction = 'DESCENDING' THEN name END DESC
    """)
    fun getFilteredAndSortedWithImages(
        profileId: Uuid,
        query: String,
        sort: String,
        direction: String,
    ): Flow<List<InventoryItemWithImages>>

    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        sort: String,
        direction: String,
    ): Flow<List<InventoryItemEntity>> = getFilteredAndSortedWithImages(profileId, query, sort, direction)
        .map { list -> list.map { it.inventoryItem } }

    @Transaction
    @Query("SELECT * FROM inventory_item WHERE id = :id")
    suspend fun getByIdWithImages(id: Uuid): InventoryItemWithImages?

    @Query("SELECT * FROM inventory_item WHERE id = :id")
    suspend fun getById(id: Uuid): InventoryItemEntity?

    @Upsert
    suspend fun upsertInventoryItem(inventoryItem: InventoryItemEntity)

    @Insert
    suspend fun insertImages(images: List<ImageEntity>)

    @Query("DELETE FROM images WHERE inventoryItemId = :inventoryItemId")
    suspend fun deleteImagesForInventoryItem(inventoryItemId: Uuid)

    @Transaction
    suspend fun upsert(inventoryItem: InventoryItem) {
        upsertInventoryItem(inventoryItem.toInventoryItemEntity())
        deleteImagesForInventoryItem(inventoryItem.id)
        val images = mutableListOf<ImageEntity>()
        inventoryItem.image?.let { images.add(it.toImageEntity(inventoryItemId = inventoryItem.id, isPrimary = true)) }
        inventoryItem.additionalImages.forEach { images.add(it.toImageEntity(inventoryItemId = inventoryItem.id, isPrimary = false)) }
        if (images.isNotEmpty()) {
            insertImages(images)
        }
    }

    @Query("DELETE FROM inventory_item WHERE id = :id")
    suspend fun delete(id: Uuid)

    @Query("SELECT * FROM inventory_item WHERE serverId IS NULL OR lastModifiedAt > :lastSyncTime")
    suspend fun getDirtyRecords(lastSyncTime: Instant): List<InventoryItemEntity>

    @Query("UPDATE inventory_item SET serverId = :serverId WHERE id = :id")
    suspend fun updateServerId(id: Uuid, serverId: Uuid)

    @Query("SELECT * FROM images WHERE profileId = :profileId AND (serverId IS NULL OR lastModifiedAt > :lastSyncTime)")
    suspend fun getDirtyImages(profileId: Uuid, lastSyncTime: Instant): List<ImageEntity>
}
