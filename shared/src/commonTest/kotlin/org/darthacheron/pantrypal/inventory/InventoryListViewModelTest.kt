package org.darthacheron.pantrypal.inventory

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
import org.darthacheron.pantrypal.food.FoodRepository
import org.darthacheron.pantrypal.navigation.Navigator
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class InventoryListViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: InventoryListViewModel
    private lateinit var repository: InventoryRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var navigator: Navigator
    private val profileId = Uuid.generateV7()
    private val authPreferencesFlow = MutableStateFlow(AuthenticationPreferences("", "", 0, 0, profileId.toString()))
    private val inventoryFlow = MutableStateFlow<List<InventoryItem>>(emptyList())

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mock<AuthenticationPreferencesRepository> {
            every { authenticationPreferencesFlow } returns authPreferencesFlow
        }
        repository = mock<InventoryRepository> {
            every { getFilteredAndSorted(any(), any(), any(), any()) } returns inventoryFlow
        }
        foodRepository = mock<FoodRepository>()
        navigator = mock<Navigator>()
        viewModel = InventoryListViewModel(repository, foodRepository, authRepository, navigator)
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
        viewModel.setSort(InventorySortOrder.Name, InventorySortDirection.Descending)
        assertEquals(InventorySortOrder.Name, viewModel.sortOrder.value)
        assertEquals(InventorySortDirection.Descending, viewModel.sortDirection.value)
    }

    @Test
    fun testDataLoadingSuccess() = runTest {
        val items = listOf(createInventoryItem("Apple", profileId))
        inventoryFlow.value = items

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
        every { repository.getFilteredAndSorted(any(), any(), any(), any()) } returns flow { throw RuntimeException("Test error") }

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
    fun testDeleteItem() = runTest {
        val item = createInventoryItem("Apple", profileId)
        everySuspend { repository.delete(item.id) } returns Unit

        viewModel.deleteItem(item)

        verifySuspend { repository.delete(item.id) }
    }

    @Test
    fun testDeleteItemError() = runTest {
        val item = createInventoryItem("Apple", profileId)
        everySuspend { repository.delete(item.id) } throws RuntimeException("Delete error")

        viewModel.deleteItem(item)

        verifySuspend { repository.delete(item.id) }
    }

    @Test
    fun testAddToFoodList() = runTest {
        val item = createInventoryItem("Apple", profileId)
        everySuspend { foodRepository.upsert(any()) } returns Unit
        every { navigator.goToFoodDetail(any()) } returns Unit

        viewModel.addToFoodList(item)

        verifySuspend { foodRepository.upsert(any()) }
        verify { navigator.goToFoodDetail(any()) }
    }

    @Test
    fun testAddToFoodListError() = runTest {
        val item = createInventoryItem("Apple", profileId)
        everySuspend { foodRepository.upsert(any()) } throws RuntimeException("Food error")

        viewModel.addToFoodList(item)

        verifySuspend { foodRepository.upsert(any()) }
    }

    @Test
    fun testGoToItemDetails() {
        every { navigator.goToInventoryDetail(any()) } returns Unit
        viewModel.goToItemDetails("123")
        verify { navigator.goToInventoryDetail("123") }
    }

    @Test
    fun testGoToItemDetailsWithNoItemId() {
        every { navigator.goToInventoryDetail(any()) } returns Unit
        viewModel.goToItemDetails()
        verify { navigator.goToInventoryDetail(null) }
    }

    @Test
    fun testGoToSettings() {
        every { navigator.goToSettings() } returns Unit
        viewModel.goToSettings()
        verify { navigator.goToSettings() }
    }

    @Test
    fun testClearMessage() = runTest {
        val inventoryItem = createInventoryItem("Apple", profileId)
        everySuspend { repository.delete(inventoryItem.id) } returns Unit

        viewModel.messages.test {
            assertEquals(null, awaitItem())
            viewModel.deleteItem(inventoryItem)
            assertNotNull(awaitItem())

            viewModel.clearMessage()
            assertEquals(null, awaitItem())
        }
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

    @Test
    fun testSearchTriggersRefetch() = runTest {
        val items = listOf(createInventoryItem("Apple", profileId))
        inventoryFlow.value = items

        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) {
                item = awaitItem()
            }
            assertTrue(item.hasData)

            viewModel.setSearchQuery("Banana")
            item = awaitItem()
            // Should still have data or be loading
            assertTrue(item.hasData || item.isLoading)
        }
    }

    @Test
    fun testSortChangeTriggersRefetch() = runTest {
        val items = listOf(createInventoryItem("Apple", profileId))
        inventoryFlow.value = items

        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) {
                item = awaitItem()
            }
            assertTrue(item.hasData)

            viewModel.setSort(InventorySortOrder.Name, InventorySortDirection.Descending)
            item = awaitItem()
            assertTrue(item.hasData || item.isLoading)
        }
    }

    private fun createInventoryItem(name: String, profileId: Uuid) = InventoryItem(
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
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now(),
        image = null,
        additionalImages = emptyList()
    )
}
