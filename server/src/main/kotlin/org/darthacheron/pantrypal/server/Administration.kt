package org.darthacheron.pantrypal.server

import io.github.flaxoos.ktor.server.plugins.ratelimiter.*
import io.github.flaxoos.ktor.server.plugins.ratelimiter.implementations.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.darthacheron.pantrypal.server.configuration.DynamicConfigurationService
import org.darthacheron.pantrypal.server.configuration.ServerDynamicConfig
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.seconds

fun Application.configureAdministration() {
    routing {
        route("/") {
            install(RateLimiting) {
                rateLimiter {
                    type = TokenBucket::class
                    capacity = 100
                    rate = 10.seconds
                }
            }
        }

        route("/admin") {
            val dynamicConfigService by inject<DynamicConfigurationService>()

            get("/config") {
                call.respond(dynamicConfigService.config)
            }

            put("/config") {
                val newConfig = call.receive<ServerDynamicConfig>()
                dynamicConfigService.saveConfig(newConfig)
                call.respond(HttpStatusCode.OK, dynamicConfigService.config)
            }

            post("/config/reload") {
                dynamicConfigService.loadConfig()
                call.respond(HttpStatusCode.OK, dynamicConfigService.config)
            }
        }
    }
}
