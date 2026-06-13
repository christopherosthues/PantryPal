package org.darthacheron.pantrypal.inventory

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.camera.Image
import org.darthacheron.pantrypal.camera.RemoteImageDao
import org.darthacheron.pantrypal.camera.RemoteImageEntity
import org.darthacheron.pantrypal.networking.ImageNetworkService
import org.darthacheron.pantrypal.profile.ProfileDao
import org.darthacheron.pantrypal.settings.DataSynchronization
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryRepositoryImpl(
    private val inventoryItemDao: InventoryItemDao,
    private val remoteInventoryItemDao: RemoteInventoryItemDao,
    private val profileDao: ProfileDao,
    private val inventoryNetworkService: InventoryNetworkService,
    private val imageNetworkService: ImageNetworkService,
    private val remoteImageDao: RemoteImageDao,
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val fileSystem: FileSystem,
) : InventoryRepository {
    private val loggerTag = "InventoryRepository"

    override fun getFilteredAndSorted(
        profileId: Uuid,
        query: String,
        sort: InventorySortOrder,
        direction: InventorySortDirection,
    ): Flow<List<InventoryItem>> =
        inventoryItemDao.getFilteredAndSortedWithImages(
            profileId = profileId,
            query = query,
            sort = sort.name.uppercase(),
            direction = direction.name.uppercase(),
        ).map { list -> list.map { it.toInventoryItem() } }

    override suspend fun getById(id: Uuid): Result<InventoryItem?> = withContext(Dispatchers.IO) {
        runCatching {
            inventoryItemDao.getByIdWithImages(id)?.toInventoryItem()
        }
    }

    override suspend fun upsert(inventoryItem: InventoryItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
            val serverUrl = prefs?.serverUrl ?: ""

            val existingWithImages = inventoryItemDao.getByIdWithImages(inventoryItem.id)
            val imagesToDeleteOnServer = mutableListOf<Uuid>()

            if (existingWithImages != null) {
                val existingItem = existingWithImages.toInventoryItem()

                // Main image
                if (existingItem.image?.localPath != null && existingItem.image.localPath != inventoryItem.image?.localPath) {
                    deleteImageFile(existingItem.image.localPath)
                }

                val existingMainServerId = existingItem.image?.getServerId(serverUrl)
                val newMainServerId = inventoryItem.image?.getServerId(serverUrl)
                if (existingMainServerId != null && existingMainServerId != newMainServerId) {
                    imagesToDeleteOnServer.add(existingMainServerId)
                }

                // Additional images
                val newPaths = inventoryItem.additionalImages.mapNotNull { it.localPath }
                val newServerIds = inventoryItem.additionalImages.mapNotNull { it.getServerId(serverUrl) }

                existingItem.additionalImages.forEach { existingImage ->
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
            inventoryItemDao.upsert(inventoryItem)

            // Phase 1: Try push immediately
            val profileId = prefs?.localProfileId?.let { if (it.isNotBlank()) Uuid.parse(it) else null }
            val profile = profileId?.let { profileDao.getProfileById(it).firstOrNull() }
            val canSync = prefs?.isLoggedInRemotely == true

            if (canSync && profile != null && (profile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                        profile.dataSynchronization == DataSynchronization.ONLY_UPLOAD
                        )
            ) {
                val serverId = inventoryItem.getServerId(serverUrl)
                val syncResult = if (serverId == null) {
                    inventoryNetworkService.createInventoryItem(inventoryItem.toDto(serverUrl), serverUrl)
                } else {
                    inventoryNetworkService.updateInventoryItem(inventoryItem.toDto(serverUrl), serverUrl)
                }

                syncResult.onSuccess { serverItem ->
                    if (serverItem != null) {
                        val serverItemId = serverItem.serverId!!
                        remoteInventoryItemDao.upsert(
                            RemoteInventoryItemEntity(
                                inventoryItem.id,
                                inventoryItem.profileId,
                                serverUrl,
                                serverItemId,
                                Clock.System.now()
                            )
                        )

                        // Delete removed images from server
                        imagesToDeleteOnServer.forEach { imageServerId ->
                            imageNetworkService.deleteInventoryImage(serverItemId, imageServerId, serverUrl)
                        }

                        // Upload images if any
                        val imagesToUpload = mutableListOf<Pair<Image, Boolean>>()
                        inventoryItem.image?.let { imagesToUpload.add(it to true) }
                        inventoryItem.additionalImages.forEach { imagesToUpload.add(it to false) }

                        imagesToUpload.forEach { (image, isPrimary) ->
                            val existingRemoteImage = image.getServerId(serverUrl)
                            if (existingRemoteImage == null && image.localPath != null) {
                                val path = image.localPath.toPath()
                                if (fileSystem.exists(path)) {
                                    val bytes = fileSystem.read(path) { readByteArray() }
                                    imageNetworkService.uploadInventoryImage(serverItemId, bytes, isPrimary, serverUrl)
                                        .onSuccess { imageDto ->
                                            remoteImageDao.upsert(
                                                RemoteImageEntity(
                                                    image.id,
                                                    inventoryItem.profileId,
                                                    serverUrl,
                                                    imageDto.serverId,
                                                    Clock.System.now()
                                                )
                                            )
                                        }
                                }
                            }
                        }
                        Logger.withTag(loggerTag).i { "Successfully synced inventory item ${inventoryItem.name} to server" }
                    }
                }.onFailure { e ->
                    Logger.withTag(loggerTag).w { "Failed immediate sync for ${inventoryItem.name}. Error: ${e.message}" }
                }
            }
        }
    }

    override suspend fun delete(id: Uuid): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val itemWithImages = inventoryItemDao.getByIdWithImages(id)
            if (itemWithImages != null) {
                val item = itemWithImages.toInventoryItem()
                item.image?.localPath?.let { deleteImageFile(it) }
                item.additionalImages.forEach { it.localPath?.let { path -> deleteImageFile(path) } }

                // Phase 1: Try delete on server
                val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
                val profileId = prefs?.localProfileId?.let { if (it.isNotBlank()) Uuid.parse(it) else null }
                val profile = profileId?.let { profileDao.getProfileById(it).firstOrNull() }
                val canSync = prefs?.isLoggedInRemotely == true

                val serverId = item.getServerId(prefs?.serverUrl ?: "")
                if (canSync && profile != null && serverId != null && (profile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                            profile.dataSynchronization == DataSynchronization.ONLY_UPLOAD)
                ) {
                    inventoryNetworkService.deleteInventoryItem(serverId, prefs.serverUrl)
                        .onFailure { e ->
                            Logger.withTag(loggerTag)
                                .w { "Failed to delete item ${item.name} from server. Error: ${e.message}" }
                        }
                }
            }
            inventoryItemDao.delete(id)
        }
    }

    override suspend fun syncWithServer(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
            val profileId = prefs?.localProfileId?.let { if (it.isNotBlank()) Uuid.parse(it) else null }
            val profile = profileId?.let { profileDao.getProfileById(it).firstOrNull() } ?: return@runCatching

            if (profile.dataSynchronization == DataSynchronization.NO_SYNCHRONIZATION) return@runCatching

            if (!prefs.isLoggedInRemotely) {
                Logger.withTag(loggerTag).d { "Skipping inventory sync: Not logged in to remote" }
                return@runCatching
            }

            // 1. Upload dirty records (Phase 2 - Batch)
            if (profile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                profile.dataSynchronization == DataSynchronization.ONLY_UPLOAD
            ) {
                val dirtyEntities = inventoryItemDao.getDirtyRecords(Instant.fromEpochMilliseconds(0))
                if (dirtyEntities.isNotEmpty()) {
                    val dirtyItems =
                        dirtyEntities.mapNotNull { inventoryItemDao.getByIdWithImages(it.id)?.toInventoryItem() }
                    inventoryNetworkService.pushInventoryItems(
                        dirtyItems.map { it.toDto(prefs.serverUrl) },
                        prefs.serverUrl
                    ).onSuccess { syncedItems ->
                        syncedItems.forEach { synced ->
                            val localItem = dirtyItems.find { it.id == synced.clientId }
                            if (localItem != null) {
                                remoteInventoryItemDao.upsert(
                                    RemoteInventoryItemEntity(
                                        synced.clientId,
                                        localItem.profileId,
                                        prefs.serverUrl,
                                        synced.serverId!!,
                                        Clock.System.now()
                                    )
                                )
                            }
                        }
                    }.onFailure { e ->
                        Logger.withTag(loggerTag).e { "Failed to upload inventory batch: ${e.message}" }
                    }
                }
            }

            // 2. Download changes
            if (profile.dataSynchronization == DataSynchronization.UPLOAD_AND_DOWNLOAD ||
                profile.dataSynchronization == DataSynchronization.ONLY_DOWNLOAD
            ) {
                inventoryNetworkService.fetchChanges(Instant.fromEpochMilliseconds(0), prefs.serverUrl)
                    .onSuccess { remoteChanges ->
                        remoteChanges.forEach { remoteItem ->
                            val local = inventoryItemDao.getByIdWithImages(remoteItem.clientId)?.toInventoryItem()
                            if (local == null || remoteItem.lastModifiedAt > local.lastModifiedAt) {
                                val item = remoteItem.toInventoryItem(prefs.serverUrl)
                                inventoryItemDao.upsert(item)
                                remoteInventoryItemDao.upsert(
                                    RemoteInventoryItemEntity(
                                        item.id,
                                        item.profileId,
                                        prefs.serverUrl,
                                        remoteItem.serverId!!,
                                        Clock.System.now()
                                    )
                                )
                            }
                        }
                    }.onFailure { e ->
                        Logger.withTag(loggerTag).e { "Inventory sync failed: ${e.message}" }
                    }
            }
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
