package org.darthacheron.pantrypal.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.path
import io.ktor.server.response.respond
import org.darthacheron.pantrypal.server.configuration.DynamicConfigurationService
import org.darthacheron.pantrypal.shared.auth.ProblemDetails
import org.koin.ktor.ext.inject

val MaintenanceMode = createApplicationPlugin(name = "MaintenanceMode") {
    val dynamicConfigService by application.inject<DynamicConfigurationService>()

    onCall { call ->
        if (dynamicConfigService.config.features.maintenanceModeEnabled) {
            // Allow admin routes even in maintenance mode
            if (!call.request.path().startsWith("/admin")) {
                call.respond(HttpStatusCode.ServiceUnavailable, ProblemDetails(
                    title = "Maintenance Mode",
                    status = HttpStatusCode.ServiceUnavailable.value,
                    detail = "Server is currently under maintenance. Please try again later."
                ))
            }
        }
    }
}
