package org.darchacheron.pantrypal.food

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface RemoteFoodDao {
    @Query("SELECT * FROM remote_food WHERE localFoodId = :localFoodId AND serverUrl = :serverUrl")
    fun getRemoteFood(localFoodId: Uuid, serverUrl: String): Flow<RemoteFoodEntity?>

    @Query("SELECT * FROM remote_food WHERE localFoodId = :localFoodId")
    fun getRemoteFoodsByLocalFoodId(localFoodId: Uuid): Flow<List<RemoteFoodEntity>>

    @Upsert
    suspend fun upsert(remoteFood: RemoteFoodEntity)

    @Query("DELETE FROM remote_food WHERE localFoodId = :localFoodId AND serverUrl = :serverUrl")
    suspend fun delete(localFoodId: Uuid, serverUrl: String)
}
