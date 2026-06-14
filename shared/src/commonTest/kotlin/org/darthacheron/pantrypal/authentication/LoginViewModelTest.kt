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
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.profile.Profile
import org.darthacheron.pantrypal.profile.ProfileRepository
import org.darthacheron.pantrypal.settings.DataSynchronization
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.login_error
import pantrypal.shared.generated.resources.login_error_empty_password
import pantrypal.shared.generated.resources.login_error_empty_username_or_email
import pantrypal.shared.generated.resources.login_error_wrong_username_or_password
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: LoginViewModel
    private lateinit var profileRepository: ProfileRepository
    private lateinit var authenticationService: AuthenticationService
    private lateinit var navigator: Navigator

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = mock<ProfileRepository>()
        authenticationService = mock<AuthenticationService>()
        navigator = mock<Navigator>()
        viewModel = LoginViewModel(profileRepository, authenticationService, navigator)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.loginState.value
        assertNotNull(state.data)
        assertEquals("", state.data?.username)
        assertEquals("", state.data?.password)
        assertFalse(state.data?.canLogin ?: true)
    }

    @Test
    fun testOnUsernameChanged() {
        viewModel.onUsernameChanged("testuser")
        assertEquals("testuser", viewModel.loginState.value.data?.username)
        assertNull(viewModel.loginState.value.data?.usernameError)

        viewModel.onUsernameChanged("")
        assertEquals("", viewModel.loginState.value.data?.username)
        assertEquals(Res.string.login_error_empty_username_or_email, viewModel.loginState.value.data?.usernameError)
    }

    @Test
    fun testOnPasswordChanged() {
        viewModel.onPasswordChanged("password123")
        assertEquals("password123", viewModel.loginState.value.data?.password)
        assertNull(viewModel.loginState.value.data?.passwordError)

        viewModel.onPasswordChanged("")
        assertEquals("", viewModel.loginState.value.data?.password)
        assertEquals(Res.string.login_error_empty_password, viewModel.loginState.value.data?.passwordError)
    }

    @Test
    fun testCanLogin() {
        assertFalse(viewModel.loginState.value.data?.canLogin ?: true)
        
        viewModel.onUsernameChanged("user")
        assertFalse(viewModel.loginState.value.data?.canLogin ?: true)
        
        viewModel.onPasswordChanged("pass")
        assertTrue(viewModel.loginState.value.data?.canLogin ?: false)
    }

    @Test
    fun testLoginSuccess() = runTest {
        val username = "user"
        val password = "password"
        val profile = createTestProfile(username, password)
        
        every { profileRepository.getProfileByIdentifier(username) } returns flowOf(profile)
        everySuspend { authenticationService.loginLocally(any(), any(), any()) } returns Result.success(Unit)
        every { navigator.goToMain() } returns Unit

        viewModel.onUsernameChanged(username)
        viewModel.onPasswordChanged(password)
        viewModel.onStayLoggedInChanged(true)
        viewModel.login()

        verifySuspend { authenticationService.loginLocally(profile.id, true, null) }
        verify { navigator.goToMain() }
        assertNull(viewModel.loginState.value.error)
    }

    @Test
    fun testLoginWrongPassword() = runTest {
        val username = "user"
        val password = "wrongpassword"
        val profile = createTestProfile(username, "correctpassword")
        
        every { profileRepository.getProfileByIdentifier(username) } returns flowOf(profile)

        viewModel.onUsernameChanged(username)
        viewModel.onPasswordChanged(password)
        viewModel.login()

        assertEquals(Res.string.login_error_wrong_username_or_password, viewModel.loginState.value.error)
    }

    @Test
    fun testLoginProfileNotFound() = runTest {
        val username = "nonexistent"
        every { profileRepository.getProfileByIdentifier(username) } returns flowOf(null)

        viewModel.onUsernameChanged(username)
        viewModel.onPasswordChanged("any")
        viewModel.login()

        assertEquals(Res.string.login_error_wrong_username_or_password, viewModel.loginState.value.error)
    }

    @Test
    fun testLoginGenericError() = runTest {
        val username = "user"
        every { profileRepository.getProfileByIdentifier(username) } returns kotlinx.coroutines.flow.flow {
            throw Exception("Database error")
        }

        viewModel.onUsernameChanged(username)
        viewModel.onPasswordChanged("any")
        viewModel.login()

        assertEquals(Res.string.login_error, viewModel.loginState.value.error)
    }

    private fun createTestProfile(username: String, password: String) = Profile(
        id = Uuid.random(),
        username = username,
        email = "$username@example.com",
        passwordHash = hashPassword(password),
        createdAt = Clock.System.now(),
        lastModifiedAt = Clock.System.now(),
        dataSynchronization = DataSynchronization.NO_SYNCHRONIZATION
    )
}
