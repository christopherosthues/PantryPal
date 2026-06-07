package org.darthacheron.pantrypal.core.profile

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class ProfileDto(
    val serverId: Uuid?,
    val clientId: Uuid,
    val username: String,
    val email: String,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val lastSyncedAt: Instant? = null,
    val deletedAt: Instant? = null
)
