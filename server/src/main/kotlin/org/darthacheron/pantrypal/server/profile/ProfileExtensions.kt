package org.darthacheron.pantrypal.server.profile

import org.darthacheron.pantrypal.shared.profile.ProfileDto
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun ProfileDAO.toDto() : ProfileDto {
    return ProfileDto(
        this.id.value,
        this.clientId,
        this.username,
        this.email,
        this.createdAt,
        this.lastModifiedAt
    )
}