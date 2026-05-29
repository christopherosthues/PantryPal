package org.darthacheron.pantrypal.server.networking

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.shared.NetworkingConstants
import org.koin.ktor.ext.inject

fun Route.connectionRoutes() {
    val configurationService by inject<ConfigurationService>()

    route("/api/connection") {
        get("/test") {
            val appToken = call.request.headers[NetworkingConstants.APP_TOKEN_HEADER]
            if (appToken == configurationService.appToken) {
                call.respond(HttpStatusCode.OK, "Connection successful")
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid application token")
            }
        }
    }
}
