package org.darchacheron.pantrypal.camera

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
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
        )
    ],
    indices = [
        Index(value = ["localImageId"])
    ]
)
data class RemoteImageEntity(
    val localImageId: Uuid,
    val serverUrl: String,
    val serverId: Uuid,
    val lastSyncedAt: Instant? = null
) {
    fun toRemoteImage(): RemoteImage = RemoteImage(
        localImageId = localImageId,
        serverUrl = serverUrl,
        serverId = serverId,
        lastSyncedAt = lastSyncedAt
    )
}

@OptIn(ExperimentalUuidApi::class)
fun RemoteImage.toRemoteImageEntity(): RemoteImageEntity = RemoteImageEntity(
    localImageId = localImageId,
    serverUrl = serverUrl,
    serverId = serverId,
    lastSyncedAt = lastSyncedAt
)
