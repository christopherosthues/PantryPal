package org.darchacheron.pantrypal.food

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.darchacheron.pantrypal.navigation.NavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class FoodDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FoodRepository
    private lateinit var navigator: Navigator
    private val foodDao = FakeFoodDao()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FoodRepository(foodDao)
        navigator = Navigator()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadingExistingFood() = runTest {
        val foodId = Uuid.generateV7()
        val foodEntity = createFoodEntity(foodId, "Apple")
        foodDao.foods[foodId] = foodEntity

        val route = NavRoute.FoodDetail(foodId.toString())
        val viewModel = FoodDetailViewModel(route, repository, navigator)

        viewModel.uiState.test {
            var item = awaitItem()
            // Wait for data or error, skipping initial and loading states
            while (item.isLoading) {
                item = awaitItem()
            }
            assertEquals("Apple", item.data?.name)
            assertFalse(viewModel.isEditing)
            assertFalse(viewModel.isAdding)
        }
    }

    @Test
    fun testNewFoodInitialState() = runTest {
        val route = NavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, navigator)

        viewModel.uiState.test {
            var item = awaitItem()
            // Wait for initial data to be populated
            while (item.isLoading) {
                item = awaitItem()
            }
            assertTrue(item.hasData)
            assertEquals("", item.data?.name)
            assertTrue(viewModel.isEditing)
            assertTrue(viewModel.isAdding)
        }
    }

    @Test
    fun testUpdateNameAndSave() = runTest {
        val route = NavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, navigator)
        
        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }
        
        viewModel.updateName("Banana")
        assertTrue(viewModel.canSave)
        
        viewModel.save()
        
        assertTrue(viewModel.isSaved.value)
        val savedFood = foodDao.foods.values.first()
        assertEquals("Banana", savedFood.name)
    }

    @Test
    fun testDeleteFood() = runTest {
        val foodId = Uuid.generateV7()
        foodDao.foods[foodId] = createFoodEntity(foodId, "To Delete")

        val route = NavRoute.FoodDetail(foodId.toString())
        val viewModel = FoodDetailViewModel(route, repository, navigator)

        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        viewModel.delete()

        assertNull(foodDao.foods[foodId])
        assertTrue(viewModel.isSaved.value)
    }

    @Test
    fun testCancelEditingExistingFood() = runTest {
        val foodId = Uuid.generateV7()
        val originalName = "Original"
        foodDao.foods[foodId] = createFoodEntity(foodId, originalName)

        val route = NavRoute.FoodDetail(foodId.toString())
        val viewModel = FoodDetailViewModel(route, repository, navigator)

        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        viewModel.setIsEditing(true)
        viewModel.updateName("Changed")
        assertEquals("Changed", viewModel.uiState.value.data?.name)

        viewModel.cancelEditing()
        assertEquals(originalName, viewModel.uiState.value.data?.name)
        assertFalse(viewModel.isEditing)
    }

    @Test
    fun testUpdateNutrients() = runTest {
        val route = NavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, navigator)

        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        viewModel.updateKiloCalories("100")
        viewModel.updateKiloJoule("420")
        viewModel.updateFatInGrams("10.5")
        viewModel.updateSaturatedFattyAcidsInGrams("2.0")
        viewModel.updateCarbsInGrams("20")
        viewModel.updateSugarInGrams("5.5")
        viewModel.updateDietaryFiberInGrams("3.2")
        viewModel.updateProteinInGrams("8.0")
        viewModel.updateSaltInGrams("0.1")
        viewModel.updateAmount("500")
        viewModel.updateIsLiquid(true)

        val food = viewModel.uiState.value.data
        assertNotNull(food)
        assertEquals(100, food.kiloCalories)
        assertEquals(420, food.kiloJoule)
        assertEquals(10.5f, food.fatInGrams)
        assertEquals("2.0", food.saturatedFattyAcidsInGrams)
        assertEquals(20, food.carbsInGrams)
        assertEquals(5.5f, food.sugarInGrams)
        assertEquals(3.2f, food.dietaryFiberInGrams)
        assertEquals(8.0f, food.proteinInGrams)
        assertEquals(0.1f, food.saltInGrams)
        assertEquals(500f, food.amount)
        assertTrue(food.isLiquid)
    }

    @Test
    fun testUpdateNutrientsWithInvalidInput() = runTest {
        val route = NavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, navigator)

        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        viewModel.updateKiloCalories("abc")
        viewModel.updateFatInGrams("xyz")

        val food = viewModel.uiState.value.data
        assertNotNull(food)
        assertNull(food.kiloCalories)
        assertNull(food.fatInGrams)
    }

    @Test
    fun testUpdateDates() = runTest {
        val route = NavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, navigator)

        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        val bbd = LocalDate(2025, 12, 31)
        val opened = LocalDate(2025, 1, 1)

        viewModel.updateBestBeforeUsedByDate(bbd)
        viewModel.updateIsUseBy(true)
        viewModel.updateOpenedAt(opened)

        val food = viewModel.uiState.value.data
        assertNotNull(food)
        assertEquals(bbd, food.bestBeforeUsedByDate)
        assertTrue(food.isUseBy)
        assertEquals(opened, food.openedAt)
    }

    private fun createFoodEntity(id: Uuid, name: String) = FoodEntity(
        id = id,
        name = name,
        kiloCalories = null,
        kiloJoule = null,
        fatInGrams = null,
        saturatedFattyAcidsInGrams = null,
        carbsInGrams = null,
        sugarInGrams = null,
        dietaryFiberInGrams = null,
        proteinInGrams = null,
        saltInGrams = null,
        amount = null,
        isLiquid = false,
        bestBeforeUsedByDate = null,
        isUseBy = false,
        openedAt = null,
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now(),
        imagePath = null,
        additionalImagePaths = emptyList()
    )

    @OptIn(ExperimentalUuidApi::class)
    class FakeFoodDao : FoodDao {
        val foods = mutableMapOf<Uuid, FoodEntity>()
        
        override fun getFilteredAndSorted(
            query: String,
            filter: String,
            sort: String,
            direction: String,
            currentDate: LocalDate
        ): Flow<List<FoodEntity>> = flow { emit(foods.values.toList()) }

        override suspend fun getById(id: Uuid): FoodEntity? = foods[id]

        override suspend fun upsert(food: FoodEntity) {
            foods[food.id] = food
        }

        override suspend fun delete(id: Uuid) {
            foods.remove(id)
        }
    }
}
