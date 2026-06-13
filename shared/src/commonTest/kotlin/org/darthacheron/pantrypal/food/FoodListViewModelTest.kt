package org.darthacheron.pantrypal.food

import app.cash.turbine.test
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.inventory.InventoryRepository
import org.darthacheron.pantrypal.navigation.Navigator
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.food_list_card_add_to_inventory_error
import pantrypal.shared.generated.resources.food_list_card_add_to_inventory_success
import pantrypal.shared.generated.resources.food_list_card_consume_error
import pantrypal.shared.generated.resources.food_list_card_consume_success
import pantrypal.shared.generated.resources.food_list_card_copy_error
import pantrypal.shared.generated.resources.food_list_card_copy_success
import pantrypal.shared.generated.resources.food_list_card_delete_error
import pantrypal.shared.generated.resources.food_list_card_delete_success
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class FoodListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: FoodListViewModel
    private lateinit var repository: FoodRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var navigator: Navigator
    private val profileId = Uuid.generateV7()
    private val authPreferencesFlow = MutableStateFlow(AuthenticationPreferences("", "", 0, 0, profileId.toString()))
    private val foodFlow = MutableStateFlow<List<Food>>(emptyList())

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mock<AuthenticationPreferencesRepository> {
            every { authenticationPreferencesFlow } returns authPreferencesFlow
        }
        repository = mock<FoodRepository> {
            every { getFilteredAndSorted(any(), any(), any(), any(), any()) } returns foodFlow
        }
        inventoryRepository = mock<InventoryRepository>()
        navigator = mock<Navigator>()
        viewModel = FoodListViewModel(repository, inventoryRepository, authRepository, navigator)
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
        viewModel.setSort(FoodSortOrder.Name, FoodSortDirection.Descending)
        assertEquals(FoodSortOrder.Name, viewModel.sortOrder.value)
        assertEquals(FoodSortDirection.Descending, viewModel.sortDirection.value)
    }

    @Test
    fun testFilterUpdate() = runTest {
        viewModel.setFilter(FoodFilter.Opened)
        assertEquals(FoodFilter.Opened, viewModel.filter.value)
    }

    @Test
    fun testDataLoadingSuccess() = runTest {
        val foods = listOf(createFood("Apple", profileId))
        foodFlow.value = foods

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
        every { repository.getFilteredAndSorted(any(), any(), any(), any(), any()) } returns flow { throw RuntimeException("Test error") }

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

    @Test
    fun testDeleteFood() = runTest {
        val food = createFood("Apple", profileId)
        everySuspend { repository.delete(food.id) } returns Unit

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.deleteFood(food)
            assertEquals(Res.string.food_list_card_delete_success, awaitItem()?.messageResource)
        }

        verifySuspend { repository.delete(food.id) }
    }

    @Test
    fun testCopyFood() = runTest {
        val food = createFood("Apple", profileId)
        everySuspend { repository.upsert(any()) } returns Unit

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.copyFood(food)
            assertEquals(Res.string.food_list_card_copy_success, awaitItem()?.messageResource)
        }

        verifySuspend { repository.upsert(any()) }
    }

    @Test
    fun testConsumeFood() = runTest {
        val food = createFood("Apple", profileId)
        everySuspend { repository.delete(food.id) } returns Unit

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.consumeFood(food)
            assertEquals(Res.string.food_list_card_consume_success, awaitItem()?.messageResource)
        }

        verifySuspend { repository.delete(food.id) }
    }

    @Test
    fun testAddToInventory() = runTest {
        val food = createFood("Apple", profileId)
        everySuspend { inventoryRepository.upsert(any()) } returns Unit
        every { navigator.goToInventoryDetail(any()) } returns Unit

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.addToInventory(food)
            assertEquals(Res.string.food_list_card_add_to_inventory_success, awaitItem()?.messageResource)
        }

        verifySuspend { inventoryRepository.upsert(any()) }
        verify { navigator.goToInventoryDetail(any()) }
    }

    @Test
    fun testGoToFoodDetail() {
        every { navigator.goToFoodDetail(any()) } returns Unit
        viewModel.goToFoodDetail("123")
        verify { navigator.goToFoodDetail("123") }
    }

    @Test
    fun testGoToFoodDetailWithNoFoodId() {
        every { navigator.goToFoodDetail(any()) } returns Unit
        viewModel.goToFoodDetail()
        verify { navigator.goToFoodDetail() }
    }

    @Test
    fun testGoToSettings() {
        every { navigator.goToSettings() } returns Unit
        viewModel.goToSettings()
        verify { navigator.goToSettings() }
    }

    @Test
    fun testClearMessage() = runTest {
        val food = createFood("Apple", profileId)
        everySuspend { repository.delete(food.id) } returns Unit

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.deleteFood(food)
            assertNotNull(awaitItem())

            viewModel.clearMessage()
            assertEquals(null, awaitItem())
        }
    }

    @Test
    fun testDeleteFoodError() = runTest {
        val food = createFood("Apple", profileId)
        everySuspend { repository.delete(food.id) } throws RuntimeException("Delete error")

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.deleteFood(food)
            assertEquals(Res.string.food_list_card_delete_error, awaitItem()?.messageResource)
        }
    }

    @Test
    fun testCopyFoodError() = runTest {
        val food = createFood("Apple", profileId)
        everySuspend { repository.upsert(any()) } throws RuntimeException("Copy error")

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.copyFood(food)
            assertEquals(Res.string.food_list_card_copy_error, awaitItem()?.messageResource)
        }
    }

    @Test
    fun testConsumeFoodError() = runTest {
        val food = createFood("Apple", profileId)
        everySuspend { repository.delete(food.id) } throws RuntimeException("Consume error")

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.consumeFood(food)
            assertEquals(Res.string.food_list_card_consume_error, awaitItem()?.messageResource)
        }
    }

    @Test
    fun testAddToInventoryError() = runTest {
        val food = createFood("Apple", profileId)
        everySuspend { inventoryRepository.upsert(any()) } throws RuntimeException("Inventory error")

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.addToInventory(food)
            assertEquals(Res.string.food_list_card_add_to_inventory_error, awaitItem()?.messageResource)
        }
        verifySuspend { inventoryRepository.upsert(any()) }
    }

    @Test
    fun testUiStateWithBlankProfileId() = runTest {
        authPreferencesFlow.value = authPreferencesFlow.value.copy(localProfileId = "")

        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) {
                item = awaitItem()
            }
            assertTrue(item.hasData)
            assertEquals(0, item.data?.size)
        }
    }

    private fun createFood(name: String, profileId: Uuid) = Food(
        id = Uuid.generateV7(),
        profileId = profileId,
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
        fillingQuantity = null,
        isLiquid = false,
        bestBeforeUsedByDate = null,
        isUseBy = false,
        openedAt = null,
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now(),
        image = null,
        additionalImages = emptyList(),
        remoteProducts = emptyList()
    )
}
