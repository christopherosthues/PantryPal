package org.darthacheron.pantrypal.camera

import org.darthacheron.pantrypal.core.camera.ImageDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ImageExtensionsTest {

    @Test
    fun testToDto() {
        val profileId = Uuid.random()
        val imageId = Uuid.random()
        val now = Clock.System.now()
        val image = Image(
            id = imageId,
            profileId = profileId,
            localPath = "/path/to/image.jpg",
            createdAt = now,
            lastModifiedAt = now
        )

        val dto = image.toDto("http://localhost")

        assertEquals(profileId, dto.profileId)
        assertEquals(now, dto.createdAt)
    }

    @Test
    fun testToImage() {
        val profileId = Uuid.random()
        val serverId = Uuid.random()
        val now = Clock.System.now()
        val dto = ImageDto(
            serverId = serverId,
            profileId = profileId,
            foodId = null,
            inventoryItemId = null,
            isPrimary = true,
            createdAt = now,
            lastModifiedAt = now
        )

        val image = dto.toImage("http://localhost")

        assertEquals(profileId, image.profileId)
        assertEquals(now, image.createdAt)
        assertEquals(1, image.remoteImages.size)
        assertEquals(serverId, image.remoteImages[0].serverId)
    }
}
