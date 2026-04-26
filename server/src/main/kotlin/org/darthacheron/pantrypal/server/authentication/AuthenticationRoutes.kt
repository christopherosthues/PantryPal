package org.darthacheron.pantrypal.server.authentication

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.darthacheron.pantrypal.server.keycloak.InvalidCredentialsException
import org.darthacheron.pantrypal.server.keycloak.KeycloakException
import org.darthacheron.pantrypal.server.keycloak.UserAlreadyExistsException
import org.darthacheron.pantrypal.server.networking.respondGone
import org.darthacheron.pantrypal.server.networking.respondNotFound
import org.darthacheron.pantrypal.server.profile.ProfileAlreadyExistsException
import org.darthacheron.pantrypal.server.profile.ProfileDeletedException
import org.darthacheron.pantrypal.server.profile.ProfileNotFoundException
import org.darthacheron.pantrypal.shared.auth.*
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Route.authenticationRoutes() {
    val authService by inject<AuthenticationService>()
    login(authService)
    register(authService)
    refresh(authService)
}

fun Route.login(authService: AuthenticationService) {
    post("/login") {
        val loginDto = call.receive<LoginDto>()
        authService.login(loginDto).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure { e ->
            when (e) {
                is InvalidCredentialsException -> call.respond(
                    HttpStatusCode.Unauthorized, ProblemDetails(
                        title = "Invalid credentials",
                        status = HttpStatusCode.Unauthorized.value,
                        detail = e.message ?: "Keycloak rejected the login attempt. Please check your username/email and password."
                    )
                )
                is ProfileNotFoundException -> call.respondNotFound(
                    title = "Profile not found",
                    detail = e.message ?: "Credentials valid in Keycloak, but no associated profile found in server database."
                )
                is ProfileDeletedException -> call.respondGone(
                    title = "Profile deleted",
                    detail = e.message ?: "Credentials valid in Keycloak, but associated profile has been deleted in server database."
                )
                else -> call.respond(
                    HttpStatusCode.InternalServerError, ProblemDetails(
                        title = "Login failed",
                        status = HttpStatusCode.InternalServerError.value,
                        detail = e.message ?: "An unexpected error occurred during login."
                    )
                )
            }
        }
    }
}

fun Route.register(authService: AuthenticationService) {
    post("/register") {
        val registrationDto = call.receive<RegistrationDto>()
        authService.register(registrationDto).onSuccess {
            call.respond(HttpStatusCode.Created, it)
        }.onFailure { e ->
            when (e) {
                is UserAlreadyExistsException -> call.respond(
                    HttpStatusCode.Conflict, ProblemDetails(
                        type = "https://pantrypal.org/probs/keycloak-conflict",
                        title = "Conflict in Identity Provider",
                        status = HttpStatusCode.Conflict.value,
                        detail = e.message ?: "A user with this username or email already exists."
                    )
                )
                is ProfileAlreadyExistsException -> call.respond(
                    HttpStatusCode.Conflict, ProblemDetails(
                        type = "https://pantrypal.org/probs/duplicate-profile",
                        title = "Profile already exists",
                        status = HttpStatusCode.Conflict.value,
                        detail = e.message ?: "A profile with this username or email already exists in the server database.",
                        errors = mapOf("profile" to listOf(e.detail))
                    )
                )
                is KeycloakException -> call.respond(
                    e.status, ProblemDetails(
                        title = "Failed to create user",
                        status = e.status.value,
                        detail = e.message
                    )
                )
                else -> call.respond(
                    HttpStatusCode.InternalServerError, ProblemDetails(
                        title = "Registration failed",
                        status = HttpStatusCode.InternalServerError.value,
                        detail = e.message ?: "An unexpected error occurred during registration."
                    )
                )
            }
        }
    }
}

fun Route.refresh(authService: AuthenticationService) {
    post("/refresh") {
        val refreshDto = call.receive<RefreshTokenDto>()
        authService.refresh(refreshDto).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure { e ->
            when (e) {
                is InvalidCredentialsException -> call.respond(
                    HttpStatusCode.Unauthorized, ProblemDetails(
                        title = "Invalid refresh token",
                        status = HttpStatusCode.Unauthorized.value,
                        detail = e.message ?: "Keycloak rejected the refresh token."
                    )
                )
                else -> call.respond(
                    HttpStatusCode.InternalServerError, ProblemDetails(
                        title = "Refresh failed",
                        status = HttpStatusCode.InternalServerError.value,
                        detail = e.message ?: "An unexpected error occurred during token refresh."
                    )
                )
            }
        }
    }
}
