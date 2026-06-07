package org.darthacheron.pantrypal.server.authentication

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.darthacheron.pantrypal.server.networking.respondProblem
import org.darthacheron.pantrypal.core.auth.*
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
            call.respondProblem(e)
        }
    }
}

fun Route.register(authService: AuthenticationService) {
    post("/register") {
        val registrationDto = call.receive<RegistrationDto>()
        authService.register(registrationDto).onSuccess {
            call.respond(HttpStatusCode.Created, it)
        }.onFailure { e ->
            call.respondProblem(e)
        }
    }
}

fun Route.refresh(authService: AuthenticationService) {
    post("/refresh") {
        val refreshDto = call.receive<RefreshTokenDto>()
        authService.refresh(refreshDto).onSuccess {
            call.respond(HttpStatusCode.OK, it)
        }.onFailure { e ->
            call.respondProblem(e)
        }
    }
}
