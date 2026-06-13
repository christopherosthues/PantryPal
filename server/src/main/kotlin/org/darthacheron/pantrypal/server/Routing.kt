package org.darthacheron.pantrypal.server

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.darthacheron.pantrypal.server.authentication.authenticationRoutes
import org.darthacheron.pantrypal.server.food.foodRoutes
import org.darthacheron.pantrypal.server.inventory.inventoryItemRoutes
import org.darthacheron.pantrypal.server.networking.connectionRoutes
import org.darthacheron.pantrypal.server.profile.profileRoutes
import org.darthacheron.pantrypal.core.auth.LoginDto
import org.darthacheron.pantrypal.core.configuration.ServerDynamicConfiguration

fun Application.configureRouting() {
    install(RequestValidation) {
        validate<String> { bodyText ->
            if (!bodyText.startsWith("Hello"))
                ValidationResult.Invalid("Body text should start with 'Hello'")
            else ValidationResult.Valid
        }
        validate<LoginDto> {
            if (it.username.isBlank()) {
                ValidationResult.Invalid("${it::username} must not be empty")
            } else if (it.password.isBlank()) {
                ValidationResult.Invalid("${it::password} must not be empty")
            } else {
                ValidationResult.Valid
            }
        }
        validate<ServerDynamicConfiguration> {
            if (it.deletion.gracePeriodDays < 0) {
                ValidationResult.Invalid("Grace period days cannot be negative")
            } else if (it.storage.maxImageUploadSizeMB <= 0) {
                ValidationResult.Invalid("Max image upload size must be positive")
            } else if (it.rateLimiting.rateLimitCapacity <= 0) {
                ValidationResult.Invalid("Rate limit capacity must be positive")
            } else if (it.diagnostics.telemetrySamplingRate !in 0.0..1.0) {
                ValidationResult.Invalid("Telemetry sampling rate must be between 0.0 and 1.0")
            } else {
                ValidationResult.Valid
            }
        }
    }

    routing {
        route("/api/v1") {
            authenticationRoutes()
            connectionRoutes()

            authenticate("auth-jwt") {
                profileRoutes()
                foodRoutes()
                inventoryItemRoutes()
            }
        }
    }
}
