package org.darthacheron.pantrypal.server

import com.auth0.jwk.UrlJwkProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.csrf.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.darthacheron.pantrypal.server.food.foodRoutes
import org.darthacheron.pantrypal.server.inventory.inventoryItemRoutes
import org.darthacheron.pantrypal.server.profile.ProfileRepository
import org.darthacheron.pantrypal.server.profile.profileRoutes
import org.darthacheron.pantrypal.shared.auth.*
import org.darthacheron.pantrypal.shared.profile.ProfileDto
import org.koin.ktor.ext.inject
import java.net.URI
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Application.configureSecurity() {
    val profileRepository by inject<ProfileRepository>()
    val keycloakBaseUrl = environment.config.property("keycloak.baseUrl").getString()
    val keycloakClientId = environment.config.property("keycloak.clientId").getString()
    val keycloakRealm = environment.config.property("keycloak.realm").getString()
    val keycloakAdminUser = environment.config.property("keycloak.adminUser").getString()
    val keycloakAdminPassword = environment.config.property("keycloak.adminPassword").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtDomain = environment.config.property("jwt.domain").getString()

    val jwkProvider = UrlJwkProvider(URI("$keycloakBaseUrl/protocol/openid-connect/certs").toURL())
    
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    authentication {
        jwt("auth-jwt") {
            realm = keycloakRealm
            verifier(jwkProvider, jwtDomain) {
                acceptLeeway(3)
                withAudience(jwtAudience)
                withIssuer(jwtDomain)
            }
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }

        oauth("auth-oauth-keycloak") {
            urlProvider = { "http://localhost:8080/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "keycloak",
                    authorizeUrl = "$keycloakBaseUrl/protocol/openid-connect/auth",
                    accessTokenUrl = "$keycloakBaseUrl/protocol/openid-connect/token",
                    requestMethod = HttpMethod.Post,
                    clientId = keycloakClientId,
                    clientSecret = "", // Public client
                    defaultScopes = listOf("openid", "profile", "email", "offline_access")
                )
            }
            client = HttpClient(CIO)
        }
    }

    install(CSRF) {
        allowOrigin("http://localhost:8080")
        originMatchesHost()
        checkHeader("X-CSRF-Token")
    }

    routing {
        post("/login") {
            val loginDto = call.receive<LoginDto>()
            try {
                val response: HttpResponse = httpClient.submitForm(
                    url = "$keycloakBaseUrl/protocol/openid-connect/token",
                    formParameters = parameters {
                        append("grant_type", "password")
                        append("client_id", keycloakClientId)
                        append("username", loginDto.username)
                        append("password", loginDto.password)
                        append("scope", "openid profile email offline_access")
                    }
                )

                if (response.status == HttpStatusCode.OK) {
                    val tokenResponse = response.body<TokenResponse>()
                    
                    val profile = profileRepository.getProfileByUsernameOrEmail(loginDto.username)
                    
                    if (profile == null) {
                        call.respond(HttpStatusCode.NotFound, ProblemDetails(
                            title = "Profile not found",
                            status = HttpStatusCode.NotFound.value,
                            detail = "Credentials valid in Keycloak, but no associated profile found in server database."
                        ))
                        return@post
                    }

                    val userResponse = UserResponse(
                        id = profile.serverId.toString(),
                        username = profile.username,
                        email = profile.email
                    )
                    call.respond(LoginResponse(tokenResponse, userResponse))
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

        post("/register") {
            val registrationDto = call.receive<RegistrationDto>()
            try {
                // 1. Get Admin Token
                val adminTokenResponse: HttpResponse = httpClient.submitForm(
                    url = "$keycloakBaseUrl/protocol/openid-connect/token",
                    formParameters = parameters {
                        append("grant_type", "password")
                        append("client_id", "admin-cli")
                        append("username", keycloakAdminUser)
                        append("password", keycloakAdminPassword)
                    }
                )
                // TODO: register also if user created a local profile and decides to sync with server where the profile does not yet exist -> possibly has to change username and email
                // TODO: possibility to change server username + email + password

                if (adminTokenResponse.status != HttpStatusCode.OK) {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to get admin token")
                    return@post
                }

                val adminToken = adminTokenResponse.body<TokenResponse>().accessToken

                // 2. Create User in Keycloak
                val createUserResponse: HttpResponse = httpClient.post("$keycloakBaseUrl/admin/realms/$keycloakRealm/users") {
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
                        url = "$keycloakBaseUrl/protocol/openid-connect/token",
                        formParameters = parameters {
                            append("grant_type", "password")
                            append("client_id", keycloakClientId)
                            append("username", registrationDto.username)
                            append("password", registrationDto.password)
                            append("scope", "openid profile email offline_access")
                        }
                    )

                    if (loginResponse.status == HttpStatusCode.OK) {
                        val tokenResponse = loginResponse.body<TokenResponse>()

                        // 4. Create local profile
                        try {
                            val profile = profileRepository.createProfile(
                                ProfileDto(
                                    serverId = null,
                                    clientId = Uuid.random(), // This should ideally come from the client or be mapped
                                    username = registrationDto.username,
                                    email = registrationDto.email,
                                    createdAt = Clock.System.now(),
                                    lastModifiedAt = Clock.System.now(),
                                    lastSyncedAt = Clock.System.now()
                                )
                            )

                            val userResponse = UserResponse(
                                id = profile.serverId.toString(),
                                username = profile.username,
                                email = profile.email
                            )
                            call.respond(RegistrationResponse(tokenResponse, userResponse))
                        } catch (e: Exception) {
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
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Registration failed")
            }
        }

        post("/refresh") {
            val refreshDto = call.receive<RefreshTokenDto>()
            try {
                val response: HttpResponse = httpClient.submitForm(
                    url = "$keycloakBaseUrl/protocol/openid-connect/token",
                    formParameters = parameters {
                        append("grant_type", "refresh_token")
                        append("client_id", keycloakClientId)
                        append("refresh_token", refreshDto.refreshToken)
                    }
                )

                if (response.status == HttpStatusCode.OK) {
                    val tokenResponse = response.body<TokenResponse>()
                    call.respond(tokenResponse)
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid refresh token")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Refresh failed")
            }
        }

        authenticate("auth-jwt") {
            profileRoutes()
            foodRoutes()
            inventoryItemRoutes()
        }
    }
}
