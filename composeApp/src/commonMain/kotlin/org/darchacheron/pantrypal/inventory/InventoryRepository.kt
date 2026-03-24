package org.darchacheron.pantrypal.inventory

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val loggerTag = "InventoryRepository"

    fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        sort: InventorySortOrder = InventorySortOrder.Name,
        direction: InventorySortDirection = InventorySortDirection.Ascending
    ): Flow<List<InventoryItem>> =
        inventoryItemDao.getFilteredAndSorted(
            profileId = profileId,
            query = query,
            sort = sort.name.uppercase(),
            direction = direction.name.uppercase(),
        ).map { entities -> entities.map { it.toInventoryItem() } }

    suspend fun getById(id: Uuid): InventoryItem? =
        inventoryItemDao.getById(id)?.toInventoryItem()

    suspend fun upsert(inventoryItem: InventoryItem) {
        val existingItem = inventoryItemDao.getById(inventoryItem.id)?.toInventoryItem()
        if (existingItem != null) {
            if (existingItem.imagePath != null && existingItem.imagePath != inventoryItem.imagePath) {
                deleteImageFile(existingItem.imagePath)
            }
            val removedImages = existingItem.additionalImagePaths.filter { it !in inventoryItem.additionalImagePaths }
            removedImages.forEach { deleteImageFile(it) }
        }

        // Phase 1: Save locally
        inventoryItemDao.upsert(inventoryItem.toInventoryItemEntity())

        // Phase 1: Try push immediately
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
            settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD
        ) {
            scope.launch {
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
    }

    suspend fun delete(id: Uuid) {
        val item = inventoryItemDao.getById(id)?.toInventoryItem()
        if (item != null) {
            item.imagePath?.let { deleteImageFile(it) }
            item.additionalImagePaths.forEach { deleteImageFile(it) }

            // Phase 1: Try delete on server
            val settings = settingsRepository.getSettings()
            if (item.serverId != null && (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD)
            ) {
                scope.launch {
                    try {
                        inventoryNetworkService.deleteInventoryItem(item.serverId, settings.serverUrl)
                    } catch (e: Exception) {
                        Logger.withTag(loggerTag).w { "Failed to delete item ${item.name} from server. Error: ${e.message}" }
                    }
                }
            }
        }
        inventoryItemDao.delete(id)
    }

    suspend fun syncWithServer() {
        val settings = settingsRepository.getSettings()
        if (settings.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) return

        try {
            // 1. Upload dirty records (Phase 2 - Batch)
            if (settings.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                settings.dataSynchronization == DataSynchronization.ONLY_UPLOAD
            ) {
                val dirtyEntities = inventoryItemDao.getDirtyRecords(Instant.fromEpochMilliseconds(0))
                if (dirtyEntities.isNotEmpty()) {
                    val dirtyItems = dirtyEntities.map { it.toInventoryItem() }
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
                    inventoryItemDao.upsert(remoteItem.toInventoryItemEntity())
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
