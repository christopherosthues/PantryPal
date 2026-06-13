package org.darthacheron.pantrypal.inventory

import app.cash.turbine.test
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import dev.mokkery.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.navigation.InventoryNavRoute
import org.darthacheron.pantrypal.navigation.Navigator
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.inventory_detail_delete_error
import pantrypal.shared.generated.resources.inventory_detail_error_saving
import pantrypal.shared.generated.resources.inventory_detail_error_loading
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class InventoryDetailViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: InventoryRepository
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
        repository = mock<InventoryRepository>()
        navigator = mock<Navigator>()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadingExistingInventoryItem() = runTest {
        val itemId = Uuid.generateV7()
        val item = createInventoryItem(itemId, "Apple", profileId)

        everySuspend { repository.getById(itemId) } returns Result.success(item)

        val route = InventoryNavRoute.InventoryDetail(itemId.toString())
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.uiState.test {
            val itemState = awaitItem()
            assertEquals("Apple", itemState.data?.name)
            assertEquals(profileId, itemState.data?.profileId)
            assertFalse(viewModel.isAdding)
        }
    }

    @Test
    fun testLoadingExistingInventoryItemNotFound() = runTest {
        val itemId = Uuid.generateV7()
        everySuspend { repository.getById(itemId) } returns Result.success(null)

        val route = InventoryNavRoute.InventoryDetail(itemId.toString())
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.uiState.test {
            val itemState = awaitItem()
            assertTrue(itemState.hasError)
        }
    }

    @Test
    fun testLoadingException() = runTest {
        val itemId = Uuid.generateV7()
        everySuspend { repository.getById(itemId) } returns Result.failure(RuntimeException("Load error"))

        val route = InventoryNavRoute.InventoryDetail(itemId.toString())
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.uiState.test {
            val itemState = awaitItem()
            assertTrue(itemState.hasError)
            assertEquals(Res.string.inventory_detail_error_loading, itemState.error)
        }
    }

    @Test
    fun testNewInventoryItemInitialState() = runTest {
        val route = InventoryNavRoute.InventoryDetail(null)
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.uiState.test {
            val itemState = awaitItem()
            assertTrue(itemState.hasData)
            assertEquals("", itemState.data?.name)
            assertEquals(profileId, itemState.data?.profileId)
            assertTrue(viewModel.isAdding)
        }
    }

    @Test
    fun testLoadingErrorWhenProfileMissing() = runTest {
        authPreferencesFlow.value = authPreferencesFlow.value.copy(localProfileId = "")
        val route = InventoryNavRoute.InventoryDetail(null)
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.uiState.test {
            val itemState = awaitItem()
            assertTrue(itemState.hasError)
        }
    }

    @Test
    fun testUpdateNameAndSaveNew() = runTest {
        val route = InventoryNavRoute.InventoryDetail(null)
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.updateName("Banana")
        assertTrue(viewModel.canSave)

        everySuspend { repository.upsert(any()) } returns Result.success(Unit)
        every { navigator.goToInventoryDetail(any()) } returns Unit

        viewModel.save()

        assertTrue(viewModel.isSaved.value)
        verifySuspend { repository.upsert(any()) }
        verify { navigator.goToInventoryDetail(any()) }
    }

    @Test
    fun testSaveExistingInventoryItem() = runTest {
        val itemId = Uuid.generateV7()
        val item = createInventoryItem(itemId, "Apple", profileId)
        everySuspend { repository.getById(itemId) } returns Result.success(item)
        everySuspend { repository.upsert(any()) } returns Result.success(Unit)

        val route = InventoryNavRoute.InventoryDetail(itemId.toString())
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.updateName("Green Apple")
        viewModel.save()

        assertTrue(viewModel.isSaved.value)
        verifySuspend { repository.upsert(any()) }
        // Verify it refreshes from repository
        verifySuspend { repository.getById(itemId) }
    }

    @Test
    fun testSaveError() = runTest {
        val route = InventoryNavRoute.InventoryDetail(null)
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.updateName("Banana")
        everySuspend { repository.upsert(any()) } returns Result.failure(RuntimeException("Save error"))

        viewModel.save()

        viewModel.uiState.test {
            val itemState = awaitItem()
            assertTrue(itemState.hasError)
            assertEquals(Res.string.inventory_detail_error_saving, itemState.error)
        }
    }

    @Test
    fun testDeleteInventoryItem() = runTest {
        val itemId = Uuid.generateV7()
        everySuspend { repository.getById(itemId) } returns Result.success(createInventoryItem(itemId, "To Delete", profileId))
        everySuspend { repository.delete(itemId) } returns Result.success(Unit)

        val route = InventoryNavRoute.InventoryDetail(itemId.toString())
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.delete()

        verifySuspend { repository.delete(itemId) }
        assertTrue(viewModel.isSaved.value)
    }

    @Test
    fun testDeleteError() = runTest {
        val itemId = Uuid.generateV7()
        everySuspend { repository.getById(itemId) } returns Result.success(createInventoryItem(itemId, "To Delete", profileId))
        everySuspend { repository.delete(itemId) } returns Result.failure(RuntimeException("Delete error"))

        val route = InventoryNavRoute.InventoryDetail(itemId.toString())
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.delete()

        assertEquals(Res.string.inventory_detail_delete_error, viewModel.snackbarMessage.value)
        assertTrue(viewModel.uiState.value.hasError)
        assertEquals(Res.string.inventory_detail_delete_error, viewModel.uiState.value.error)
    }

    @Test
    fun testUpdateNutrients() = runTest {
        val route = InventoryNavRoute.InventoryDetail(null)
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

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

        val item = viewModel.uiState.value.data
        assertNotNull(item)
        assertEquals(100, item.kiloCalories)
        assertEquals(420, item.kiloJoule)
        assertEquals(10.5f, item.fatInGrams)
        assertEquals(2.0f, item.saturatedFattyAcidsInGrams)
        assertEquals(20f, item.carbsInGrams)
        assertEquals(5.5f, item.sugarInGrams)
        assertEquals(3.2f, item.dietaryFiberInGrams)
        assertEquals(8.0f, item.proteinInGrams)
        assertEquals(0.1f, item.saltInGrams)
        assertEquals(500f, item.fillingQuantity)
        assertTrue(item.isLiquid)
    }

    @Test
    fun testSyncItemFromStringsWithCommas() = runTest {
        val route = InventoryNavRoute.InventoryDetail(null)
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        viewModel.updateFatInGrams("10,5")
        viewModel.updateCarbsInGrams("20,2")

        val item = viewModel.uiState.value.data
        assertNotNull(item)
        assertEquals(10.5f, item.fatInGrams)
        assertEquals(20.2f, item.carbsInGrams)
    }

    @Test
    fun testSaveWhenCannotSave() = runTest {
        val route = InventoryNavRoute.InventoryDetail(null)
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        // Name is empty, so canSave should be false
        assertEquals("", viewModel.uiState.value.data?.name)
        assertFalse(viewModel.canSave)

        viewModel.save()

        // Upsert should not be called
        assertFalse(viewModel.isSaved.value)
    }

    @Test
    fun testCameraAndImages() = runTest {
        val route = InventoryNavRoute.InventoryDetail(null)
        val viewModel = InventoryDetailViewModel(route, repository, authRepository, navigator)

        var capturedCallback: ((String) -> Unit)? = null
        every { navigator.goToSimpleCamera(any()) } calls { (onSuccess: (String) -> Unit) ->
            capturedCallback = onSuccess
        }

        // Test primary image via camera
        viewModel.openCamera()
        capturedCallback?.invoke("path/to/main.jpg")
        assertEquals("path/to/main.jpg", viewModel.uiState.value.data?.image?.localPath)

        // Test remove primary image
        viewModel.removePrimaryImage()
        assertNull(viewModel.uiState.value.data?.image)

        // Test add additional image
        viewModel.addAdditionalImage()
        capturedCallback?.invoke("path/to/extra.jpg")
        assertEquals(1, viewModel.uiState.value.data?.additionalImages?.size)
        assertEquals("path/to/extra.jpg", viewModel.uiState.value.data?.additionalImages?.first()?.localPath)

        // Test remove additional image
        val extraImage = viewModel.uiState.value.data!!.additionalImages.first()
        viewModel.removeAdditionalImage(extraImage)
        assertTrue(viewModel.uiState.value.data!!.additionalImages.isEmpty())
    }

    private fun createInventoryItem(id: Uuid, name: String, profileId: Uuid) = InventoryItem(
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
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now(),
        image = null,
        additionalImages = emptyList()
    )
}
