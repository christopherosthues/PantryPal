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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.profile.Profile
import org.darthacheron.pantrypal.profile.ProfileRepository
import pantrypal.shared.generated.resources.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class RegistrationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: RegistrationViewModel
    private lateinit var profileRepository: ProfileRepository
    private lateinit var authenticationService: AuthenticationService
    private lateinit var navigator: Navigator

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = mock<ProfileRepository>()
        authenticationService = mock<AuthenticationService>()
        navigator = mock<Navigator>()
        viewModel = RegistrationViewModel(profileRepository, authenticationService, navigator)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.registrationState.value
        assertNotNull(state.data)
        assertEquals("", state.data.userName)
        assertFalse(state.data.canRegister)
    }

    @Test
    fun testOnUserNameChanged() = runTest {
        every { profileRepository.getProfileByUsername(any()) } returns flowOf(null)
        
        viewModel.onUserNameChanged("testuser")
        assertEquals("testuser", viewModel.registrationState.value.data?.userName)
        assertNull(viewModel.registrationState.value.data?.userNameError)

        viewModel.onUserNameChanged("")
        assertEquals("", viewModel.registrationState.value.data?.userName)
        assertEquals(Res.string.registration_error_empty_username, viewModel.registrationState.value.data?.userNameError)
    }

    @Test
    fun testOnEmailChanged() = runTest {
        every { profileRepository.getProfileByEmail(any()) } returns flowOf(null)

        viewModel.onEmailChanged("test@example.com")
        assertEquals("test@example.com", viewModel.registrationState.value.data?.email)
        assertNull(viewModel.registrationState.value.data?.emailError)

        viewModel.onEmailChanged("invalid-email")
        assertEquals(Res.string.registration_error_invalid_email, viewModel.registrationState.value.data?.emailError)
    }

    @Test
    fun testOnEmailChanged_Exists() = runTest {
        every { profileRepository.getProfileByEmail("existing@example.com") } returns flowOf(
            Profile(
                username = "other",
                email = "existing@example.com",
                createdAt = Clock.System.now(),
                lastModifiedAt = Clock.System.now()
            )
        )

        viewModel.onEmailChanged("existing@example.com")
        // Wait for validation job
        delay(400.milliseconds)
        assertEquals(Res.string.registration_error_email_exists, viewModel.registrationState.value.data?.emailError)
    }

    @Test
    fun testOnPasswordChanged() {
        viewModel.onPasswordChanged("pass123")
        assertEquals("pass123", viewModel.registrationState.value.data?.password)
        
        viewModel.onRepeatPasswordChanged("pass456")
        assertEquals(Res.string.registration_error_password_mismatch, viewModel.registrationState.value.data?.repeatedPasswordError)
        
        viewModel.onRepeatPasswordChanged("pass123")
        assertNull(viewModel.registrationState.value.data?.repeatedPasswordError)
    }

    @Test
    fun testCanRegister() = runTest {
        every { profileRepository.getProfileByUsername(any()) } returns flowOf(null)
        every { profileRepository.getProfileByEmail(any()) } returns flowOf(null)

        viewModel.onUserNameChanged("user")
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password")
        viewModel.onRepeatPasswordChanged("password")
        
        assertTrue(viewModel.registrationState.value.data?.canRegister ?: false)
    }

    @Test
    fun testRegisterSuccess() = runTest {
        val username = "user"
        val email = "user@example.com"
        val password = "password"

        every { profileRepository.getProfileByUsername(any()) } returns flowOf(null)
        every { profileRepository.getProfileByEmail(any()) } returns flowOf(null)
        every { profileRepository.getProfileByIdentifier(any()) } returns flowOf(null)
        everySuspend { profileRepository.upsert(any()) } returns Unit
        everySuspend { authenticationService.loginLocally(any(), any(), any()) } returns Unit
        every { navigator.goToMain() } returns Unit

        viewModel.onUserNameChanged(username)
        viewModel.onEmailChanged(email)
        viewModel.onPasswordChanged(password)
        viewModel.onRepeatPasswordChanged(password)
        
        viewModel.register()

        verifySuspend { profileRepository.upsert(any()) }
        verifySuspend { authenticationService.loginLocally(any(), false, null) }
        verify { navigator.goToMain() }
    }

    @Test
    fun testRegisterUserAlreadyExists() = runTest {
        val username = "existing"
        every { profileRepository.getProfileByUsername(any()) } returns flowOf(null)
        every { profileRepository.getProfileByEmail(any()) } returns flowOf(null)
        every { profileRepository.getProfileByIdentifier(username) } returns flowOf(
            Profile(
                username = username,
                email = "user@example.com",
                createdAt = Clock.System.now(),
                lastModifiedAt = Clock.System.now()
            )
        )

        viewModel.onUserNameChanged(username)
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("password")
        viewModel.onRepeatPasswordChanged("password")
        
        viewModel.register()

        assertEquals(Res.string.registration_error_username_exists, viewModel.registrationState.value.error)
    }
}
