package org.darchacheron.pantrypal.food

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.darchacheron.pantrypal.common.RemoteProduct
import org.darchacheron.pantrypal.profile.RemoteProfileEntity
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Entity(
    tableName = "remote_food",
    primaryKeys = ["localFoodId", "serverUrl"],
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["localFoodId"],
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
        Index(value = ["localFoodId"]),
        Index(value = ["localProfileId", "serverUrl"])
    ]
)
data class RemoteFoodEntity(
    val localFoodId: Uuid,
    val localProfileId: Uuid,
    val serverUrl: String,
    val serverId: Uuid,
    val lastSyncedAt: Instant? = null
) {
    fun toRemoteProduct(): RemoteProduct = RemoteProduct(
        localProductId = localFoodId,
        localProfileId = localProfileId,
        serverUrl = serverUrl,
        serverId = serverId,
        lastSyncedAt = lastSyncedAt
    )
}

@OptIn(ExperimentalUuidApi::class)
fun RemoteProduct.toRemoteFoodEntity(): RemoteFoodEntity = RemoteFoodEntity(
    localFoodId = localProductId,
    localProfileId = localProfileId,
    serverUrl = serverUrl,
    serverId = serverId,
    lastSyncedAt = lastSyncedAt
)
