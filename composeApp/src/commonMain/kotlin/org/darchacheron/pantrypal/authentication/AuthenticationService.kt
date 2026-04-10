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
import org.darchacheron.pantrypal.settings.DataSynchronization
import org.darchacheron.pantrypal.settings.SettingsRepository
import org.darthacheron.pantrypal.shared.auth.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class AuthenticationService(
    private val preferencesRepository: AuthenticationPreferencesRepository,
    private val settingsRepository: SettingsRepository
) {
    private val authenticationTag = "Authentication"

    suspend fun isRemoteEnabled(): Boolean {
        val settings = settingsRepository.getSettings()
        return settings.serverUrl.isNotBlank() && settings.dataSynchronization != DataSynchronization.NO_SYNCHRONIZATION
    }

    suspend fun login(username: String, password: String): Result<LoginResponse?> {
        if (!isRemoteEnabled()) return Result.success(null)

        val settings = settingsRepository.getSettings()
        val loginUrl = "${settings.serverUrl}/login"

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
                preferencesRepository.updateAccessPreferences(
                    loginResponse.tokenResponse.accessToken,
                    loginResponse.tokenResponse.refreshToken,
                    loginResponse.tokenResponse.expiresIn,
                    loginResponse.tokenResponse.refreshExpiresIn
                )
                Logger.withTag(authenticationTag).d("Login successful")

                return Result.success(loginResponse)
            } else {
                val problem = try { response.body<ProblemDetails>() } catch (e: Exception) { null }
                return Result.failure(Exception(problem?.detail ?: "Login failed with status ${response.status}"))
            }
        } catch (e: Exception) {
            Logger.withTag(authenticationTag).e(e) { "Error logging in user $username" }
            return Result.failure(e)
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

    suspend fun logout(): Result<Boolean> {
        try {
            // Passing null for localProfileId to preserve it in updateAccessPreferences
            // Setting isLoggedInRemotely to false on logout
            preferencesRepository.updateAccessPreferences("", "", 0, 0, null, isLoggedInRemotely = false)
            return Result.success(true)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    suspend fun refreshToken(): Result<Boolean> {
        if (!isRemoteEnabled()) return Result.success(false)

        val settings = settingsRepository.getSettings()
        val refreshUrl = "${settings.serverUrl}/refresh"

        try {
            val authenticationPreferences =
                preferencesRepository.authenticationPreferencesFlow.firstOrNull()
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
                preferencesRepository.updateAccessPreferences(
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
    ): Result<RegistrationResponse?> {
        if (!isRemoteEnabled()) return Result.success(null)

        val settings = settingsRepository.getSettings()
        val registerUrl = "${settings.serverUrl}/register"

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
                preferencesRepository.updateAccessPreferences(
                    registrationResponse.tokenResponse.accessToken,
                    registrationResponse.tokenResponse.refreshToken,
                    registrationResponse.tokenResponse.expiresIn,
                    registrationResponse.tokenResponse.refreshExpiresIn
                )
                return Result.success(registrationResponse)
            } else {
                val problem = try { response.body<ProblemDetails>() } catch (e: Exception) { null }
                return Result.failure(Exception(problem?.detail ?: "Registration failed with status ${response.status}"))
            }
        } catch (e: Exception) {
            Logger.withTag(authenticationTag).e(e) { "Error registering user $username" }
            return Result.failure(e)
        }
    }

    suspend fun updateUser(
        username: String? = null,
        email: String? = null,
        password: String? = null
    ): Result<Boolean> {
        if (!isRemoteEnabled()) return Result.success(true)

        val settings = settingsRepository.getSettings()
        val updateUrl = "${settings.serverUrl}/users/me"

        try {
            val prefs = preferencesRepository.authenticationPreferencesFlow.firstOrNull() ?: return Result.failure(Exception("Not authenticated"))
            val response: HttpResponse = createHttpClient().use {
                it.patch(updateUrl) {
                    header(HttpHeaders.Authorization, "Bearer ${prefs.accessToken}")
                    contentType(ContentType.Application.Json)
                    setBody(UpdateUserDto(username, email, password))
                }
            }
            return if (response.status == HttpStatusCode.OK) Result.success(true) else Result.failure(Exception("Update failed"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun deleteUser(remote: Boolean): Result<Boolean> {
        if (!isRemoteEnabled()) return Result.success(true)

        val settings = settingsRepository.getSettings()
        val deleteUrl = "${settings.serverUrl}/users/me"

        try {
            val prefs = preferencesRepository.authenticationPreferencesFlow.firstOrNull() ?: return Result.failure(Exception("Not authenticated"))
            val response: HttpResponse = createHttpClient().use {
                it.delete(deleteUrl) {
                    header(HttpHeaders.Authorization, "Bearer ${prefs.accessToken}")
                    parameter("remote", remote)
                }
            }
            return if (response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK) Result.success(true) else Result.failure(Exception("Delete failed"))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
