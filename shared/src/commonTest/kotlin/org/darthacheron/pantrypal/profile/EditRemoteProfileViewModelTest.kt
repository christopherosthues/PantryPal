package org.darthacheron.pantrypal.profile

import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.darthacheron.pantrypal.authentication.AuthenticationService
import org.darthacheron.pantrypal.authentication.InvalidCredentialsException
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.profile_error_wrong_password
import kotlin.test.*
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class EditRemoteProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: EditRemoteProfileViewModel
    private lateinit var authService: AuthenticationService
    private lateinit var profileRepository: ProfileRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authService = mock<AuthenticationService>()
        profileRepository = mock<ProfileRepository>()
        viewModel = EditRemoteProfileViewModel(authService, profileRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUpdateRemoteProfile_Success() = runTest {
        val serverUrl = "http://localhost"
        val profileId = Uuid.random()
        val remoteProfile = RemoteProfile(profileId, serverUrl, Uuid.random(), "old", "old@ex.com", Clock.System.now())
        val profile = Profile(
            id = profileId,
            username = "old",
            email = "old@ex.com",
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now(),
            remoteProfiles = listOf(remoteProfile)
        )
        
        everySuspend { authService.updateUser(any(), any(), any(), any(), any()) } returns Result.success(true)
        everySuspend { profileRepository.upsertRemoteProfile(any()) } returns Unit

        viewModel.onUsernameChanged("new")
        viewModel.onEmailChanged("new@ex.com")
        
        var dismissed = false
        viewModel.updateRemoteProfile(serverUrl, profile) { dismissed = true }

        assertTrue(dismissed)
        verifySuspend { authService.updateUser(serverUrl, "new", "new@ex.com", null, null) }
        verifySuspend { profileRepository.upsertRemoteProfile(any()) }
    }

    @Test
    fun testUpdateRemoteProfile_WrongPassword() = runTest {
        val serverUrl = "http://localhost"
        val profile = Profile(
            id = Uuid.random(),
            username = "u",
            email = "e",
            createdAt = Clock.System.now(),
            lastModifiedAt = Clock.System.now()
        )
        
        everySuspend { authService.updateUser(any(), any(), any(), any(), any()) } returns Result.failure(InvalidCredentialsException("Bad pass"))

        viewModel.updateRemoteProfile(serverUrl, profile) {}

        assertTrue(viewModel.uiState.value.hasError)
        assertEquals(Res.string.profile_error_wrong_password, viewModel.uiState.value.error)
    }
}
