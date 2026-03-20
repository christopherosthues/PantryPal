package org.darchacheron.pantrypal.profile

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
    val createdAt: Instant
)
