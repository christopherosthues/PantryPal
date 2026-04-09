package org.darchacheron.pantrypal.profile

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "profile",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class ProfileEntity(
    @PrimaryKey val id: Uuid = Uuid.generateV7(),
    val serverId: Uuid?,
    val username: String,
    val email: String,
    val passwordHash: String? = null,
    val createdAt: Instant // TODO: lastModifiedAt
) {
    fun toProfile(): Profile = Profile(
        id = id,
        serverId = serverId,
        username = username,
        email = email,
        passwordHash = passwordHash,
        createdAt = createdAt
    )
}

@OptIn(ExperimentalUuidApi::class)
fun Profile.toProfileEntity(): ProfileEntity = ProfileEntity(
    id = id,
    serverId = serverId,
    username = username,
    email = email,
    passwordHash = passwordHash,
    createdAt = createdAt
)
