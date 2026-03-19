package org.darchacheron.pantrypal.authentication

import co.touchlab.kermit.Logger
import org.darchacheron.pantrypal.authentication.dtos.LoginDto
import org.darchacheron.pantrypal.authentication.dtos.RefreshTokenDto
import org.darchacheron.pantrypal.authentication.dtos.RegistrationDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.lastOrNull
import pantrypal.composeapp.generated.resources.Res

class AuthenticationService(
    private val preferencesRepository: AuthenticationPreferencesRepository
) {
    private val authenticationTag = "Authentication"

    suspend fun login(username: String, password: String): Result<Boolean> {
        val loginUrl = "https://localhost:8080/login"

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
                val tokenResponse = response.body<TokenResponse>()
                preferencesRepository.updateAccessPreferences(
                    tokenResponse.accessToken,
                    tokenResponse.refreshToken,
                    tokenResponse.expiresIn,
                    tokenResponse.refreshExpiresIn
                )
                Logger.withTag(authenticationTag).d("Login successful")

                return Result.success(true)
            }
        } catch (e: RedirectResponseException) {
            Logger.withTag(authenticationTag).e(e) { "Error logging in user $username" }
            return Result.failure(e)
        } catch (e: ClientRequestException) {
            Logger.withTag(authenticationTag).e(e) { "Error logging in user $username" }
            return Result.failure(e)
        } catch (e: ServerResponseException) {
            Logger.withTag(authenticationTag).e(e) { "Error logging in user $username" }
            return Result.failure(e)
        } catch (e: Exception) {
            Logger.withTag(authenticationTag).e(e) { "Error logging in user $username" }
            return Result.failure(e)
        }

        return Result.success(false)
    }

    private fun createHttpClient(): HttpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.INFO
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        expectSuccess = true
    }

    suspend fun logout(): Result<Boolean> {
        try {
            preferencesRepository.updateAccessPreferences("", "", 0, 0)
            return Result.success(true)
        } catch (exception: Exception) {
            return Result.failure(exception)
        }
    }

    suspend fun refreshToken(): Result<Boolean> {
        val refreshUrl = "https://localhost:8080/refresh"

        try {
            val authenticationPreferences =
                preferencesRepository.authenticationPreferencesFlow.lastOrNull()
                    ?: // TODO: logout / navigate to login screen
                    return Result.success(false)

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
        } catch (e: RedirectResponseException) {
            Logger.withTag(authenticationTag).e(e) { "Error refreshing token" }
            return Result.failure(e)
        } catch (e: ClientRequestException) {
            Logger.withTag(authenticationTag).e(e) { "Error refreshing token" }
            return Result.failure(e)
        } catch (e: ServerResponseException) {
            Logger.withTag(authenticationTag).e(e) { "Error refreshing token" }
            return Result.failure(e)
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
        firstName: String,
        lastName: String
    ): Result<Boolean> {
        val registerUrl = "https://localhost:8080/register"

        try {
            val response: HttpResponse = createHttpClient().use {
                it.post(registerUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        RegistrationDto(username, email, password, firstName, lastName)
                    )
                }
            }

            if (response.status == HttpStatusCode.OK) {
                // TODO: redirect to login page
                return Result.success(true)
            }
        } catch (e: RedirectResponseException) {
            Logger.withTag(authenticationTag).e(e) { "Error registering user $username" }
            return Result.failure(e)
        } catch (e: ClientRequestException) {
            Logger.withTag(authenticationTag).e(e) { "Error registering user $username" }
            return Result.failure(e)
        } catch (e: ServerResponseException) {
            Logger.withTag(authenticationTag).e(e) { "Error registering user $username" }
            return Result.failure(e)
        } catch (e: Exception) {
            Logger.withTag(authenticationTag).e(e) { "Error registering user $username" }
            return Result.failure(e)
        }

        return Result.success(false)
    }
}