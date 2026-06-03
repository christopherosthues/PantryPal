package org.darchacheron.pantrypal.inventory

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface RemoteInventoryItemDao {
    @Query("SELECT * FROM remote_inventory_item WHERE localInventoryItemId = :localInventoryItemId AND serverUrl = :serverUrl")
    fun getRemoteInventoryItem(localInventoryItemId: Uuid, serverUrl: String): Flow<RemoteInventoryItemEntity?>

    @Query("SELECT * FROM remote_inventory_item WHERE localInventoryItemId = :localInventoryItemId")
    fun getRemoteInventoryItemsByLocalInventoryItemId(localInventoryItemId: Uuid): Flow<List<RemoteInventoryItemEntity>>

    @Upsert
    suspend fun upsert(remoteInventoryItem: RemoteInventoryItemEntity)

    @Query("DELETE FROM remote_inventory_item WHERE localInventoryItemId = :localInventoryItemId AND serverUrl = :serverUrl")
    suspend fun delete(localInventoryItemId: Uuid, serverUrl: String)
}
