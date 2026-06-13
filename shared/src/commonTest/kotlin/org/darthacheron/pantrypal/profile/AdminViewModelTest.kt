package org.darthacheron.pantrypal.profile

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.configuration.*
import pantrypal.shared.generated.resources.Res
import pantrypal.shared.generated.resources.admin_config_load_error
import pantrypal.shared.generated.resources.admin_config_save_success
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: AdminViewModel
    private lateinit var adminService: AdminNetworkService
    private lateinit var authRepository: AuthenticationPreferencesRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        adminService = mock<AdminNetworkService>()
        authRepository = mock<AuthenticationPreferencesRepository> {
            every { authenticationPreferencesFlow } returns flowOf(
                AuthenticationPreferences("", "", 0, 0, serverUrl = "http://localhost")
            )
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadConfigSuccess() = runTest {
        val config = createTestConfig()
        everySuspend { adminService.fetchConfig("http://localhost") } returns Result.success(config)
        
        viewModel = AdminViewModel(adminService, authRepository)

        val state = viewModel.uiState.value
        assertTrue(state.hasData)
        assertEquals(config, state.data)
    }

    @Test
    fun testLoadConfigError() = runTest {
        everySuspend { adminService.fetchConfig("http://localhost") } returns Result.failure(Exception("Load error"))
        
        viewModel = AdminViewModel(adminService, authRepository)

        val state = viewModel.uiState.value
        assertTrue(state.hasError)
        assertEquals(Res.string.admin_config_load_error, state.error)
    }

    @Test
    fun testUpdateConfigSuccess() = runTest {
        val config = createTestConfig()
        everySuspend { adminService.fetchConfig("http://localhost") } returns Result.success(config)
        viewModel = AdminViewModel(adminService, authRepository)

        val updatedConfig = config.copy(features = config.features.copy(registrationEnabled = false))
        everySuspend { adminService.updateConfig("http://localhost", updatedConfig) } returns Result.success(updatedConfig)

        viewModel.updateConfig(updatedConfig)

        val state = viewModel.uiState.value
        assertEquals(updatedConfig, state.data)
        assertEquals(Res.string.admin_config_save_success, state.message)
    }

    private fun createTestConfig() = ServerDynamicConfiguration(
        features = FeatureToggles(registrationEnabled = true),
        deletion = DeletionConfiguration(gracePeriodDays = 7),
        storage = StorageLimits(maxImageUploadSizeMB = 5, supportedImageTypes = listOf("jpg")),
        rateLimiting = RateLimitConfiguration(rateLimitCapacity = 100),
        diagnostics = DiagnosticsConfiguration(telemetrySamplingRate = 0.1)
    )
}
