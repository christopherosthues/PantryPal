package org.darchacheron.pantrypal.food

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodRepository(
    private val foodDao: FoodDao,
    private val fileSystem: FileSystem
) {
    private val loggerTag = "FoodRepository"

    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        filter: FoodFilter = FoodFilter.All,
        sort: FoodSortOrder = FoodSortOrder.Name,
        direction: FoodSortDirection = FoodSortDirection.Ascending
    ): Flow<List<Food>> {
        val currentDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return foodDao.getFilteredAndSorted(
            profileId = profileId,
            query = query,
            filter = filter.name.uppercase(),
            sort = sort.name.uppercase(),
            direction = direction.name.uppercase(),
            currentDate = currentDate
        ).map { entities -> entities.map { it.toFood() } }
    }

    suspend fun getById(id: Uuid): Food? =
        foodDao.getById(id)?.toFood()

    suspend fun upsert(food: Food) {
        val existingFood = foodDao.getById(food.id)?.toFood()
        if (existingFood != null) {
            // Delete old main image if it was changed or removed
            if (existingFood.imagePath != null && existingFood.imagePath != food.imagePath) {
                deleteImageFile(existingFood.imagePath)
            }
            // Delete old additional images if they were removed
            val removedImages = existingFood.additionalImagePaths.filter { it !in food.additionalImagePaths }
            removedImages.forEach { deleteImageFile(it) }
        }
        foodDao.upsert(food.toFoodEntity())
    }

    suspend fun delete(id: Uuid) {
        val food = foodDao.getById(id)?.toFood()
        if (food != null) {
            food.imagePath?.let { deleteImageFile(it) }
            food.additionalImagePaths.forEach { deleteImageFile(it) }
        }
        foodDao.delete(id)
    }

    private fun deleteImageFile(path: String) {
        try {
            val okioPath = path.toPath()
            if (fileSystem.exists(okioPath)) {
                fileSystem.delete(okioPath)
                Logger.withTag(loggerTag).i { "Deleted image file: $path" }
            }
        } catch (e: Exception) {
            Logger.withTag(loggerTag).e(e) { "Failed to delete image file: $path" }
        }
    }
}

enum class FoodSortOrder {
    Name,
    Date
}

enum class FoodSortDirection {
    Ascending,
    Descending
}

enum class FoodFilter {
    All,
    Opened,
    Unopened,
    Overdue
}
