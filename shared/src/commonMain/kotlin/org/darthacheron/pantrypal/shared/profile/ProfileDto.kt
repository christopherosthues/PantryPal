package org.darthacheron.pantrypal.shared.profile

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
) {
    companion object {
        val NotFound = ProfileDto(null, Uuid.random(), "", "", Instant.DISTANT_PAST, Instant.DISTANT_PAST)
    }
}
