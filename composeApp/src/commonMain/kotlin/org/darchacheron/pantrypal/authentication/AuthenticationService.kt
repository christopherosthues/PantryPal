package org.darchacheron.pantrypal.authentication

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import org.darthacheron.pantrypal.shared.auth.LoginDto
import org.darthacheron.pantrypal.shared.auth.LoginResponse
import org.darthacheron.pantrypal.shared.auth.ProblemDetails
import org.darthacheron.pantrypal.shared.auth.RefreshTokenDto
import org.darthacheron.pantrypal.shared.auth.RegistrationDto
import org.darthacheron.pantrypal.shared.auth.RegistrationResponse
import org.darthacheron.pantrypal.shared.auth.TokenResponse
import org.darthacheron.pantrypal.shared.auth.UpdateUserDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InvalidCredentialsException(message: String) : Exception(message)
class ProfileNotFoundException(message: String) : Exception(message)
class ServerUnreachableException(message: String, cause: Throwable? = null) : Exception(message, cause)
class UserAlreadyExistsException(message: String) : Exception(message)
class ServerErrorException(message: String) : Exception(message)

@OptIn(ExperimentalUuidApi::class)
class AuthenticationService(
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
) {
    private val authenticationTag = "Authentication"

    suspend fun loginLocally(profileId: Uuid, serverUrl: String?) {
        authenticationPreferencesRepository.loginLocally(profileId.toString(), serverUrl)
    }

    suspend fun loginRemotely(username: String, password: String, serverUrl: String): Result<LoginResponse?> {
        val loginUrl = "$serverUrl/login"

        try {
            val response: HttpResponse = createHttpClient().use { client ->
                client.post(loginUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        LoginDto(username, password)
                    )
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val loginResponse = response.body<LoginResponse>()
                authenticationPreferencesRepository.loginRemotely(
                    loginResponse.tokenResponse.accessToken,
                    loginResponse.tokenResponse.refreshToken,
                    loginResponse.tokenResponse.expiresIn,
                    loginResponse.tokenResponse.refreshExpiresIn,
                    serverUrl
                )
                Logger.withTag(authenticationTag).d("Login successful")

                return Result.success(loginResponse)
            } else {
                val problem = try { response.body<ProblemDetails>() } catch (e: Exception) { null }
                val detail = problem?.detail ?: "Login failed with status ${response.status}"
                return when (response.status) {
                    HttpStatusCode.Unauthorized -> Result.failure(InvalidCredentialsException(detail))
                    HttpStatusCode.NotFound -> Result.failure(ProfileNotFoundException(detail))
                    else -> Result.failure(Exception(detail))
                }
            }
        } catch (e: Exception) {
            Logger.withTag(authenticationTag).e(e) { "Error logging in user $username" }
            return Result.failure(ServerUnreachableException("Server unreachable", e))
        }
    }

    private fun createHttpClient(): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        expectSuccess = false
    }

    suspend fun logoutRemotely(): Result<Boolean> {
        try {
            authenticationPreferencesRepository.logoutRemotely()
            return Result.success(true)
        } catch (exception: Exception) {
            Logger.withTag(authenticationTag).e(exception) { "Error logging out remote user" }
            return Result.failure(exception)
        }
    }

    suspend fun logout(): Result<Boolean> {
        try {
            authenticationPreferencesRepository.logout()
            return Result.success(true)
        } catch (exception: Exception) {
            Logger.withTag(authenticationTag).e(exception) { "Error logging out user" }
            return Result.failure(exception)
        }
    }

    suspend fun refreshToken(serverUrl: String): Result<Boolean> {
        val refreshUrl = "$serverUrl/refresh"

        try {
            val authenticationPreferences =
                authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
                    ?: return Result.success(false)

            val response: HttpResponse = createHttpClient().use {
                it.post(refreshUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        RefreshTokenDto(authenticationPreferences.refreshToken)
                    )
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val tokenResponse = response.body<TokenResponse>()
                authenticationPreferencesRepository.updateAccessToken(
                    tokenResponse.accessToken,
                    tokenResponse.refreshToken,
                    tokenResponse.expiresIn,
                    tokenResponse.refreshExpiresIn
                )
                return Result.success(true)
            }
        } catch (e: Exception) {
            Logger.withTag(authenticationTag).e(e) { "Error refreshing token" }
            return Result.failure(e)
        }

        return Result.success(false)
    }

    suspend fun registerUser(
        username: String,
        email: String,
        password: String,
        serverUrl: String
    ): Result<RegistrationResponse?> {
        val registerUrl = "$serverUrl/register"

        try {
            val response: HttpResponse = createHttpClient().use {
                it.post(registerUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        RegistrationDto(username, email, password)
                    )
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val registrationResponse = response.body<RegistrationResponse>()
                authenticationPreferencesRepository.loginRemotely(
                    registrationResponse.tokenResponse.accessToken,
                    registrationResponse.tokenResponse.refreshToken,
                    registrationResponse.tokenResponse.expiresIn,
                    registrationResponse.tokenResponse.refreshExpiresIn,
                    serverUrl
                )
                return Result.success(registrationResponse)
            } else {
                val problem = try { response.body<ProblemDetails>() } catch (e: Exception) { null }
                val detail = problem?.detail ?: "Registration failed with status ${response.status}"
                return when (response.status) {
                    HttpStatusCode.Conflict -> Result.failure(UserAlreadyExistsException(detail))
                    HttpStatusCode.BadRequest -> Result.failure(Exception(detail))
                    else -> Result.failure(ServerErrorException(detail))
                }
            }
        } catch (e: Exception) {
            Logger.withTag(authenticationTag).e(e) { "Error registering user $username" }
            return Result.failure(ServerUnreachableException("Server unreachable", e))
        }
    }

    suspend fun updateUser(
        serverUrl: String,
        username: String? = null,
        email: String? = null,
        password: String? = null,
        currentPassword: String? = null
    ): Result<Boolean> {
        val updateUrl = "$serverUrl/profile"

        try {
            val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull() ?: return Result.failure(Exception("Not authenticated"))
            val response: HttpResponse = createHttpClient().use {
                it.patch(updateUrl) {
                    header(HttpHeaders.Authorization, "Bearer ${prefs.accessToken}")
                    contentType(ContentType.Application.Json)
                    setBody(UpdateUserDto(username, email, password, currentPassword))
                }
            }
            return when (response.status) {
                HttpStatusCode.OK -> Result.success(true)
                HttpStatusCode.Conflict -> Result.failure(UserAlreadyExistsException("Username or email already exists"))
                HttpStatusCode.Unauthorized -> Result.failure(InvalidCredentialsException("Invalid current password"))
                else -> Result.failure(Exception("Update failed with status ${response.status}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
