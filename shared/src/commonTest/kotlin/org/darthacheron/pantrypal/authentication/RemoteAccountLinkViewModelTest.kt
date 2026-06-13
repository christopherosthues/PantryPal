package org.darthacheron.pantrypal.authentication

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.darthacheron.pantrypal.networking.ConnectionNetworkService
import org.darthacheron.pantrypal.profile.Profile
import org.darthacheron.pantrypal.profile.ProfileRepository
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.remote_login_error_credentials
import pantrypal.shared.generated.resources.remote_login_error_user_exists
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class RemoteAccountLinkViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: RemoteAccountLinkViewModel
    private lateinit var authenticationService: AuthenticationService
    private lateinit var profileRepository: ProfileRepository
    private lateinit var preferencesRepository: AuthenticationPreferencesRepository
    private lateinit var connectionNetworkService: ConnectionNetworkService

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authenticationService = mock<AuthenticationService>()
        profileRepository = mock<ProfileRepository>()
        preferencesRepository = mock<AuthenticationPreferencesRepository>()
        connectionNetworkService = mock<ConnectionNetworkService>()
        
        viewModel = RemoteAccountLinkViewModel(
            authenticationService,
            profileRepository,
            preferencesRepository,
            connectionNetworkService
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.state.value.data
        assertNotNull(state)
        assertFalse(state.isCreatingNew)
        assertFalse(state.canSubmit)
    }

    @Test
    fun testSubmit_Login_Failure() = runTest {
        val localProfileId = Uuid.random()
        val profile = Profile(id = localProfileId, username = "local", email = "local@ex.com", createdAt = Clock.System.now(), lastModifiedAt = Clock.System.now())
        
        every { preferencesRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("", "", 0, 0, localProfileId = localProfileId.toString())
        )
        every { profileRepository.getProfileById(localProfileId) } returns flowOf(profile)
        everySuspend { authenticationService.loginRemotely(any(), any(), any()) } returns Result.failure(InvalidCredentialsException("Bad"))

        viewModel.onServerUrlChanged("http://localhost")
        viewModel.onUsernameChanged("user")
        viewModel.onPasswordChanged("pass")
        viewModel.setIsCreatingNew(false)
        
        viewModel.submit {}

        assertEquals(Res.string.remote_login_error_credentials, viewModel.state.value.error)
    }

    @Test
    fun testSubmit_Register_Failure() = runTest {
        val localProfileId = Uuid.random()
        val profile = Profile(id = localProfileId, username = "local", email = "local@ex.com", createdAt = Clock.System.now(), lastModifiedAt = Clock.System.now())
        
        every { preferencesRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("", "", 0, 0, localProfileId = localProfileId.toString())
        )
        every { profileRepository.getProfileById(localProfileId) } returns flowOf(profile)
        everySuspend { authenticationService.registerUser(any(), any(), any(), any()) } returns Result.failure(UserAlreadyExistsException("Exists"))

        viewModel.onServerUrlChanged("http://localhost")
        viewModel.onUsernameChanged("user")
        viewModel.onEmailChanged("user@ex.com")
        viewModel.onPasswordChanged("pass")
        viewModel.onRepeatedPasswordChanged("pass")
        viewModel.setIsCreatingNew(true)
        
        viewModel.submit {}

        assertEquals(Res.string.remote_login_error_user_exists, viewModel.state.value.error)
    }
}
