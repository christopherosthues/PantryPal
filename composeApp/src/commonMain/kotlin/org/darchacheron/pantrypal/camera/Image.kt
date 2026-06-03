package org.darchacheron.pantrypal.camera

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class RemoteImage(
    val localImageId: Uuid,
    val serverUrl: String,
    val serverId: Uuid,
    val lastSyncedAt: Instant? = null
)

@OptIn(ExperimentalUuidApi::class)
data class Image(
    val id: Uuid = Uuid.generateV7(),
    val serverId: Uuid? = null,
    val serverUrl: String? = null,
    val profileId: Uuid,
    val localPath: String? = null,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val remoteImages: List<RemoteImage> = emptyList()
)
