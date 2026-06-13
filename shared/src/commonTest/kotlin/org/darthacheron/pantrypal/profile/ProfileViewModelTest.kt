package org.darthacheron.pantrypal.profile

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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.authentication.AuthenticationService
import org.darthacheron.pantrypal.food.FoodRepository
import org.darthacheron.pantrypal.inventory.InventoryRepository
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.networking.ConnectionNetworkService
import org.darthacheron.pantrypal.settings.DataSynchronization
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.profile_error_update
import pantrypal.shared.generated.resources.profile_error_email_invalid
import pantrypal.shared.generated.resources.profile_error_username_empty
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: ProfileViewModel
    private lateinit var profileRepository: ProfileRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var authService: AuthenticationService
    private lateinit var connectionService: ConnectionNetworkService
    private lateinit var navigator: Navigator

    private val profileId = Uuid.generateV7()
    private val authPreferencesFlow = MutableStateFlow(AuthenticationPreferences("", "", 0, 0, localProfileId = profileId.toString()))

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = mock<ProfileRepository>()
        foodRepository = mock<FoodRepository>()
        inventoryRepository = mock<InventoryRepository>()
        authRepository = mock<AuthenticationPreferencesRepository> {
            every { authenticationPreferencesFlow } returns authPreferencesFlow
        }
        authService = mock<AuthenticationService>()
        connectionService = mock<ConnectionNetworkService>()
        navigator = mock<Navigator>()

        val profile = createTestProfile(profileId)
        every { profileRepository.getProfileById(profileId) } returns flowOf(profile)

        viewModel = ProfileViewModel(
            profileRepository,
            foodRepository,
            inventoryRepository,
            authRepository,
            authService,
            connectionService,
            navigator
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadProfile() = runTest {
        val state = viewModel.uiState.value
        assertNotNull(state.data)
        assertEquals("testuser", state.data?.username)
    }

    @Test
    fun testUpdateUsername() {
        viewModel.updateUsername("newname")
        assertEquals("newname", viewModel.uiState.value.data?.username)
        assertNull(viewModel.profileValidationState.value.usernameError)

        viewModel.updateUsername("")
        assertEquals(Res.string.profile_error_username_empty, viewModel.profileValidationState.value.usernameError)
    }

    @Test
    fun testUpdateEmail() {
        viewModel.updateEmail("valid@example.com")
        assertEquals("valid@example.com", viewModel.uiState.value.data?.email)
        assertNull(viewModel.profileValidationState.value.emailError)

        viewModel.updateEmail("invalid")
        assertEquals(Res.string.profile_error_email_invalid, viewModel.profileValidationState.value.emailError)
    }

    @Test
    fun testUpdateDataSynchronization() {
        viewModel.updateDataSynchronization(DataSynchronization.UPLOAD_AND_DOWNLOAD)
        assertEquals(DataSynchronization.UPLOAD_AND_DOWNLOAD, viewModel.uiState.value.data?.dataSynchronization)
    }

    @Test
    fun testSaveProfileSuccess() = runTest {
        everySuspend { profileRepository.upsert(any()) } returns Unit
        viewModel.updateUsername("updatedUser")
        viewModel.saveProfile()
        verifySuspend { profileRepository.upsert(any()) }
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun testSaveProfileFailure() = runTest {
        everySuspend { profileRepository.upsert(any()) } throws Exception("Database full")
        viewModel.saveProfile()
        assertNotNull(viewModel.uiState.value.error)
        assertEquals(Res.string.profile_error_update, viewModel.uiState.value.error)
    }

    @Test
    fun testUnlinkAccount() = runTest {
        everySuspend { profileRepository.upsert(any()) } returns Unit
        everySuspend { authService.logoutRemotely() } returns Result.success(true)

        viewModel.unlinkAccount()

        verifySuspend { profileRepository.upsert(any()) }
        verifySuspend { authService.logoutRemotely() }
    }

    @Test
    fun testLogout() = runTest {
        everySuspend { authService.logout() } returns Result.success(true)
        every { navigator.goToLogin() } returns Unit

        viewModel.logout()

        verifySuspend { authService.logout() }
        verify { navigator.goToLogin() }
    }

    @Test
    fun testDeleteLocalProfile() = runTest {
        everySuspend { profileRepository.deleteLocal() } returns Unit
        everySuspend { authService.logout() } returns Result.success(true)
        every { navigator.goToLogin() } returns Unit

        viewModel.deleteLocalProfile()

        verifySuspend { profileRepository.deleteLocal() }
        verifySuspend { authService.logout() }
        verify { navigator.goToLogin() }
    }

    @Test
    fun testDeleteRemoteProfile_NotLoggedIn_LoginFailure() = runTest {
        authPreferencesFlow.value = authPreferencesFlow.value.copy(isLoggedInRemotely = false, serverUrl = "http://srv")
        everySuspend { authService.loginRemotely(any(), any(), any()) } returns Result.failure(Exception("Wrong pass"))

        viewModel.showDeleteRemoteDialog()
        viewModel.onRemoteDeletePasswordChanged("wrong")
        viewModel.deleteRemoteProfile()

        verifySuspend { authService.loginRemotely(any(), any(), any()) }
        assertNotNull(viewModel.remoteDeleteError.value)
    }

    private fun createTestProfile(id: Uuid) = Profile(
        id = id,
        username = "testuser",
        email = "test@example.com",
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now()
    )
}
