package org.darchacheron.pantrypal.food

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.datetime.*
import org.darchacheron.pantrypal.database.PantryPalDatabase
import org.darchacheron.pantrypal.database.createInMemoryRoomDatabase
import org.darchacheron.pantrypal.navigation.NavRoute
import org.darchacheron.pantrypal.navigation.Navigator
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class FoodIntegrationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: PantryPalDatabase
    private lateinit var repository: FoodRepository
    private lateinit var navigator: Navigator

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        database = createInMemoryRoomDatabase()
        repository = FoodRepository(database.foodDao)
        navigator = Navigator()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testAddAndListFood() = runTest {
        val listViewModel = FoodListViewModel(repository, navigator)
        
        // Initially empty
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
            assertNotNull(item.data)
            assertTrue(item.data.isEmpty())
        }

        // Add a food via DetailViewModel
        val route = NavRoute.FoodDetail(null)
        val detailViewModel = FoodDetailViewModel(route, repository, navigator)
        
        // Wait for ready
        detailViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        detailViewModel.updateName("Integrated Apple")
        
        detailViewModel.isSaved.test {
            assertEquals(false, awaitItem())
            detailViewModel.save()
            assertTrue(awaitItem())
        }

        // Verify in List
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.isEmpty() == true) item = awaitItem()
            assertEquals(1, item.data?.size)
            assertEquals("Integrated Apple", item.data?.first()?.name)
        }
    }

    @Test
    fun testUpdateAndDeleteFood() = runTest {
        // 1. Setup initial data
        val foodId = Uuid.generateV7()
        val initialFood = createFoodEntity(foodId, "To Update")
        database.foodDao.upsert(initialFood)

        val listViewModel = FoodListViewModel(repository, navigator)
        
        // 2. Verify it's there
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.isEmpty() == true) item = awaitItem()
            assertEquals("To Update", item.data?.first()?.name)
        }

        // 3. Update via DetailViewModel
        val route = NavRoute.FoodDetail(foodId.toString())
        val detailViewModel = FoodDetailViewModel(route, repository, navigator)
        
        detailViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        detailViewModel.setIsEditing(true)
        detailViewModel.updateName("Updated Name")
        
        detailViewModel.isSaved.test {
            assertEquals(false, awaitItem())
            detailViewModel.save()
            assertTrue(awaitItem())
        }

        // 4. Verify update in list
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.any { it.name == "Updated Name" } != true) item = awaitItem()
            assertEquals("Updated Name", item.data.first { it.id == foodId }.name)
        }

        // 5. Delete via DetailViewModel
        // We wait for uiState to become success(null) after deletion
        detailViewModel.uiState.test {
            // Skip current state
            awaitItem() 
            
            detailViewModel.delete()
            
            var item = awaitItem()
            while (item.isLoading || item.data != null) {
                item = awaitItem()
            }
            assertNull(item.data)
        }
        
        // 6. Verify removal in list
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.any { it.id == foodId } == true) item = awaitItem()
            assertNotNull(item.data)
            assertTrue(item.data.none { it.id == foodId })
        }
    }

    @Test
    fun testFiltering() = runTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        
        val openedFood = createFoodEntity(Uuid.generateV7(), "Opened Apple").copy(openedAt = today)
        val unopenedFood = createFoodEntity(Uuid.generateV7(), "Unopened Banana")
        val overdueFood = createFoodEntity(Uuid.generateV7(), "Overdue Milk").copy(bestBeforeUsedByDate = yesterday)
        
        database.foodDao.upsert(openedFood)
        database.foodDao.upsert(unopenedFood)
        database.foodDao.upsert(overdueFood)
        
        val listViewModel = FoodListViewModel(repository, navigator)
        
        // Filter: All
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.size != 3) item = awaitItem()
            assertEquals(3, item.data.size)
        }
        
        // Filter: Opened
        listViewModel.setFilter(FoodFilter.Opened)
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.size != 1) item = awaitItem()
            assertEquals("Opened Apple", item.data.first().name)
        }
        
        // Filter: Unopened
        listViewModel.setFilter(FoodFilter.Unopened)
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.size != 2) item = awaitItem()
            assertTrue(item.data.any { it.name == "Unopened Banana" })
            assertTrue(item.data.any { it.name == "Overdue Milk" })
        }
        
        // Filter: Overdue
        listViewModel.setFilter(FoodFilter.Overdue)
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.size != 1) item = awaitItem()
            assertEquals("Overdue Milk", item.data.first().name)
        }
    }

    @Test
    fun testSorting() = runTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        
        val apple = createFoodEntity(Uuid.generateV7(), "Apple").copy(bestBeforeUsedByDate = tomorrow)
        val banana = createFoodEntity(Uuid.generateV7(), "Banana").copy(bestBeforeUsedByDate = today)
        
        database.foodDao.upsert(apple)
        database.foodDao.upsert(banana)
        
        val listViewModel = FoodListViewModel(repository, navigator)
        
        // Sort: Name Ascending (Default)
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.size != 2) item = awaitItem()
            assertEquals("Apple", item.data[0].name)
            assertEquals("Banana", item.data[1].name)
        }
        
        // Sort: Name Descending
        listViewModel.setSort(FoodSortOrder.Name, FoodSortDirection.Descending)
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.get(0)?.name != "Banana") item = awaitItem()
            assertEquals("Banana", item.data[0].name)
            assertEquals("Apple", item.data[1].name)
        }
        
        // Sort: Date Ascending
        listViewModel.setSort(FoodSortOrder.Date, FoodSortDirection.Ascending)
        listViewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading || item.data?.get(0)?.name != "Banana") item = awaitItem()
            assertEquals("Banana", item.data[0].name) // Banana is today, Apple is tomorrow
            assertEquals("Apple", item.data[1].name)
        }
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
}
