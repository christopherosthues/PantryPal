package org.darthacheron.pantrypal.profile

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.configuration.DeletionConfiguration
import org.darthacheron.pantrypal.core.configuration.DiagnosticsConfiguration
import org.darthacheron.pantrypal.core.configuration.FeatureToggles
import org.darthacheron.pantrypal.core.configuration.RateLimitConfiguration
import org.darthacheron.pantrypal.core.configuration.ServerDynamicConfiguration
import org.darthacheron.pantrypal.core.configuration.StorageLimits
import org.darthacheron.pantrypal.utils.HttpClientFactoryImpl
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminNetworkServiceImplTest {

    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var service: AdminNetworkServiceImpl

    @BeforeTest
    fun setup() {
        authRepository = mock<AuthenticationPreferencesRepository>()
    }

    private fun setupService(mockEngine: MockEngine) {
        val clientFactory = HttpClientFactoryImpl(mockEngine)
        service = AdminNetworkServiceImpl(authRepository, clientFactory)
    }

    @Test
    fun testFetchConfig_Success() = runTest {
        val serverUrl = "http://localhost"
        val mockEngine = MockEngine { request ->
            assertEquals("/admin/config", request.url.encodedPath)
            respond(
                content = """
                    {
                        "features": { "registrationEnabled": true },
                        "deletion": { "gracePeriodDays": 7 },
                        "storage": { "maxImageUploadSizeMB": 5, "supportedImageTypes": ["jpg"] },
                        "rateLimiting": { "rateLimitCapacity": 100 },
                        "diagnostics": { "telemetrySamplingRate": 0.1 }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val result = service.fetchConfig(serverUrl)
        
        assertTrue(result.isSuccess)
        assertEquals(7, result.getOrNull()?.deletion?.gracePeriodDays)
    }

    @Test
    fun testUpdateConfig_Failure() = runTest {
        val serverUrl = "http://localhost"
        val config = ServerDynamicConfiguration(
            features = FeatureToggles(registrationEnabled = false),
            deletion = DeletionConfiguration(gracePeriodDays = 14),
            storage = StorageLimits(maxImageUploadSizeMB = 10, supportedImageTypes = listOf("png")),
            rateLimiting = RateLimitConfiguration(rateLimitCapacity = 200),
            diagnostics = DiagnosticsConfiguration(telemetrySamplingRate = 0.5)
        )
        
        val mockEngine = MockEngine { _ ->
            respond(content = "Forbidden", status = HttpStatusCode.Forbidden)
        }
        setupService(mockEngine)
        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("token123", "refresh", 3600, 7200)
        )

        val result = service.updateConfig(serverUrl, config)
        
        assertTrue(result.isFailure)
        assertEquals(result.exceptionOrNull()?.message?.contains("Forbidden"), true)
    }
}
