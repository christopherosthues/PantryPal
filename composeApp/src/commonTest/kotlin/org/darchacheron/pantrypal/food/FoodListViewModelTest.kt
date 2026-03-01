package org.darchacheron.pantrypal.food

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.darchacheron.pantrypal.navigation.Navigator
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class FoodListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: FoodListViewModel
    private lateinit var repository: FoodRepository
    private lateinit var navigator: Navigator
    private val foodDao = FakeFoodDao()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FoodRepository(foodDao)
        navigator = Navigator()
        viewModel = FoodListViewModel(repository, navigator)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsLoading() = runTest {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun testSearchQueryUpdate() = runTest {
        viewModel.setSearchQuery("Apple")
        assertEquals("Apple", viewModel.searchQuery.value)
    }

    @Test
    fun testSortUpdate() = runTest {
        viewModel.setSort(FoodSortOrder.Date, FoodSortDirection.Descending)
        assertEquals(FoodSortOrder.Date, viewModel.sortOrder.value)
        assertEquals(FoodSortDirection.Descending, viewModel.sortDirection.value)
    }

    @Test
    fun testFilterUpdate() = runTest {
        viewModel.setFilter(FoodFilter.Opened)
        assertEquals(FoodFilter.Opened, viewModel.filter.value)
    }

    @Test
    fun testDataLoadingSuccess() = runTest {
        val foodEntities = listOf(createFoodEntity("Apple"))
        foodDao.emit(foodEntities)

        viewModel.uiState.test {
            var item = awaitItem()
            // Wait for data to be loaded
            while (item.isLoading) {
                item = awaitItem()
            }

            assertTrue(item.hasData)
            assertEquals(1, item.data?.size)
            assertEquals("Apple", item.data?.first()?.name)
        }
    }

    @Test
    fun testDataLoadingError() = runTest {
        foodDao.setShouldThrow(true)

        // Trigger a change to ensure the flow is re-collected and encounters the error
        viewModel.setSearchQuery("Error Trigger")

        viewModel.uiState.test {
            var item = awaitItem()
            // Wait for error state
            while (!item.hasError) {
                item = awaitItem()
            }
            assertTrue(item.hasError)
        }
    }

    private fun createFoodEntity(name: String) = FoodEntity(
        id = Uuid.generateV7(),
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

@OptIn(ExperimentalUuidApi::class)
class FakeFoodDao : FoodDao {
    private val _flow = MutableStateFlow<List<FoodEntity>>(emptyList())
    private var shouldThrow = false

    fun emit(list: List<FoodEntity>) {
        _flow.value = list
    }

    fun setShouldThrow(value: Boolean) {
        shouldThrow = value
    }

    override fun getFilteredAndSorted(
        query: String,
        filter: String,
        sort: String,
        direction: String,
        currentDate: LocalDate
    ): Flow<List<FoodEntity>> = if (shouldThrow) {
        flow { throw RuntimeException("Test error") }
    } else {
        _flow
    }

    override suspend fun getById(id: Uuid): FoodEntity? = null
    override suspend fun upsert(food: FoodEntity) {}
    override suspend fun delete(id: Uuid) {}
}
