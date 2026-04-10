package org.darthacheron.pantrypal.server.profile

import org.darthacheron.pantrypal.shared.profile.ProfileDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun ProfileDAO.toDto(clientId: Uuid = Uuid.random()) : ProfileDto {
    return ProfileDto(
        serverId = this.id.value,
        clientId = clientId,
        username = this.username,
        email = this.email,
        createdAt = this.createdAt,
        lastModifiedAt = this.lastModifiedAt,
        lastSyncedAt = this.lastSyncedAt
    )
}