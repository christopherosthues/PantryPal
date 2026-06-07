package org.darthacheron.pantrypal.camera

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface RemoteImageDao {
    @Query("SELECT * FROM remote_image WHERE localImageId = :localImageId AND serverUrl = :serverUrl")
    fun getRemoteImage(localImageId: Uuid, serverUrl: String): Flow<RemoteImageEntity?>

    @Query("SELECT * FROM remote_image WHERE localImageId = :localImageId")
    fun getRemoteImagesByLocalImageId(localImageId: Uuid): Flow<List<RemoteImageEntity>>

    @Upsert
    suspend fun upsert(remoteImage: RemoteImageEntity)

    @Query("DELETE FROM remote_image WHERE localImageId = :localImageId AND serverUrl = :serverUrl")
    suspend fun delete(localImageId: Uuid, serverUrl: String)
}
