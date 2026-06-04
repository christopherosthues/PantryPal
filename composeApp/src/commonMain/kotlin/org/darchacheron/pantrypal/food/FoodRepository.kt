package org.darchacheron.pantrypal.food

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import okio.FileSystem
import okio.Path.Companion.toPath
import org.darchacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darchacheron.pantrypal.camera.RemoteImageDao
import org.darchacheron.pantrypal.camera.RemoteImageEntity
import org.darchacheron.pantrypal.networking.ImageNetworkService
import org.darchacheron.pantrypal.profile.ProfileDao
import org.darchacheron.pantrypal.settings.DataSynchronization
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodRepository(
    private val foodDao: FoodDao,
    private val remoteFoodDao: RemoteFoodDao,
    private val profileDao: ProfileDao,
    private val foodNetworkService: FoodNetworkService,
    private val imageNetworkService: ImageNetworkService,
    private val remoteImageDao: RemoteImageDao,
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
        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val serverUrl = prefs?.serverUrl ?: ""
        
        val existingWithImages = foodDao.getByIdWithImages(food.id)
        val imagesToDeleteOnServer = mutableListOf<Uuid>()

        if (existingWithImages != null) {
            val existingFood = existingWithImages.toFood()
            
            // Delete old main image if it was changed or removed
            if (existingFood.image?.localPath != null && existingFood.image.localPath != food.image?.localPath) {
                deleteImageFile(existingFood.image.localPath)
            }
            
            val existingMainServerId = existingFood.image?.getServerId(serverUrl)
            val newMainServerId = food.image?.getServerId(serverUrl)
            if (existingMainServerId != null && existingMainServerId != newMainServerId) {
                imagesToDeleteOnServer.add(existingMainServerId)
            }
            
            // Delete old additional images if they were removed
            val newPaths = food.additionalImages.mapNotNull { it.localPath }
            val newServerIds = food.additionalImages.mapNotNull { it.getServerId(serverUrl) }

            existingFood.additionalImages.forEach { existingImage ->
                if (existingImage.localPath != null && existingImage.localPath !in newPaths) {
                    deleteImageFile(existingImage.localPath)
                }
                
                val existingImageServerId = existingImage.getServerId(serverUrl)
                if (existingImageServerId != null && existingImageServerId !in newServerIds) {
                    imagesToDeleteOnServer.add(existingImageServerId)
                }
            }
        }
        
        // Phase 1: Save locally
        foodDao.upsert(food)
        
        // Phase 1: Try push immediately if enabled
        val profileId = prefs?.localProfileId?.let { if (it.isNotBlank()) Uuid.parse(it) else null }
        val profile = profileId?.let { profileDao.getProfileById(it).firstOrNull() }
        val canSync = prefs?.isLoggedInRemotely == true

        if (canSync && profile != null && (profile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
            profile.dataSynchronization == DataSynchronization.ONLY_UPLOAD)) {
            try {
                val syncedFoods = foodNetworkService.pushFoods(listOf(food.toDto(serverUrl)), serverUrl)
                val serverFood = syncedFoods.firstOrNull()
                if (serverFood != null) {
                    val serverFoodId = serverFood.serverId!!
                    remoteFoodDao.upsert(RemoteFoodEntity(food.id, serverUrl, serverFoodId, Clock.System.now()))

                    // Delete removed images from server
                    imagesToDeleteOnServer.forEach { imageServerId ->
                        imageNetworkService.deleteFoodImage(serverFoodId, imageServerId, serverUrl)
                    }
                    
                    // Upload images if any
                    val imagesToUpload = mutableListOf<Pair<org.darchacheron.pantrypal.camera.Image, Boolean>>()
                    food.image?.let { imagesToUpload.add(it to true) }
                    food.additionalImages.forEach { imagesToUpload.add(it to false) }
                    
                    imagesToUpload.forEach { (image, isPrimary) ->
                        val existingRemoteImage = image.getServerId(serverUrl)
                        if (existingRemoteImage == null && image.localPath != null) {
                            val path = image.localPath.toPath()
                            if (fileSystem.exists(path)) {
                                val bytes = fileSystem.read(path) { readByteArray() }
                                imageNetworkService.uploadFoodImage(serverFoodId, bytes, isPrimary, serverUrl)
                                    .onSuccess { imageDto ->
                                        remoteImageDao.upsert(RemoteImageEntity(image.id, serverUrl, imageDto.serverId, Clock.System.now()))
                                    }
                            }
                        }
                    }
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
            val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
            val profileId = prefs?.localProfileId?.let { if (it.isNotBlank()) Uuid.parse(it) else null }
            val profile = profileId?.let { profileDao.getProfileById(it).firstOrNull() }
            val canSync = prefs?.isLoggedInRemotely == true

            val serverId = food.getServerId(prefs?.serverUrl ?: "")
            if (canSync && profile != null && serverId != null && (profile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
                profile.dataSynchronization == DataSynchronization.ONLY_UPLOAD)) {
                try {
                    foodNetworkService.deleteFood(serverId, prefs.serverUrl)
                } catch (e: Exception) {
                    Logger.withTag(loggerTag).w { "Failed to delete food ${food.name} from server. Error: ${e.message}" }
                }
            }
        }
        foodDao.delete(id)
    }

    suspend fun syncWithServer() = withContext(Dispatchers.IO) {
        val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
        val profileId = prefs?.localProfileId?.let { if (it.isNotBlank()) Uuid.parse(it) else null }
        val profile = profileId?.let { profileDao.getProfileById(it).firstOrNull() } ?: return@withContext
        
        if (profile.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) return@withContext

        if (!prefs.isLoggedInRemotely) {
            Logger.withTag(loggerTag).d { "Skipping sync: Not logged in to remote" }
            return@withContext
        }

        try {
            // 1. Upload dirty records (Phase 2)
            if (profile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
                profile.dataSynchronization == DataSynchronization.ONLY_UPLOAD) {
                
                val dirtyEntities = foodDao.getDirtyRecords(Instant.fromEpochMilliseconds(0)) // TODO: store instant of last sync
                if (dirtyEntities.isNotEmpty()) {
                    val dirtyFoods = dirtyEntities.mapNotNull { foodDao.getByIdWithImages(it.id)?.toFood() }
                    try {
                        val syncedFoods = foodNetworkService.pushFoods(dirtyFoods.map { it.toDto(prefs.serverUrl) }, prefs.serverUrl)
                        syncedFoods.forEach { synced ->
                            remoteFoodDao.upsert(RemoteFoodEntity(synced.clientId, prefs.serverUrl, synced.serverId!!, Clock.System.now()))
                        }
                    } catch (e: Exception) {
                        Logger.withTag(loggerTag).e { "Failed to upload batch: ${e.message}" }
                    }
                }
            }

            // 2. Download changes
            if (profile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD || 
                profile.dataSynchronization == DataSynchronization.ONLY_DOWNLOAD) {
                val remoteChanges = foodNetworkService.fetchChanges(Instant.fromEpochMilliseconds(0), prefs.serverUrl)
                remoteChanges.forEach { remoteFood ->
                    val local = foodDao.getByIdWithImages(remoteFood.clientId)?.toFood()
                    if (local == null || remoteFood.lastModifiedAt > local.lastModifiedAt) {
                        val food = remoteFood.toFood(prefs.serverUrl)
                        foodDao.upsert(food)
                        remoteFoodDao.upsert(RemoteFoodEntity(food.id, prefs.serverUrl, remoteFood.serverId!!, Clock.System.now()))
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
