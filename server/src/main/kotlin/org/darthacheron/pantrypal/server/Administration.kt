package org.darthacheron.pantrypal.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.darthacheron.pantrypal.server.authentication.AuthenticationService
import org.darthacheron.pantrypal.server.configuration.DynamicConfigurationService
import org.darthacheron.pantrypal.server.networking.respondProblem
import org.darthacheron.pantrypal.core.auth.LoginDto
import org.darthacheron.pantrypal.core.configuration.ServerDynamicConfiguration
import org.koin.ktor.ext.inject

fun Application.configureAdministration() {
    routing {
        val authService by inject<AuthenticationService>()

        post("/admin/login") {
            val loginDto = call.receive<LoginDto>()
            authService.adminLogin(loginDto).onSuccess {
                call.respond(HttpStatusCode.OK, it)
            }.onFailure { e ->
                call.respondProblem(e)
            }
        }

        authenticate("auth-jwt-admin") {
            route("/admin") {
                val dynamicConfigService by inject<DynamicConfigurationService>()

                get("/config") {
                    call.respond(dynamicConfigService.config)
                }

                put("/config") {
                    val newConfig = call.receive<ServerDynamicConfiguration>()
                    dynamicConfigService.saveConfiguration(newConfig)
                    call.respond(HttpStatusCode.OK, dynamicConfigService.config)
                }

                post("/config/reload") {
                    dynamicConfigService.loadConfiguration()
                    call.respond(HttpStatusCode.OK, dynamicConfigService.config)
                }
            }
        }
    }
}
