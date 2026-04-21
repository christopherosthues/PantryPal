package org.darthacheron.pantrypal.shared.camera

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class ImageDto(
    val serverId: Uuid,
    val profileId: Uuid,
    val foodId: Uuid? = null,
    val inventoryItemId: Uuid? = null,
    val isPrimary: Boolean = false,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val deletedAt: Instant? = null
)
