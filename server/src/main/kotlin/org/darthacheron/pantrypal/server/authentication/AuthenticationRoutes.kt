package org.darthacheron.pantrypal.server.authentication

import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.server.networking.createHttpClient
import org.darthacheron.pantrypal.server.profile.ProfileService
import org.darthacheron.pantrypal.shared.auth.LoginDto
import org.darthacheron.pantrypal.shared.auth.LoginResponse
import org.darthacheron.pantrypal.shared.auth.ProblemDetails
import org.darthacheron.pantrypal.shared.auth.RefreshTokenDto
import org.darthacheron.pantrypal.shared.auth.RegistrationDto
import org.darthacheron.pantrypal.shared.auth.RegistrationResponse
import org.darthacheron.pantrypal.shared.auth.TokenResponse
import org.darthacheron.pantrypal.shared.auth.UserResponse
import org.darthacheron.pantrypal.shared.profile.ProfileDto
import org.koin.ktor.ext.inject
import kotlin.getValue
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Route.authenticationRoutes() {
    login()
    register()
    refresh()
}

@OptIn(ExperimentalUuidApi::class)
fun Route.login() {
    post("/login") {
        val configurationService by inject<ConfigurationService>()
        val profileService by inject<ProfileService>()
        val httpClient = createHttpClient()
        val loginDto = call.receive<LoginDto>()
        try {
            val response: HttpResponse = httpClient.submitForm(
                url = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "password")
                    append("client_id", configurationService.keycloakClientId)
                    append("username", loginDto.username)
                    append("password", loginDto.password)
                    append("scope", "openid profile email offline_access")
                }
            )

            if (response.status == HttpStatusCode.OK) {
                val tokenResponse = response.body<TokenResponse>()

                profileService.getProfileByUsernameOrEmail(loginDto.username).onSuccess { profile ->
                    if (profile == null) {
                        call.respond(
                            HttpStatusCode.NotFound, ProblemDetails(
                                title = "Profile not found",
                                status = HttpStatusCode.NotFound.value,
                                detail = "Credentials valid in Keycloak, but no associated profile found in server database."
                            )
                        )
                    } else if (profile.deletedAt != null) {
                        call.respond(
                            HttpStatusCode.Gone, ProblemDetails(
                                title = "Profile deleted",
                                status = HttpStatusCode.Gone.value,
                                detail = "Credentials valid in Keycloak, but associated profile has been deleted in server database."
                            )
                        )
                    } else {
                        val userResponse = UserResponse(
                            id = profile.serverId.toString(),
                            username = profile.username,
                            email = profile.email
                        )
                        call.respond(HttpStatusCode.OK, LoginResponse(tokenResponse, userResponse))
                    }
                }.onFailure { e ->
                    call.respond(
                        HttpStatusCode.InternalServerError, ProblemDetails(
                            title = "Login failed",
                            status = HttpStatusCode.InternalServerError.value,
                            detail = e.message ?: "An unexpected error occurred during login."
                        )
                    )
                }
            } else {
                call.respond(HttpStatusCode.Unauthorized, ProblemDetails(
                    title = "Invalid credentials",
                    status = HttpStatusCode.Unauthorized.value,
                    detail = "Keycloak rejected the login attempt. Please check your username/email and password."
                ))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                title = "Login failed",
                status = HttpStatusCode.InternalServerError.value,
                detail = e.message ?: "An unexpected error occurred during login."
            ))
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
fun Route.register() {
    post("/register") {
        val configurationService by inject<ConfigurationService>()
        val profileService by inject<ProfileService>()
        val httpClient = createHttpClient()
        val registrationDto = call.receive<RegistrationDto>()
        try {
            // 1. Get Admin Token
            val adminTokenResponse: HttpResponse = httpClient.submitForm(
                url = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "password")
                    append("client_id", "admin-cli")
                    append("username", configurationService.keycloakAdminUser)
                    append("password", configurationService.keycloakAdminPassword)
                }
            )

            if (adminTokenResponse.status != HttpStatusCode.OK) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to get admin token")
                return@post
            }

            val adminToken = adminTokenResponse.body<TokenResponse>().accessToken

            // 2. Create User in Keycloak
            val createUserResponse: HttpResponse = httpClient.post("${configurationService.keycloakBaseUrl}/admin/realms/${configurationService.keycloakRealm}/users") {
                header(HttpHeaders.Authorization, "Bearer $adminToken")
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "username" to registrationDto.username,
                    "email" to registrationDto.email,
                    "enabled" to true,
                    "credentials" to listOf(mapOf(
                        "type" to "password",
                        "value" to registrationDto.password,
                        "temporary" to false
                    ))
                ))
            }

            if (createUserResponse.status == HttpStatusCode.Created) {
                // 3. Login with new credentials to get tokens for the response
                val loginResponse: HttpResponse = httpClient.submitForm(
                    url = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/token",
                    formParameters = parameters {
                        append("grant_type", "password")
                        append("client_id", configurationService.keycloakClientId)
                        append("username", registrationDto.username)
                        append("password", registrationDto.password)
                        append("scope", "openid profile email offline_access")
                    }
                )

                if (loginResponse.status == HttpStatusCode.OK) {
                    val tokenResponse = loginResponse.body<TokenResponse>()

                    // 4. Create local profile
                    profileService.createProfile(
                        ProfileDto(
                            serverId = null,
                            clientId = Uuid.random(),
                            username = registrationDto.username,
                            email = registrationDto.email,
                            createdAt = Clock.System.now(),
                            lastModifiedAt = Clock.System.now(),
                            lastSyncedAt = Clock.System.now()
                        )
                    ).onSuccess { profile ->
                        val userResponse = UserResponse(
                            id = profile.serverId.toString(),
                            username = profile.username,
                            email = profile.email
                        )
                        call.respond(HttpStatusCode.Created, RegistrationResponse(tokenResponse, userResponse))
                    }.onFailure { e ->
                        call.respond(HttpStatusCode.Conflict, ProblemDetails(
                            type = "https://pantrypal.org/probs/duplicate-profile",
                            title = "Profile already exists",
                            status = HttpStatusCode.Conflict.value,
                            detail = "A profile with this username or email already exists in the server database.",
                            errors = mapOf("profile" to listOf(e.message ?: "Conflict"))
                        ))
                    }
                } else {
                    call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                        title = "User created but login failed",
                        status = HttpStatusCode.InternalServerError.value,
                        detail = "Keycloak user created successfully, but initial login attempt failed."
                    ))
                }
            } else if (createUserResponse.status == HttpStatusCode.Conflict) {
                val errorBody = createUserResponse.bodyAsText()
                val detail = if (errorBody.contains("exists", ignoreCase = true)) {
                    "A user with this username or email already exists in Keycloak."
                } else {
                    "Conflict during user creation in Keycloak."
                }
                call.respond(HttpStatusCode.Conflict, ProblemDetails(
                    type = "https://pantrypal.org/probs/keycloak-conflict",
                    title = "Conflict in Identity Provider",
                    status = HttpStatusCode.Conflict.value,
                    detail = detail
                ))
            } else {
                val errorBody = createUserResponse.bodyAsText()
                call.respond(createUserResponse.status, ProblemDetails(
                    title = "Failed to create user",
                    status = createUserResponse.status.value,
                    detail = errorBody
                ))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                title = "Registration failed",
                status = HttpStatusCode.InternalServerError.value,
                detail = e.message ?: "An unexpected error occurred during registration."
            ))
        }
    }
}

fun Route.refresh() {
    post("/refresh") {
        val configurationService by inject<ConfigurationService>()
        val httpClient = createHttpClient()
        val refreshDto = call.receive<RefreshTokenDto>()
        try {
            val response: HttpResponse = httpClient.submitForm(
                url = "${configurationService.keycloakBaseUrl}/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("client_id", configurationService.keycloakClientId)
                    append("refresh_token", refreshDto.refreshToken)
                }
            )

            if (response.status == HttpStatusCode.OK) {
                val tokenResponse = response.body<TokenResponse>()
                call.respond(HttpStatusCode.OK, tokenResponse)
            } else {
                call.respond(HttpStatusCode.Unauthorized, ProblemDetails(
                    title = "Invalid refresh token",
                    status = HttpStatusCode.Unauthorized.value,
                    detail = "Keycloak rejected the refresh token."
                ))
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                title = "Refresh failed",
                status = HttpStatusCode.InternalServerError.value,
                detail = e.message ?: "An unexpected error occurred during token refresh."
            ))
        }
    }
}
