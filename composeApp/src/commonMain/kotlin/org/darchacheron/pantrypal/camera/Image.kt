package org.darchacheron.pantrypal.camera

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Image(
    val id: Uuid = Uuid.generateV7(),
    val serverId: Uuid? = null,
    val profileId: Uuid,
    val localPath: String? = null,
    val createdAt: Instant,
    val lastModifiedAt: Instant
)
