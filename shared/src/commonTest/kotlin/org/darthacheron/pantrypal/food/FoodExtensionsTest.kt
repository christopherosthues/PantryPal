package org.darthacheron.pantrypal.food

import kotlinx.datetime.LocalDate
import org.darthacheron.pantrypal.core.food.FoodDto
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FoodExtensionsTest {

    @Test
    fun testToDto() {
        val profileId = Uuid.random()
        val foodId = Uuid.random()
        val now = Clock.System.now()
        val food = Food(
            id = foodId,
            profileId = profileId,
            name = "Apple",
            kiloCalories = 52,
            kiloJoule = 218,
            fatInGrams = 0.2f,
            saturatedFattyAcidsInGrams = 0f,
            carbsInGrams = 14f,
            sugarInGrams = 10f,
            dietaryFiberInGrams = 2.4f,
            proteinInGrams = 0.3f,
            saltInGrams = 0f,
            fillingQuantity = 150f,
            isLiquid = false,
            createdAt = now,
            lastModifiedAt = now,
            bestBeforeUsedByDate = LocalDate(2024, 12, 31),
            isUseBy = false,
            openedAt = null
        )

        val dto = food.toDto("http://localhost")

        assertEquals(foodId, dto.clientId)
        assertEquals("Apple", dto.name)
        assertEquals(52, dto.kiloCalories)
        assertEquals(LocalDate(2024, 12, 31), dto.bestBeforeUsedByDate)
        assertEquals(now, dto.createdAt)
    }

    @Test
    fun testToFood() {
        val profileId = Uuid.random()
        val foodId = Uuid.random()
        val now = Clock.System.now()
        val dto = FoodDto(
            serverId = Uuid.random(),
            profileId = profileId,
            clientId = foodId,
            name = "Banana",
            kiloCalories = 89,
            kiloJoule = 372,
            fatInGrams = 0.3f,
            saturatedFattyAcidsInGrams = 0.1f,
            carbsInGrams = 23f,
            sugarInGrams = 12f,
            dietaryFiberInGrams = 2.6f,
            proteinInGrams = 1.1f,
            saltInGrams = 0f,
            fillingQuantity = 120f,
            isLiquid = false,
            bestBeforeUsedByDate = null,
            isUseBy = false,
            openedAt = null,
            createdAt = now,
            lastModifiedAt = now
        )

        val food = dto.toFood("http://localhost")

        assertEquals(foodId, food.id)
        assertEquals("Banana", food.name)
        assertEquals(profileId, food.profileId)
        assertEquals(89, food.kiloCalories)
        assertEquals(now, food.createdAt)
        assertEquals(1, food.remoteProducts.size)
        assertEquals(dto.serverId, food.remoteProducts[0].serverId)
    }

    @Test
    fun testFoodLogic() {
        val food = createTestFood().copy(openedAt = LocalDate(2024, 1, 1))
        assertTrue(food.isOpened)
        
        val unopened = createTestFood().copy(openedAt = null)
        assertFalse(unopened.isOpened)
    }

    private fun createTestFood() = Food(
        id = Uuid.random(),
        profileId = Uuid.random(),
        name = "Test",
        kiloCalories = null,
        kiloJoule = null,
        fatInGrams = null,
        saturatedFattyAcidsInGrams = null,
        carbsInGrams = null,
        sugarInGrams = null,
        dietaryFiberInGrams = null,
        proteinInGrams = null,
        saltInGrams = null,
        fillingQuantity = null,
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now(),
        bestBeforeUsedByDate = null,
        isUseBy = false,
        openedAt = null
    )
}
