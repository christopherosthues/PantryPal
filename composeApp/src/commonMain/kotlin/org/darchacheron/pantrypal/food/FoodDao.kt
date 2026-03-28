package org.darchacheron.pantrypal.food

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.darchacheron.pantrypal.camera.ImageEntity
import org.darchacheron.pantrypal.camera.toImageEntity
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface FoodDao {
    @Transaction
    @Query("""
        SELECT * FROM food 
        WHERE profileId = :profileId
        AND (:query = '' OR name LIKE '%' || :query || '%')
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
    fun getFilteredAndSortedWithImages(
        profileId: Uuid,
        query: String,
        filter: String,
        sort: String,
        direction: String,
        currentDate: LocalDate
    ): Flow<List<FoodWithImages>>

    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        filter: String,
        sort: String,
        direction: String,
        currentDate: LocalDate
    ): Flow<List<FoodEntity>> = getFilteredAndSortedWithImages(profileId, query, filter, sort, direction, currentDate)
        .map { list -> list.map { it.food } }

    @Transaction
    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getByIdWithImages(id: Uuid): FoodWithImages?

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Uuid): FoodEntity?

    @Upsert
    suspend fun upsertFood(food: FoodEntity)

    @Insert
    suspend fun insertImages(images: List<ImageEntity>)

    @Query("DELETE FROM images WHERE foodId = :foodId")
    suspend fun deleteImagesForFood(foodId: Uuid)

    @Transaction
    suspend fun upsert(food: Food) {
        upsertFood(food.toFoodEntity())
        // For copy operations, we don't want to copy images. 
        // We only upsert images if they are provided in the domain object.
        // The repository should handle providing/clearing these.
        deleteImagesForFood(food.id)
        val images = mutableListOf<ImageEntity>()
        food.image?.let { images.add(it.toImageEntity(foodId = food.id, isPrimary = true)) }
        images.addAll(food.additionalImages.map { it.toImageEntity(foodId = food.id, isPrimary = false) })
        if (images.isNotEmpty()) {
            insertImages(images)
        }
    }

    @Query("DELETE FROM food WHERE id = :id")
    suspend fun delete(id: Uuid)

    @Query("SELECT * FROM food WHERE serverId IS NULL OR lastModifiedAt > :lastSyncTime")
    suspend fun getDirtyRecords(lastSyncTime: Instant): List<FoodEntity>

    @Query("UPDATE food SET serverId = :serverId WHERE id = :id")
    suspend fun updateServerId(id: Uuid, serverId: Uuid)

    @Query("SELECT * FROM images WHERE profileId = :profileId AND (serverId IS NULL OR lastModifiedAt > :lastSyncTime)")
    suspend fun getDirtyImages(profileId: Uuid, lastSyncTime: Instant): List<ImageEntity>
}
