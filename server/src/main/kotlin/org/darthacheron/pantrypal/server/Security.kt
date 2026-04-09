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
import org.darthacheron.pantrypal.shared.auth.*
import java.net.URI

fun Application.configureSecurity() {
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
                    // In a real app, you'd decode the JWT to get user info
                    val userResponse = UserResponse(
                        id = loginDto.username, // Simplified
                        username = loginDto.username,
                        email = ""
                    )
                    call.respond(LoginResponse(tokenResponse, userResponse))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Login failed")
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
                        val userResponse = UserResponse(
                            id = registrationDto.username,
                            username = registrationDto.username,
                            email = registrationDto.email
                        )
                        call.respond(RegistrationResponse(tokenResponse, userResponse))
                    } else {
                        call.respond(HttpStatusCode.OK, "User created but login failed")
                    }
                } else {
                    val errorBody = createUserResponse.bodyAsText()
                    call.respond(createUserResponse.status, "Failed to create user: $errorBody")
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

        authenticate("auth-oauth-keycloak") {
            get("/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                if (principal != null) {
                    call.respond(TokenResponse(
                        accessToken = principal.accessToken,
                        refreshToken = principal.refreshToken ?: "",
                        expiresIn = principal.expiresIn.toInt(),
                        refreshExpiresIn = 0,
                        tokenType = principal.tokenType
                    ))
                } else {
                    call.respond(HttpStatusCode.Unauthorized)
                }
            }
        }
    }
}
