package org.darchacheron.pantrypal.profile

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.darthacheron.pantrypal.core.profile.ProfileDto
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "remote_profile",
    primaryKeys = ["localProfileId", "serverUrl"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["localProfileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["localProfileId"])
    ]
)
data class RemoteProfileEntity(
    val localProfileId: Uuid,
    val serverUrl: String,
    val serverId: Uuid,
    val username: String,
    val email: String,
    val lastSyncedAt: Instant? = null
) {
    fun toRemoteProfile(): RemoteProfile = RemoteProfile(
        localProfileId = localProfileId,
        serverUrl = serverUrl,
        serverId = serverId,
        username = username,
        email = email,
        lastSyncedAt = lastSyncedAt
    )
}

@OptIn(ExperimentalUuidApi::class)
data class RemoteProfile(
    val localProfileId: Uuid,
    val serverUrl: String,
    val serverId: Uuid,
    val username: String,
    val email: String,
    val lastSyncedAt: Instant? = null
)

@OptIn(ExperimentalUuidApi::class)
fun RemoteProfile.toRemoteProfileEntity(): RemoteProfileEntity = RemoteProfileEntity(
    localProfileId = localProfileId,
    serverUrl = serverUrl,
    serverId = serverId,
    username = username,
    email = email,
    lastSyncedAt = lastSyncedAt
)

@OptIn(ExperimentalUuidApi::class)
fun ProfileDto.toRemoteProfileEntity(localProfileId: Uuid, serverUrl: String): RemoteProfileEntity = RemoteProfileEntity(
    localProfileId = localProfileId,
    serverUrl = serverUrl,
    serverId = serverId ?: throw IllegalArgumentException("serverId must not be null"),
    username = username,
    email = email,
    lastSyncedAt = lastSyncedAt
)
