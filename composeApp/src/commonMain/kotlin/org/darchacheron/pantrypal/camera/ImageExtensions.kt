package org.darchacheron.pantrypal.camera

import org.darthacheron.pantrypal.core.camera.ImageDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Image.toDto(serverUrl: String): ImageDto = ImageDto(
    serverId = getServerId(serverUrl) ?: Uuid.NIL,
    profileId = profileId,
    foodId = null, // Will be set by Food mapping or server
    inventoryItemId = null, // Will be set by InventoryItem mapping or server
    isPrimary = false, // Caller should override if needed
    createdAt = createdAt,
    lastModifiedAt = lastModifiedAt,
)

@OptIn(ExperimentalUuidApi::class)
fun ImageDto.toImage(serverUrl: String): Image {
    val localId = Uuid.generateV7()
    return Image(
        id = localId, // DTO doesn't have local image ID
        profileId = profileId,
        localPath = null, // Remote image has no local path initially
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        remoteImages = listOf(
            RemoteImage(
                localImageId = localId,
                localProfileId = profileId,
                serverUrl = serverUrl,
                serverId = serverId,
                lastSyncedAt = lastModifiedAt
            )
        )
    )
}
