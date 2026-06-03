package org.darchacheron.pantrypal.camera

import androidx.room.Embedded
import androidx.room.Relation
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class ImageWithRemotes(
    @Embedded val image: ImageEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "localImageId"
    )
    val remoteImages: List<RemoteImageEntity>
) {
    fun toImage(): Image = image.toImage(remoteImages.map { it.toRemoteImage() })
}
