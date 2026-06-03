package org.darchacheron.pantrypal.food

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import org.darchacheron.pantrypal.common.RemoteProduct
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
        )
    ],
    indices = [
        Index(value = ["localFoodId"])
    ]
)
data class RemoteFoodEntity(
    val localFoodId: Uuid,
    val serverUrl: String,
    val serverId: Uuid,
    val lastSyncedAt: Instant? = null
) {
    fun toRemoteProduct(): RemoteProduct = RemoteProduct(
        localProductId = localFoodId,
        serverUrl = serverUrl,
        serverId = serverId,
        lastSyncedAt = lastSyncedAt
    )
}

@OptIn(ExperimentalUuidApi::class)
fun RemoteProduct.toRemoteFoodEntity(): RemoteFoodEntity = RemoteFoodEntity(
    localFoodId = localProductId,
    serverUrl = serverUrl,
    serverId = serverId,
    lastSyncedAt = lastSyncedAt
)
