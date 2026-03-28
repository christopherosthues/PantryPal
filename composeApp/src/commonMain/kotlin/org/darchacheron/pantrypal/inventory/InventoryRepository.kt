package org.darchacheron.pantrypal.inventory

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.darchacheron.pantrypal.settings.DataSynchronization
import org.darchacheron.pantrypal.settings.SettingsRepository
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryRepository(
    private val inventoryItemDao: InventoryItemDao,
    private val inventoryNetworkService: InventoryNetworkService,
    private val settingsRepository: SettingsRepository,
    private val fileSystem: FileSystem,
) {
    private val loggerTag = "InventoryRepository"

    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        sort: InventorySortOrder = InventorySortOrder.Name,
        direction: InventorySortDirection = InventorySortDirection.Ascending
    ): Flow<List<InventoryItem>> =
        inventoryItemDao.getFilteredAndSortedWithImages(
            profileId = profileId,
            query = query,
            sort = sort.name.uppercase(),
            direction = direction.name.uppercase(),
        ).map { list -> list.map { it.toInventoryItem() } }

    suspend fun getById(id: Uuid): InventoryItem? = withContext(Dispatchers.IO) {
        inventoryItemDao.getByIdWithImages(id)?.toInventoryItem()
    }

    suspend fun upsert(inventoryItem: InventoryItem) = withContext(Dispatchers.IO) {
        val existingWithImages = inventoryItemDao.getByIdWithImages(inventoryItem.id)
        if (existingWithImages != null) {
            val existingItem = existingWithImages.toInventoryItem()
            if (existingItem.image?.localPath != null && existingItem.image.localPath != inventoryItem.image?.localPath) {
                deleteImageFile(existingItem.image.localPath)
            }
            val newPaths = inventoryItem.additionalImages.mapNotNull { it.localPath }
            existingItem.additionalImages.forEach { existingImage ->
                if (existingImage.localPath != null && existingImage.localPath !in newPaths) {
                    deleteImageFile(existingImage.localPath)
                }
            }
        }

        // Phase 1: Save locally
        inventoryItemDao.upsert(inventoryItem)

        // Phase 1: Try push immediately
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
            settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD
        ) {
            try {
                val syncedItems = inventoryNetworkService.pushInventoryItems(listOf(inventoryItem), settings.serverUrl)
                syncedItems.firstOrNull()?.let { synced ->
                    inventoryItemDao.updateServerId(inventoryItem.id, synced.serverId!!)
                }
                Logger.withTag(loggerTag).i { "Successfully synced inventory item ${inventoryItem.name} to server" }
            } catch (e: Exception) {
                Logger.withTag(loggerTag).w { "Failed immediate sync for ${inventoryItem.name}. Error: ${e.message}" }
            }
        }
    }

    suspend fun delete(id: Uuid) = withContext(Dispatchers.IO) {
        val itemWithImages = inventoryItemDao.getByIdWithImages(id)
        if (itemWithImages != null) {
            val item = itemWithImages.toInventoryItem()
            item.image?.localPath?.let { deleteImageFile(it) }
            item.additionalImages.forEach { it.localPath?.let { path -> deleteImageFile(path) } }

            // Phase 1: Try delete on server
            val settings = settingsRepository.getSettings()
            if (item.serverId != null && (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD)
            ) {
                try {
                    inventoryNetworkService.deleteInventoryItem(item.serverId, settings.serverUrl)
                } catch (e: Exception) {
                    Logger.withTag(loggerTag).w { "Failed to delete item ${item.name} from server. Error: ${e.message}" }
                }
            }
        }
        inventoryItemDao.delete(id)
    }

    suspend fun syncWithServer() = withContext(Dispatchers.IO) {
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) return@withContext

        try {
            // 1. Upload dirty records (Phase 2 - Batch)
            if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD
            ) {
                val dirtyEntities = inventoryItemDao.getDirtyRecords(Instant.fromEpochMilliseconds(0))
                if (dirtyEntities.isNotEmpty()) {
                    val dirtyItems = dirtyEntities.mapNotNull { inventoryItemDao.getByIdWithImages(it.id)?.toInventoryItem() }
                    try {
                        val syncedItems = inventoryNetworkService.pushInventoryItems(dirtyItems, settings.serverUrl)
                        syncedItems.forEach { synced ->
                            inventoryItemDao.updateServerId(synced.id, synced.serverId!!)
                        }
                    } catch (e: Exception) {
                        Logger.withTag(loggerTag).e { "Failed to upload inventory batch: ${e.message}" }
                    }
                }
            }

            // 2. Download changes
            if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                settings.dataSynchronization == DataSynchronization.ONLY_DOWNLOAD
            ) {
                val remoteChanges = inventoryNetworkService.fetchChanges(Instant.fromEpochMilliseconds(0), settings.serverUrl)
                remoteChanges.forEach { remoteItem ->
                    val local = inventoryItemDao.getByIdWithImages(remoteItem.id)?.toInventoryItem()
                    if (local == null || remoteItem.lastModifiedAt > local.lastModifiedAt) {
                        inventoryItemDao.upsert(remoteItem)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.withTag(loggerTag).e { "Inventory sync failed: ${e.message}" }
        }
    }

    private fun deleteImageFile(path: String) {
        try {
            val okioPath = path.toPath()
            if (fileSystem.exists(okioPath)) {
                fileSystem.delete(okioPath)
            }
        } catch (e: Exception) {
            Logger.withTag(loggerTag).e(e) { "Failed to delete image file: $path" }
        }
    }
}

enum class InventorySortOrder {
    Name,
}

enum class InventorySortDirection {
    Ascending,
    Descending
}
