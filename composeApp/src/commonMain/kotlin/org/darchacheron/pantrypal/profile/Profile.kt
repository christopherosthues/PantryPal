package org.darchacheron.pantrypal.profile

import org.darchacheron.pantrypal.settings.DataSynchronization
import org.darthacheron.pantrypal.core.profile.ProfileDto
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Profile(
    val id: Uuid = Uuid.generateV7(),
    val username: String,
    val email: String,
    val passwordHash: String? = null,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val lastSyncedAt: Instant? = null,
    val isLocalOnly: Boolean = false,
    val dataSynchronization: DataSynchronization = DataSynchronization.NO_SYNCHRONIZATION,
    val remoteProfiles: List<RemoteProfile> = emptyList()
) {
    fun toDto(serverId: Uuid?, usernameOverride: String? = null, emailOverride: String? = null): ProfileDto = ProfileDto(
        serverId = serverId,
        clientId = id,
        username = usernameOverride ?: username,
        email = emailOverride ?: email,
        createdAt = createdAt,
        lastModifiedAt = lastModifiedAt,
        lastSyncedAt = lastSyncedAt
    )
}
