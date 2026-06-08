package org.darthacheron.pantrypal.food

import app.cash.turbine.test
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.navigation.FoodNavRoute
import org.darthacheron.pantrypal.navigation.Navigator
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class FoodDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FoodRepository
    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var navigator: Navigator
    private val profileId = Uuid.generateV7()
    private val authPreferencesFlow = MutableStateFlow(AuthenticationPreferences("", "", 0, 0, profileId.toString()))

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mock<AuthenticationPreferencesRepository> {
            every { authenticationPreferencesFlow } returns authPreferencesFlow
        }
        repository = mock<FoodRepository>()
        navigator = Navigator()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadingExistingFood() = runTest {
        val foodId = Uuid.generateV7()
        val food = createFood(foodId, "Apple", profileId)
        
        everySuspend { repository.getById(foodId) } returns food

        val route = FoodNavRoute.FoodDetail(foodId.toString())
        val viewModel = FoodDetailViewModel(route, repository, authRepository, navigator)

        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) {
                item = awaitItem()
            }
            assertEquals("Apple", item.data?.name)
            assertEquals(profileId, item.data?.profileId)
            assertFalse(viewModel.isAdding)
        }
    }

    @Test
    fun testNewFoodInitialState() = runTest {
        val route = FoodNavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, authRepository, navigator)

        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) {
                item = awaitItem()
            }
            assertTrue(item.hasData)
            assertEquals("", item.data?.name)
            assertEquals(profileId, item.data?.profileId)
            assertTrue(viewModel.isAdding)
        }
    }

    @Test
    fun testLoadingErrorWhenProfileMissing() = runTest {
        authPreferencesFlow.value = authPreferencesFlow.value.copy(localProfileId = "")
        val route = FoodNavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, authRepository, navigator)

        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) {
                item = awaitItem()
            }
            assertTrue(item.hasError)
        }
    }

    @Test
    fun testUpdateNameAndSave() = runTest {
        val route = FoodNavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, authRepository, navigator)
        
        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }
        
        viewModel.updateName("Banana")
        assertTrue(viewModel.canSave)
        
        everySuspend { repository.upsert(any()) } returns Unit
        everySuspend { repository.getById(any()) } returns createFood(Uuid.generateV7(), "Banana", profileId)
        
        viewModel.save()
        
        assertTrue(viewModel.isSaved.value)
        verifySuspend { repository.upsert(any()) }
    }

    @Test
    fun testDeleteFood() = runTest {
        val foodId = Uuid.generateV7()
        everySuspend { repository.getById(foodId) } returns createFood(foodId, "To Delete", profileId)
        everySuspend { repository.delete(foodId) } returns Unit

        val route = FoodNavRoute.FoodDetail(foodId.toString())
        val viewModel = FoodDetailViewModel(route, repository, authRepository, navigator)

        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        viewModel.delete()

        verifySuspend { repository.delete(foodId) }
        assertTrue(viewModel.isSaved.value)
    }

    @Test
    fun testUpdateNutrients() = runTest {
        val route = FoodNavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, authRepository, navigator)

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
        viewModel.updateFillingQuantity("500.0")
        viewModel.updateIsLiquid(true)

        val food = viewModel.uiState.value.data
        assertNotNull(food)
        assertEquals(100, food.kiloCalories)
        assertEquals(420, food.kiloJoule)
        assertEquals(10.5f, food.fatInGrams)
        assertEquals(2.0f, food.saturatedFattyAcidsInGrams)
        assertEquals(20f, food.carbsInGrams)
        assertEquals(5.5f, food.sugarInGrams)
        assertEquals(3.2f, food.dietaryFiberInGrams)
        assertEquals(8.0f, food.proteinInGrams)
        assertEquals(0.1f, food.saltInGrams)
        assertEquals(500f, food.fillingQuantity)
        assertTrue(food.isLiquid)
    }

    @Test
    fun testUpdateDates() = runTest {
        val route = FoodNavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, authRepository, navigator)

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

    @Test
    fun testOcrDateParsing() = runTest {
        val route = FoodNavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, authRepository, navigator)

        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        // Test ISO format
        viewModel.updateOcrDate("2025-12-31")
        assertEquals(LocalDate(2025, 12, 31), viewModel.uiState.value.data?.bestBeforeUsedByDate)

        // Test German format (dd.MM.yyyy)
        viewModel.updateOcrDate("24.12.2024")
        assertEquals(LocalDate(2024, 12, 24), viewModel.uiState.value.data?.bestBeforeUsedByDate)

        // Test invalid format
        viewModel.updateOcrDate("not a date")
        assertEquals(LocalDate(2024, 12, 24), viewModel.uiState.value.data?.bestBeforeUsedByDate) // Remains unchanged
    }

    @Test
    fun testCameraAndImages() = runTest {
        val route = FoodNavRoute.FoodDetail(null)
        val viewModel = FoodDetailViewModel(route, repository, authRepository, navigator)

        // Wait for it to be ready
        viewModel.uiState.test {
            var item = awaitItem()
            while (item.isLoading) item = awaitItem()
        }

        // Test primary image via camera
        viewModel.openCamera()
        navigator.onSimpleCameraResult("path/to/main.jpg")
        assertEquals("path/to/main.jpg", viewModel.uiState.value.data?.image?.localPath)

        // Test remove primary image
        viewModel.removePrimaryImage()
        assertNull(viewModel.uiState.value.data?.image)

        // Test add additional image
        viewModel.addAdditionalImage()
        navigator.onSimpleCameraResult("path/to/extra.jpg")
        assertEquals(1, viewModel.uiState.value.data?.additionalImages?.size)
        assertEquals("path/to/extra.jpg", viewModel.uiState.value.data?.additionalImages?.first()?.localPath)

        // Test remove additional image
        val extraImage = viewModel.uiState.value.data!!.additionalImages.first()
        viewModel.removeAdditionalImage(extraImage)
        assertTrue(viewModel.uiState.value.data!!.additionalImages.isEmpty())
    }

    private fun createFood(id: Uuid, name: String, profileId: Uuid) = Food(
        id = id,
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
