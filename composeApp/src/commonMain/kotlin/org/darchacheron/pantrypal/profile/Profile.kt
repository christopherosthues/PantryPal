package org.darchacheron.pantrypal.profile

import org.darthacheron.pantrypal.shared.profile.ProfileDto
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Profile(
    val id: Uuid = Uuid.generateV7(),
    val serverId: Uuid?,
    val username: String,
    val email: String,
    val passwordHash: String? = null,
    val serverUrl: String? = null,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val lastSyncedAt: Instant? = null,
    val isLocalOnly: Boolean = false
) {
    fun toDto(): ProfileDto = ProfileDto(
        serverId = serverId,
        clientId = id,
        username = username,
        email = email,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        lastSyncedAt = lastSyncedAt
    )
}
