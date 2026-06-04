package org.darchacheron.pantrypal.camera

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class RemoteImage(
    val localImageId: Uuid,
    val localProfileId: Uuid,
    val serverUrl: String,
    val serverId: Uuid,
    val lastSyncedAt: Instant? = null
)

@OptIn(ExperimentalUuidApi::class)
data class Image(
    val id: Uuid = Uuid.generateV7(),
    val profileId: Uuid,
    val localPath: String? = null,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val remoteImages: List<RemoteImage> = emptyList()
) {
    fun getServerId(serverUrl: String): Uuid? = remoteImages.find { it.serverUrl == serverUrl }?.serverId
}
