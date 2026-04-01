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
    val passwordHash: String? = null,
    val createdAt: Instant,
    val lastModifiedAt: Instant?
)