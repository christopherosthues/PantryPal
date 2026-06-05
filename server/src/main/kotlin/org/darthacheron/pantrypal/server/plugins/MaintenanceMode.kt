package org.darthacheron.pantrypal.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.response.respond
import org.darthacheron.pantrypal.server.configuration.DynamicConfigurationService
import org.koin.ktor.ext.inject

val MaintenanceMode = createApplicationPlugin(name = "MaintenanceMode") {
    val dynamicConfigService by application.inject<DynamicConfigurationService>()

    onCall { call ->
        if (dynamicConfigService.config.features.maintenanceModeEnabled) {
            // Allow admin routes even in maintenance mode
            if (!call.request.local.uri.startsWith("/admin")) {
                call.respond(HttpStatusCode.ServiceUnavailable, "Server is currently under maintenance. Please try again later.")
            }
        }
    }
}
