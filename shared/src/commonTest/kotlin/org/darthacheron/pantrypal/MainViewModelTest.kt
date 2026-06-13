package org.darthacheron.pantrypal

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.authentication.AuthenticationService
import org.darthacheron.pantrypal.navigation.Navigator
import org.darthacheron.pantrypal.profile.ProfileRepository
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: MainViewModel
    private lateinit var profileRepository: ProfileRepository
    private lateinit var authService: AuthenticationService
    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var navigator: Navigator

    private val remoteAccountDeletedFlow = MutableSharedFlow<Boolean>()
    private val authPreferencesFlow = MutableStateFlow(AuthenticationPreferences("", "", 0, 0))

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = mock<ProfileRepository> {
            every { remoteAccountDeleted } returns remoteAccountDeletedFlow
        }
        authService = mock<AuthenticationService>()
        authRepository = mock<AuthenticationPreferencesRepository> {
            every { authenticationPreferencesFlow } returns authPreferencesFlow
        }
        navigator = mock<Navigator>()

        viewModel = MainViewModel(profileRepository, authService, authRepository, navigator)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRemoteAccountDeletedShowsDialog() = runTest {
        assertFalse(viewModel.showRemoteDeletedDialog.value)
        remoteAccountDeletedFlow.emit(true)
        assertTrue(viewModel.showRemoteDeletedDialog.value)
    }

    @Test
    fun testDeleteLocalAccount() = runTest {
        everySuspend { profileRepository.delete(false) } returns Result.success(Unit)
        everySuspend { authService.logout() } returns Result.success(true)
        every { navigator.goToLogin() } returns Unit

        viewModel.deleteLocalAccount()

        verifySuspend { profileRepository.delete(false) }
        verifySuspend { authService.logout() }
        assertFalse(viewModel.showRemoteDeletedDialog.value)
    }
}
