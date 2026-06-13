package org.darthacheron.pantrypal.profile

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.darthacheron.pantrypal.authentication.AuthenticationPreferences
import org.darthacheron.pantrypal.authentication.AuthenticationPreferencesRepository
import org.darthacheron.pantrypal.core.configuration.*
import org.darthacheron.pantrypal.utils.HttpClientFactory
import dev.mokkery.mock
import dev.mokkery.every
import dev.mokkery.answering.returns
import kotlin.test.*

class AdminNetworkServiceImplTest {

    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var clientFactory: HttpClientFactory
    private lateinit var service: AdminNetworkServiceImpl

    @BeforeTest
    fun setup() {
        authRepository = mock<AuthenticationPreferencesRepository>()
        clientFactory = mock<HttpClientFactory>()
    }

    private fun setupService(mockEngine: MockEngine) {
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        every { clientFactory.create() } returns client
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
