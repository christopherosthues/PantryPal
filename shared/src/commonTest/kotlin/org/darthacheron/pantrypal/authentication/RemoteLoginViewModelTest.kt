package org.darthacheron.pantrypal.authentication

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.darthacheron.pantrypal.networking.ConnectionNetworkService
import org.darthacheron.pantrypal.profile.Profile
import org.darthacheron.pantrypal.profile.ProfileRepository
import org.darthacheron.pantrypal.ui.UiState
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.remote_login_error_credentials
import pantrypal.shared.generated.resources.remote_login_error_unreachable
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class RemoteLoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: RemoteLoginViewModel
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
        
        viewModel = RemoteLoginViewModel(
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
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertEquals("", state.serverUrl)
        assertFalse(state.canSubmit)
    }

    @Test
    fun testOnUsernameChanged() {
        viewModel.onUsernameChanged("user")
        assertEquals("user", viewModel.state.value.data?.username)
        assertNull(viewModel.state.value.data?.usernameError)

        viewModel.onUsernameChanged("")
        assertNotNull(viewModel.state.value.data?.usernameError)
    }

    @Test
    fun testLogin_InvalidCredentials() = runTest {
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
        
        viewModel.login {}

        assertEquals(Res.string.remote_login_error_credentials, viewModel.state.value.error)
    }

    @Test
    fun testLogin_Success() = runTest {
        val localProfileId = Uuid.random()
        val serverUserId = Uuid.random()
        val profile = Profile(id = localProfileId, username = "local", email = "local@ex.com", createdAt = Clock.System.now(), lastModifiedAt = Clock.System.now())
        val loginResponse = org.darthacheron.pantrypal.core.auth.LoginResponse(
            tokenResponse = org.darthacheron.pantrypal.core.auth.TokenResponse("access", "refresh", 3600, 7200, "Bearer"),
            user = org.darthacheron.pantrypal.core.auth.UserResponse(serverUserId.toString(), "remote", "remote@ex.com")
        )

        every { preferencesRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("", "", 0, 0, localProfileId = localProfileId.toString())
        )
        every { profileRepository.getProfileById(localProfileId) } returns flowOf(profile)
        everySuspend { authenticationService.loginRemotely("remote", "pass", "http://localhost") } returns Result.success(loginResponse)
        everySuspend { profileRepository.upsert(any()) } returns Result.success(Unit)
        everySuspend { profileRepository.upsertRemoteProfile(any()) } returns Result.success(Unit)

        viewModel.onServerUrlChanged("http://localhost")
        viewModel.onUsernameChanged("remote")
        viewModel.onPasswordChanged("pass")
        
        var successCalled = false
        viewModel.login { successCalled = true }

        assertTrue(successCalled)
        verifySuspend { profileRepository.upsert(any()) }
        verifySuspend { profileRepository.upsertRemoteProfile(any()) }
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun testConnection() = runTest {
        everySuspend { connectionNetworkService.testConnection("http://localhost") } returns Result.success("OK")

        viewModel.onServerUrlChanged("http://localhost")
        viewModel.testConnection()

        assertFalse(viewModel.state.value.data?.isTestingConnection ?: true)
        assertEquals(true, viewModel.state.value.data?.connectionTestSuccess)
    }
}
