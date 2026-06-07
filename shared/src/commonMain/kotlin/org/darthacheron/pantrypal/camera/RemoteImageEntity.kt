package org.darthacheron.pantrypal.camera

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.darthacheron.pantrypal.profile.RemoteProfileEntity
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "remote_image",
    primaryKeys = ["localImageId", "serverUrl"],
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["localImageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RemoteProfileEntity::class,
            parentColumns = ["localProfileId", "serverUrl"],
            childColumns = ["localProfileId", "serverUrl"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["localImageId"]),
        Index(value = ["localProfileId", "serverUrl"])
    ]
)
data class RemoteImageEntity(
    val localImageId: Uuid,
    val localProfileId: Uuid,
    val serverUrl: String,
    val serverId: Uuid,
    val lastSyncedAt: Instant? = null
) {
    fun toRemoteImage(): RemoteImage = RemoteImage(
        localImageId = localImageId,
        localProfileId = localProfileId,
        serverUrl = serverUrl,
        serverId = serverId,
        lastSyncedAt = lastSyncedAt
    )
}

@OptIn(ExperimentalUuidApi::class)
fun RemoteImage.toRemoteImageEntity(): RemoteImageEntity = RemoteImageEntity(
    localImageId = localImageId,
    localProfileId = localProfileId,
    serverUrl = serverUrl,
    serverId = serverId,
    lastSyncedAt = lastSyncedAt
)
