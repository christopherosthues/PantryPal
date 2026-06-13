package org.darthacheron.pantrypal.authentication

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.darthacheron.pantrypal.core.auth.RegistrationResponse
import org.darthacheron.pantrypal.core.auth.TokenResponse
import org.darthacheron.pantrypal.core.auth.UserResponse
import org.darthacheron.pantrypal.utils.HttpClientFactoryImpl
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AuthenticationServiceImplTest {

    private lateinit var authRepository: AuthenticationPreferencesRepository
    private lateinit var service: AuthenticationServiceImpl

        @BeforeTest
    fun setup() {
        authRepository = mock<AuthenticationPreferencesRepository> {
            everySuspend { logoutRemotely() } returns Result.success(Unit)
            everySuspend { logout() } returns Result.success(Unit)
        }
    }

    private val testJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private fun setupService(mockEngine: MockEngine) {
        val clientFactory = HttpClientFactoryImpl(mockEngine)
        service = AuthenticationServiceImpl(authRepository, clientFactory)
    }

    @Test
    fun testLoginRemotely_Success() = runTest {
        val loginResponse = org.darthacheron.pantrypal.core.auth.LoginResponse(
            tokenResponse = TokenResponse("access123", "refresh123", 3600, 7200, "Bearer"),
            user = UserResponse(Uuid.random().toString(), "testuser", "test@example.com")
        )
        val responseJson = testJson.encodeToString(loginResponse)
        
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/login", request.url.encodedPath)
            assertEquals(HttpMethod.Post, request.method)
            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)
        
        everySuspend { authRepository.loginRemotely(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)

        val result = service.loginRemotely("testuser", "password", "http://localhost")
        
        assertTrue(result.isSuccess, "Expected success but got ${result.exceptionOrNull()}")
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
    fun testLoginRemotely_ProfileNotFound() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"detail": "Profile not found"}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)

        val result = service.loginRemotely("testuser", "password", "http://localhost")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ProfileNotFoundException)
        assertEquals("Profile not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun testLoginRemotely_ServerUnreachable() = runTest {
        val mockEngine = MockEngine { _ ->
            throw ConnectTimeoutException("Timeout")
        }
        setupService(mockEngine)

        val result = service.loginRemotely("testuser", "password", "http://localhost")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ServerUnreachableException)
    }

    @Test
    fun testRegisterUser_Success() = runTest {
        val registrationResponse = RegistrationResponse(
            tokenResponse = TokenResponse("access123", "refresh123", 3600, 7200, "Bearer"),
            user = UserResponse(Uuid.random().toString(), "newuser", "new@example.com")
        )
        val responseJson = testJson.encodeToString(registrationResponse)
        
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/register", request.url.encodedPath)
            respond(
                content = responseJson,
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)

        everySuspend { authRepository.loginRemotely(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)

        val result = service.registerUser("newuser", "new@example.com", "password", "http://localhost")
        
        assertTrue(result.isSuccess, "Expected success but got ${result.exceptionOrNull()}")
        assertNotNull(result.getOrNull())
        verifySuspend { authRepository.loginRemotely("access123", "refresh123", 3600, 7200, "http://localhost", any()) }
    }

    @Test
    fun testRegisterUser_UserAlreadyExists() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"detail": "User already exists"}""",
                status = HttpStatusCode.Conflict,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)

        val result = service.registerUser("newuser", "new@example.com", "password", "http://localhost")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserAlreadyExistsException)
        assertEquals("User already exists", result.exceptionOrNull()?.message)
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

        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("access123", "refresh123", 3600, 7200, "profile-id")
        )

        val result = service.updateUser("http://localhost", "newname", null, null, "current")
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrDefault(false))
    }

    @Test
    fun testUpdateUser_Conflict() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.Conflict)
        }
        setupService(mockEngine)

        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("access123", "refresh123", 3600, 7200, "profile-id")
        )

        val result = service.updateUser("http://localhost", "existingname", null, null, "current")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UserAlreadyExistsException)
    }

    @Test
    fun testRefreshToken_Success() = runTest {
        val tokenResponse = TokenResponse("newAccess", "newRefresh", 3600, 7200, "Bearer")
        val responseJson = testJson.encodeToString(tokenResponse)
        
        val mockEngine = MockEngine { request ->
            assertEquals("/api/v1/refresh", request.url.encodedPath)
            respond(
                content = responseJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)

        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("oldAccess", "oldRefresh", 3600, 7200, "profile-id")
        )
        everySuspend { authRepository.updateAccessToken(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        val result = service.refreshToken("http://localhost")

        assertTrue(result.isSuccess, "Expected success but got ${result.exceptionOrNull()}")
        assertTrue(result.getOrDefault(false))
        verifySuspend { authRepository.updateAccessToken("newAccess", "newRefresh", 3600, 7200, any()) }
    }

    @Test
    fun testRefreshToken_Unauthorized() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(content = "", status = HttpStatusCode.Unauthorized)
        }
        setupService(mockEngine)

        every { authRepository.authenticationPreferencesFlow } returns flowOf(
            AuthenticationPreferences("oldAccess", "oldRefresh", 3600, 7200, "profile-id")
        )
        everySuspend { authRepository.logoutRemotely() } returns Result.success(Unit)

        val result = service.refreshToken("http://localhost")

        assertTrue(result.isSuccess)
        assertFalse(result.getOrDefault(true))
        verifySuspend { authRepository.logoutRemotely() }
    }

    @Test
    fun testRegisterUser_ServerError() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"detail": "Internal server error"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        setupService(mockEngine)

        val result = service.registerUser("newuser", "new@example.com", "password", "http://localhost")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ServerErrorException)
        assertEquals("Internal server error", result.exceptionOrNull()?.message)
    }

    @Test
    fun testLogoutRemotely_Success() = runTest {
        val mockEngine = MockEngine { _ -> respond(content = "true", status = HttpStatusCode.OK) }
        setupService(mockEngine)
        everySuspend { authRepository.logoutRemotely() } returns Result.success(Unit)
        
        val result = service.logoutRemotely()
        
        assertTrue(result.isSuccess)
        verifySuspend { authRepository.logoutRemotely() }
    }

    @Test
    fun testLogout_Success() = runTest {
        val mockEngine = MockEngine { _ -> respond(content = "true", status = HttpStatusCode.OK) }
        setupService(mockEngine)
        everySuspend { authRepository.logout() } returns Result.success(Unit)
        
        val result = service.logout()
        
        assertTrue(result.isSuccess)
        verifySuspend { authRepository.logout() }
    }
}
