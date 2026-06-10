package org.darthacheron.pantrypal.authentication

import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.firstOrNull
import org.darthacheron.pantrypal.core.auth.LoginDto
import org.darthacheron.pantrypal.core.auth.LoginResponse
import org.darthacheron.pantrypal.core.auth.ProblemDetails
import org.darthacheron.pantrypal.core.auth.RefreshTokenDto
import org.darthacheron.pantrypal.core.auth.RegistrationDto
import org.darthacheron.pantrypal.core.auth.RegistrationResponse
import org.darthacheron.pantrypal.core.auth.TokenResponse
import org.darthacheron.pantrypal.core.auth.UpdateUserDto
import org.darthacheron.pantrypal.utils.HttpClientFactory
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AuthenticationServiceImpl(
    private val authenticationPreferencesRepository: AuthenticationPreferencesRepository,
    private val clientFactory: HttpClientFactory
) : AuthenticationService {
    private val authenticationTag = "Authentication"

    override suspend fun loginLocally(profileId: Uuid, stayLoggedIn: Boolean, serverUrl: String?) {
        authenticationPreferencesRepository.loginLocally(profileId.toString(), serverUrl, stayLoggedIn)
    }

    override suspend fun loginRemotely(username: String, password: String, serverUrl: String): Result<LoginResponse?> {
        val loginUrl = "$serverUrl/login"

        try {
            val response: HttpResponse = clientFactory.create().use { httpClient ->
                httpClient.post(loginUrl) {
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
                    serverUrl,
                    Clock.System.now().toEpochMilliseconds()
                )
                Logger.withTag(authenticationTag).d("Login successful")

                return Result.success(loginResponse)
            } else {
                val problem = try {
                    response.body<ProblemDetails>()
                } catch (e: Exception) {
                    null
                }
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

    override suspend fun logoutRemotely(): Result<Boolean> {
        try {
            authenticationPreferencesRepository.logoutRemotely()
            return Result.success(true)
        } catch (exception: Exception) {
            Logger.withTag(authenticationTag).e(exception) { "Error logging out remote user" }
            return Result.failure(exception)
        }
    }

    override suspend fun logout(): Result<Boolean> {
        try {
            authenticationPreferencesRepository.logout()
            return Result.success(true)
        } catch (exception: Exception) {
            Logger.withTag(authenticationTag).e(exception) { "Error logging out user" }
            return Result.failure(exception)
        }
    }

    override suspend fun refreshToken(serverUrl: String): Result<Boolean> {
        val refreshUrl = "$serverUrl/refresh"

        try {
            val authenticationPreferences =
                authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
                    ?: return Result.success(false)
            val token = authenticationPreferences.accessToken
            if (token.isBlank()) return Result.failure(NotAuthenticatedException("No access token found"))

            val response: HttpResponse = clientFactory.create().use { httpClient ->
                httpClient.post(refreshUrl) {
                    header(HttpHeaders.Authorization, "Bearer $token")
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
                    tokenResponse.refreshExpiresIn,
                    Clock.System.now().toEpochMilliseconds()
                )
                return Result.success(true)
            } else if (response.status == HttpStatusCode.Unauthorized) {
                logoutRemotely()
                return Result.success(false)
            }
        } catch (e: Exception) {
            logoutRemotely()
            Logger.withTag(authenticationTag).e(e) { "Error refreshing token" }
            return Result.failure(e)
        }

        return Result.success(false)
    }

    override suspend fun registerUser(
        username: String,
        email: String,
        password: String,
        serverUrl: String
    ): Result<RegistrationResponse?> {
        val registerUrl = "$serverUrl/register"

        try {
            val response: HttpResponse = clientFactory.create().use { httpClient ->
                httpClient.post(registerUrl) {
                    setBody(
                        RegistrationDto(username, email, password)
                    )
                }
            }

            if (response.status == HttpStatusCode.Created) {
                val registrationResponse = response.body<RegistrationResponse>()
                authenticationPreferencesRepository.loginRemotely(
                    registrationResponse.tokenResponse.accessToken,
                    registrationResponse.tokenResponse.refreshToken,
                    registrationResponse.tokenResponse.expiresIn,
                    registrationResponse.tokenResponse.refreshExpiresIn,
                    serverUrl,
                    Clock.System.now().toEpochMilliseconds()
                )
                return Result.success(registrationResponse)
            } else {
                val problem = try {
                    response.body<ProblemDetails>()
                } catch (e: Exception) {
                    null
                }
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

    override suspend fun updateUser(
        serverUrl: String,
        username: String?,
        email: String?,
        password: String?,
        currentPassword: String?
    ): Result<Boolean> {
        val updateUrl = "$serverUrl/profile"

        try {
            val prefs = authenticationPreferencesRepository.authenticationPreferencesFlow.firstOrNull()
                ?: return Result.failure(NotAuthenticatedException("Not authenticated"))
            val token = prefs.accessToken
            if (token.isBlank()) return Result.failure(NotAuthenticatedException("No access token found"))

            val response: HttpResponse = clientFactory.create().use { httpClient ->
                httpClient.patch(updateUrl) {
                    header(HttpHeaders.Authorization, "Bearer $token")
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
