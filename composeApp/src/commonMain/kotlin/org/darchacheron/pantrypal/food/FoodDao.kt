package org.darchacheron.pantrypal.food

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface FoodDao {
    @Query("""
        SELECT * FROM food 
        WHERE (:query = '' OR name LIKE '%' || :query || '%')
        AND (
            :filter = 'ALL' 
            OR (:filter = 'OPENED' AND openedAt IS NOT NULL)
            OR (:filter = 'UNOPENED' AND openedAt IS NULL)
            OR (:filter = 'OVERDUE' AND bestBeforeUsedByDate IS NOT NULL AND bestBeforeUsedByDate < :currentDate)
        )
        ORDER BY 
        CASE WHEN :sort = 'NAME' AND :direction = 'ASCENDING' THEN name END ASC,
        CASE WHEN :sort = 'NAME' AND :direction = 'DESCENDING' THEN name END DESC,
        CASE WHEN :sort = 'DATE' AND :direction = 'ASCENDING' THEN bestBeforeUsedByDate END ASC,
        CASE WHEN :sort = 'DATE' AND :direction = 'DESCENDING' THEN bestBeforeUsedByDate END DESC
    """)
    fun getFilteredAndSorted(
        query: String,
        filter: String,
        sort: String,
        direction: String,
        currentDate: LocalDate
    ): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Uuid): FoodEntity?

    @Upsert
    suspend fun upsert(food: FoodEntity)

    @Query("DELETE FROM food WHERE id = :id")
    suspend fun delete(id: Uuid)
}
