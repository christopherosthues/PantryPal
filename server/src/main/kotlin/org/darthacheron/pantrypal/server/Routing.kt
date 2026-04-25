package org.darthacheron.pantrypal.server

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import jakarta.validation.Validation
import org.darthacheron.pantrypal.server.authentication.authenticationRoutes
import org.darthacheron.pantrypal.server.food.foodRoutes
import org.darthacheron.pantrypal.server.inventory.inventoryItemRoutes
import org.darthacheron.pantrypal.server.profile.profileRoutes
import org.darthacheron.pantrypal.shared.auth.LoginDto

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
    }

    routing {
        authenticationRoutes()

        authenticate("auth-jwt") {
            profileRoutes()
            foodRoutes()
            inventoryItemRoutes()
        }
    }
}
