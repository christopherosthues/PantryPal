package org.darchacheron.pantrypal.food

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface FoodDao {
    @Query("SELECT * FROM food")
    fun getAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Uuid): FoodEntity?

    @Query("SELECT * FROM food WHERE id = :id")
    fun observeById(id: Uuid): Flow<FoodEntity?>

    @Upsert
    suspend fun upsert(food: FoodEntity)

    @Query("DELETE FROM food WHERE id = :id")
    suspend fun delete(id: Uuid)
}
