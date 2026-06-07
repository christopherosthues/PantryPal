package org.darthacheron.pantrypal.profile

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface RemoteProfileDao {
    @Query("SELECT * FROM remote_profile WHERE localProfileId = :localProfileId AND serverUrl = :serverUrl")
    fun getRemoteProfile(localProfileId: Uuid, serverUrl: String): Flow<RemoteProfileEntity?>

    @Query("SELECT * FROM remote_profile WHERE localProfileId = :localProfileId")
    fun getRemoteProfilesByLocalProfileId(localProfileId: Uuid): Flow<List<RemoteProfileEntity>>

    @Upsert
    suspend fun upsert(remoteProfile: RemoteProfileEntity)

    @Query("DELETE FROM remote_profile WHERE localProfileId = :localProfileId AND serverUrl = :serverUrl")
    suspend fun delete(localProfileId: Uuid, serverUrl: String)
}
