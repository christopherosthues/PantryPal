package org.darthacheron.pantrypal.inventory

import kotlinx.datetime.Instant
import org.darthacheron.pantrypal.core.inventory.InventoryItemDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class InventoryItemExtensionsTest {

    @Test
    fun testToDto() {
        val profileId = Uuid.random()
        val itemId = Uuid.random()
        val now = Clock.System.now()
        val item = InventoryItem(
            id = itemId,
            profileId = profileId,
            name = "Sugar",
            kiloCalories = 400,
            kiloJoule = 1674,
            fatInGrams = 0f,
            saturatedFattyAcidsInGrams = 0f,
            carbsInGrams = 100f,
            sugarInGrams = 100f,
            dietaryFiberInGrams = 0f,
            proteinInGrams = 0f,
            saltInGrams = 0f,
            fillingQuantity = 1000f,
            isLiquid = false,
            createdAt = now,
            lastModifiedAt = now
        )

        val dto = item.toDto("http://localhost")

        assertEquals(itemId, dto.clientId)
        assertEquals("Sugar", dto.name)
        assertEquals(400, dto.kiloCalories)
        assertEquals(now, dto.createdAt)
    }

    @Test
    fun testToInventoryItem() {
        val profileId = Uuid.random()
        val itemId = Uuid.random()
        val now = Clock.System.now()
        val dto = InventoryItemDto(
            serverId = Uuid.random(),
            profileId = profileId,
            clientId = itemId,
            name = "Milk",
            kiloCalories = 64,
            kiloJoule = 268,
            fatInGrams = 3.5f,
            saturatedFattyAcidsInGrams = 2.3f,
            carbsInGrams = 4.8f,
            sugarInGrams = 4.8f,
            dietaryFiberInGrams = 0f,
            proteinInGrams = 3.3f,
            saltInGrams = 0.1f,
            fillingQuantity = 1000f,
            isLiquid = true,
            createdAt = now,
            lastModifiedAt = now
        )

        val item = dto.toInventoryItem("http://localhost")

        assertEquals(itemId, item.id)
        assertEquals("Milk", item.name)
        assertEquals(profileId, item.profileId)
        assertEquals(64, item.kiloCalories)
        assertEquals(true, item.isLiquid)
        assertNotNull(item.remoteProducts.find { it.serverId == dto.serverId })
    }
}
