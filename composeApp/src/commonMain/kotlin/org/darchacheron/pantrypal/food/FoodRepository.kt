package org.darchacheron.pantrypal.food

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.FileSystem
import okio.Path.Companion.toPath
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.settings.DataSynchronization
import org.darchacheron.pantrypal.settings.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodRepository(
    private val foodDao: FoodDao,
    private val foodNetworkService: FoodNetworkService,
    private val settingsRepository: SettingsRepository,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val fileSystem: FileSystem,
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
        return foodDao.getFilteredAndSortedWithImages(
            profileId = profileId,
            query = query,
            filter = filter.name.uppercase(),
            sort = sort.name.uppercase(),
            direction = direction.name.uppercase(),
            currentDate = currentDate
        ).map { list -> list.map { it.toFood() } }
    }

    suspend fun getById(id: Uuid): Food? = withContext(Dispatchers.IO) {
        foodDao.getByIdWithImages(id)?.toFood()
    }

    suspend fun upsert(food: Food) = withContext(Dispatchers.IO) {
        val existingWithImages = foodDao.getByIdWithImages(food.id)
        if (existingWithImages != null) {
            val existingFood = existingWithImages.toFood()
            
            // Delete old main image if it was changed or removed
            if (existingFood.image?.localPath != null && existingFood.image.localPath != food.image?.localPath) {
                deleteImageFile(existingFood.image.localPath)
            }
            
            // Delete old additional images if they were removed
            val newPaths = food.additionalImages.mapNotNull { it.localPath }
            existingFood.additionalImages.forEach { existingImage ->
                if (existingImage.localPath != null && existingImage.localPath !in newPaths) {
                    deleteImageFile(existingImage.localPath)
                }
            }
        }
        
        // Phase 1: Save locally
        foodDao.upsert(food)
        
        // Phase 1: Try push immediately if enabled
        val settings = settingsRepository.getSettings()
        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val canSync = prefs?.isLoggedInRemotely == true

        if (canSync && (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
            settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD)) {
            try {
                val syncedFoods = foodNetworkService.pushFoods(listOf(food), prefs!!.serverUrl)
                syncedFoods.firstOrNull()?.let { synced ->
                    foodDao.updateServerId(food.id, synced.serverId!!)
                }
                Logger.withTag(loggerTag).i { "Successfully synced food ${food.name} to server" }
            } catch (e: Exception) {
                Logger.withTag(loggerTag).w { "Failed immediate sync for ${food.name}, will retry in background. Error: ${e.message}" }
            }
        }
    }

    suspend fun delete(id: Uuid) = withContext(Dispatchers.IO) {
        val foodWithImages = foodDao.getByIdWithImages(id)
        if (foodWithImages != null) {
            val food = foodWithImages.toFood()
            food.image?.localPath?.let { deleteImageFile(it) }
            food.additionalImages.forEach { it.localPath?.let { path -> deleteImageFile(path) } }
            
            // Phase 1: Try delete on server
            val settings = settingsRepository.getSettings()
            val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
            val canSync = prefs?.isLoggedInRemotely == true

            if (canSync && food.serverId != null && (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
                settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD)) {
                try {
                    foodNetworkService.deleteFood(food.serverId, prefs!!.serverUrl)
                } catch (e: Exception) {
                    Logger.withTag(loggerTag).w { "Failed to delete food ${food.name} from server. Error: ${e.message}" }
                }
            }
        }
        foodDao.delete(id)
    }

    suspend fun syncWithServer() = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) return@withContext

        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        if (prefs?.isLoggedInRemotely != true) {
            Logger.withTag(loggerTag).d { "Skipping sync: Not logged in to remote" }
            return@withContext
        }

        try {
            // 1. Upload dirty records (Phase 2)
            if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
                settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD) {
                
                val dirtyEntities = foodDao.getDirtyRecords(Instant.fromEpochMilliseconds(0)) // TODO: store instant of last sync
                if (dirtyEntities.isNotEmpty()) {
                    val dirtyFoods = dirtyEntities.mapNotNull { foodDao.getByIdWithImages(it.id)?.toFood() }
                    try {
                        val syncedFoods = foodNetworkService.pushFoods(dirtyFoods, prefs.serverUrl)
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
                val remoteChanges = foodNetworkService.fetchChanges(Instant.fromEpochMilliseconds(0), prefs.serverUrl)
                remoteChanges.forEach { remoteFood ->
                    val local = foodDao.getByIdWithImages(remoteFood.id)?.toFood()
                    if (local == null || remoteFood.lastModifiedAt > local.lastModifiedAt) {
                        foodDao.upsert(remoteFood)
                    }
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
