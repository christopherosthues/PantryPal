package org.darthacheron.pantrypal.server.keycloak

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.server.networking.createHttpClient
import org.darthacheron.pantrypal.shared.auth.TokenResponse
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class KeycloakService(private val configurationService: ConfigurationService) {
    private val logger = LoggerFactory.getLogger(KeycloakService::class.java)
    private val httpClient = createHttpClient()

    suspend fun getAccessToken(username: String, password: String): Result<TokenResponse> {
        val response = runCatching {
            httpClient.submitForm(
                url = "${configurationService.keycloakBaseUrl}/realms/${configurationService.keycloakRealm}/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "password")
                    append("client_id", configurationService.keycloakClientId)
                    append("client_secret", configurationService.keycloakClientSecret)
                    append("username", username)
                    append("password", password)
                    append("scope", "openid profile email offline_access PantryPal")
                }
            )
        }.getOrElse { return Result.failure(it) }

        if (response.status == HttpStatusCode.OK) {
            return runCatching { response.body<TokenResponse>() }
        }

        val errorResponse = runCatching { response.body<KeycloakErrorResponse>() }.getOrNull()
        val error = errorResponse?.error
        val description = errorResponse?.errorDescription

        logger.warn("Keycloak login failed for user: {}, status: {}, error: {}, description: {}", username, response.status, error, description)

        return when {
            response.status == HttpStatusCode.BadRequest && error == "invalid_grant" -> {
                if (description == "Account is not fully set up") {
                    Result.failure(AccountNotFullySetUpException("Account is not fully set up in Keycloak."))
                } else {
                    Result.failure(InvalidCredentialsException("Keycloak rejected the login attempt. Please check your username/email and password."))
                }
            }
            response.status == HttpStatusCode.Unauthorized && error == "invalid_client" -> {
                Result.failure(KeycloakException(response.status, "Invalid client credentials."))
            }
            response.status == HttpStatusCode.NotFound && error == "Realm does not exist" -> {
                Result.failure(KeycloakException(response.status, "Keycloak realm does not exist."))
            }
            error == "unauthorized_client" -> {
                Result.failure(KeycloakException(response.status, "Client not allowed for direct access grants."))
            }
            error == "invalid_scope" -> {
                Result.failure(KeycloakException(response.status, "Invalid scope requested: $description"))
            }
            else -> {
                Result.failure(KeycloakException(response.status, description ?: error ?: "Keycloak login failed."))
            }
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): Result<TokenResponse> {
        val response = runCatching {
            httpClient.submitForm(
                url = "${configurationService.keycloakBaseUrl}/realms/${configurationService.keycloakRealm}/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("client_id", configurationService.keycloakClientId)
                    append("refresh_token", refreshToken)
                }
            )
        }.getOrElse { return Result.failure(it) }

        if (response.status == HttpStatusCode.OK) {
            return runCatching { response.body<TokenResponse>() }
        }

        val errorResponse = runCatching { response.body<KeycloakErrorResponse>() }.getOrNull()
        logger.warn("Keycloak token refresh failed, status: {}, error: {}, description: {}",
            response.status, errorResponse?.error, errorResponse?.errorDescription)

        return Result.failure(InvalidCredentialsException("Keycloak rejected the refresh token."))
    }

    suspend fun createUser(username: String, email: String, password: String): Result<Uuid> {
        val adminToken = getAdminToken().getOrElse { return Result.failure(it) }

        val response = runCatching {
            httpClient.post("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users") {
                header(HttpHeaders.Authorization, "Bearer $adminToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "username" to username,
                    "email" to email,
                    "emailVerified" to true,
                    "enabled" to true,
                    "credentials" to listOf(mapOf(
                        "type" to "password",
                        "value" to password,
                        "temporary" to false
                    ))
                ))
            }
        }.getOrElse { return Result.failure(it) }

        return when (response.status) {
            HttpStatusCode.Created -> {
                val location = response.headers[HttpHeaders.Location]
                val userId = location?.substringAfterLast("/")?.let { runCatching { Uuid.parse(it) }.getOrNull() }
                    ?: return Result.failure(Exception("User created in Keycloak, but failed to extract user ID from Location header."))
                Result.success(userId)
            }
            HttpStatusCode.Conflict -> {
                val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
                val detail = if (errorBody.contains("exists", ignoreCase = true)) {
                    "A user with this username or email already exists."
                } else {
                    "Conflict during user creation."
                }
                Result.failure(UserAlreadyExistsException(detail))
            }
            else -> {
                val errorBody = runCatching { response.bodyAsText() }.getOrDefault("")
                logger.error("Failed to create user in Keycloak, status: {}, body: {}", response.status, errorBody)
                Result.failure(KeycloakException(response.status, errorBody))
            }
        }
    }

    suspend fun updateUser(
        userId: Uuid,
        username: String? = null,
        email: String? = null,
        password: String? = null
    ): Result<Unit> {
        val adminToken = getAdminToken().getOrElse { return Result.failure(it) }

        val response = runCatching {
            httpClient.put("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users/$userId") {
                header(HttpHeaders.Authorization, "Bearer $adminToken")
                contentType(ContentType.Application.Json)
                val body = mutableMapOf<String, Any>()
                username?.let { body["username"] = it }
                email?.let { body["email"] = it }
                password?.let {
                    body["credentials"] = listOf(
                        mapOf(
                            "type" to "password",
                            "value" to it,
                            "temporary" to false
                        )
                    )
                }
                setBody(body)
            }
        }.getOrElse { return Result.failure(it) }

        return if (response.status != HttpStatusCode.NoContent && response.status != HttpStatusCode.OK) {
            logger.error("Failed to update Keycloak user ID: {}, status: {}", userId, response.status)
            Result.failure(KeycloakException(response.status, "Failed to update user in Keycloak"))
        } else {
            Result.success(Unit)
        }
    }

    suspend fun deleteUser(userId: Uuid): Result<Unit> {
        val adminToken = getAdminToken().getOrElse { return Result.failure(it) }

        val response = runCatching {
            httpClient.delete("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users/$userId") {
                header(HttpHeaders.Authorization, "Bearer $adminToken")
            }
        }.getOrElse { return Result.failure(it) }

        return if (response.status != HttpStatusCode.NoContent && response.status != HttpStatusCode.OK) {
            logger.error("Failed to delete Keycloak user ID: {}, status: {}", userId, response.status)
            Result.failure(KeycloakException(response.status, "Failed to delete user from Keycloak"))
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun getAdminToken(): Result<String> {
        val response = runCatching {
            httpClient.submitForm(
                url = "${configurationService.keycloakBaseUrl}/realms/master/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "password")
                    append("client_id", "admin-cli")
                    append("username", configurationService.keycloakAdminUser)
                    append("password", configurationService.keycloakAdminPassword)
                }
            )
        }.getOrElse {
            logger.error("Failed to get Keycloak admin token due to network/client error", it)
            return Result.failure(it)
        }

        if (response.status != HttpStatusCode.OK) {
            logger.error("Failed to get Keycloak admin token, status: {}", response.status)
            return Result.failure(KeycloakException(response.status, "Failed to get admin token"))
        }

        return runCatching { response.body<TokenResponse>().accessToken }
            .onFailure { logger.error("Failed to parse admin token response", it) }
    }
}
