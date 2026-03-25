package org.darchacheron.pantrypal.food

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.FileSystem
import okio.Path.Companion.toPath
import org.darchacheron.pantrypal.settings.DataSynchronization
import org.darchacheron.pantrypal.settings.SettingsRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodRepository(
    private val foodDao: FoodDao,
    private val foodNetworkService: FoodNetworkService,
    private val settingsRepository: SettingsRepository,
    private val fileSystem: FileSystem,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
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
        
        // Phase 1: Save locally
        foodDao.upsert(food.toFoodEntity())
        
        // Phase 1: Try push immediately if enabled
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
            settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD) {
            scope.launch {
                try {
                    val syncedFoods = foodNetworkService.pushFoods(listOf(food), settings.serverUrl)
                    syncedFoods.firstOrNull()?.let { synced ->
                        foodDao.updateServerId(food.id, synced.serverId!!)
                    }
                    Logger.withTag(loggerTag).i { "Successfully synced food ${food.name} to server" }
                } catch (e: Exception) {
                    Logger.withTag(loggerTag).w { "Failed immediate sync for ${food.name}, will retry in background. Error: ${e.message}" }
                }
            }
        }
    }

    suspend fun delete(id: Uuid) {
        val food = foodDao.getById(id)?.toFood()
        if (food != null) {
            food.imagePath?.let { deleteImageFile(it) }
            food.additionalImagePaths.forEach { deleteImageFile(it) }
            
            // Phase 1: Try delete on server
            val settings = settingsRepository.getSettings()
            if (food.serverId != null && (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
                settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD)) {
                scope.launch {
                    try {
                        foodNetworkService.deleteFood(food.serverId, settings.serverUrl)
                    } catch (e: Exception) {
                        Logger.withTag(loggerTag).w { "Failed to delete food ${food.name} from server. Error: ${e.message}" }
                    }
                }
            }
        }
        foodDao.delete(id)
    }

    suspend fun syncWithServer() {
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) return

        try {
            // 1. Upload dirty records (Phase 2)
            if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
                settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD) {
                
                val dirtyEntities = foodDao.getDirtyRecords(Instant.fromEpochMilliseconds(0)) // TODO: store instant of last sync
                if (dirtyEntities.isNotEmpty()) {
                    val dirtyFoods = dirtyEntities.map { it.toFood() }
                    try {
                        val syncedFoods = foodNetworkService.pushFoods(dirtyFoods, settings.serverUrl)
                        syncedFoods.forEach { synced ->
                            foodDao.updateServerId(synced.id, synced.serverId!!)
                        }
                    } catch (e: Exception) {
                        Logger.withTag(loggerTag).e { "Failed to upload batch: ${e.message}" }
                    }
                }
            }

            // 2. Download changes
            if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
                settings.dataSynchronization == DataSynchronization.ONLY_DOWNLOAD) {
                val remoteChanges = foodNetworkService.fetchChanges(Instant.fromEpochMilliseconds(0), settings.serverUrl)
                remoteChanges.forEach { remoteFood ->
                    foodDao.upsert(remoteFood.toFoodEntity())
                }
            }
        } catch (e: Exception) {
            Logger.withTag(loggerTag).e { "Sync failed: ${e.message}" }
        }
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
