package org.darthacheron.pantrypal.authentication

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.darthacheron.pantrypal.utils.HttpClientFactory
import dev.mokkery.mock
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.verifySuspend
import dev.mokkery.answering.returns
import dev.mokkery.matcher.any
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class AuthenticationServiceImplTest {

    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var clientFactory: HttpClientFactory
    private lateinit var service: AuthenticationServiceImpl

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
        service = AuthenticationServiceImpl(authRepository, clientFactory)
    }

    @Test
    fun testLoginRemotely_Success() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/login", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            respond(
                content = """
                    {
                        "tokenResponse": {
                            "accessToken": "access123",
                            "refreshToken": "refresh123",
                            "expiresIn": 3600,
                            "refreshExpiresIn": 7200
                        },
                        "userResponse": {
                            "id": "user-uuid",
                            "username": "testuser",
                            "email": "test@example.com"
                        }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)
        
        everySuspend { authRepository.loginRemotely(any(), any(), any(), any(), any(), any()) } returns Unit

        val result = service.loginRemotely("testuser", "password", "http://localhost")
        
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        verifySuspend { authRepository.loginRemotely("access123", "refresh123", 3600, 7200, "http://localhost", any()) }
    }

    @Test
    fun testLoginRemotely_Unauthorized() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"detail": "Invalid credentials"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)

        val result = service.loginRemotely("testuser", "wrong", "http://localhost")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidCredentialsException)
        assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
    }

    @Test
    fun testRegisterUser_Success() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/register", request.url.encodedPath)
            respond(
                content = """
                    {
                        "tokenResponse": {
                            "accessToken": "access123",
                            "refreshToken": "refresh123",
                            "expiresIn": 3600,
                            "refreshExpiresIn": 7200
                        },
                        "userResponse": {
                            "id": "new-user-uuid",
                            "username": "newuser",
                            "email": "new@example.com"
                        }
                    }
                """.trimIndent(),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)

        everySuspend { authRepository.loginRemotely(any(), any(), any(), any(), any(), any()) } returns Unit

        val result = service.registerUser("newuser", "new@example.com", "password", "http://localhost")
        
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        verifySuspend { authRepository.loginRemotely("access123", "refresh123", 3600, 7200, "http://localhost", any()) }
    }

    @Test
    fun testUpdateUser_Success() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/profile", request.url.encodedPath)
            assertEquals(HttpMethod.Patch, request.method)
            assertEquals("Bearer access123", request.headers[HttpHeaders.Authorization])
            respond(content = "true", status = HttpStatusCode.OK)
        }
        setupService(mockEngine)

        every { authRepository.authenticationPreferencesFlow } returns kotlinx.coroutines.flow.flowOf(
            AuthenticationPreferences("access123", "refresh123", 3600, 7200, "profile-id")
        )

        val result = service.updateUser("http://localhost", "newname", null, null, "current")
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrDefault(false))
    }
}
