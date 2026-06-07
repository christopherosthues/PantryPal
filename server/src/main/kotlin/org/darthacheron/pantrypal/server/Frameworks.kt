package org.darthacheron.pantrypal.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respond
import org.darthacheron.pantrypal.server.authentication.AuthenticationService
import org.darthacheron.pantrypal.server.camera.ImageRepository
import org.darthacheron.pantrypal.server.camera.ImageService
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.server.configuration.DynamicConfigurationService
import org.darthacheron.pantrypal.server.food.FoodRepository
import org.darthacheron.pantrypal.server.food.FoodService
import org.darthacheron.pantrypal.server.inventory.InventoryItemRepository
import org.darthacheron.pantrypal.server.inventory.InventoryItemService
import org.darthacheron.pantrypal.server.keycloak.KeycloakService
import org.darthacheron.pantrypal.server.profile.ProfileRepository
import org.darthacheron.pantrypal.server.profile.ProfileService
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.ktor.ext.getKoin
import org.koin.logger.slf4jLogger
import org.darthacheron.pantrypal.server.networking.respondProblem
import org.darthacheron.pantrypal.core.auth.ProblemDetails

fun Application.configureFrameworks() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ProblemDetails(
                title = "Validation Error",
                status = HttpStatusCode.BadRequest.value,
                detail = cause.reasons.joinToString()
            ))
        }
        exception<Throwable> { call, cause ->
            call.respondProblem(cause)
        }
    }

    install(Koin) {
        slf4jLogger()
        modules(module {
            factoryOf(::ProfileRepository)
            factoryOf(::ProfileService)
            factoryOf(::KeycloakService)
            factoryOf(::AuthenticationService)
            factoryOf(::FoodRepository)
            factoryOf(::FoodService)
            factoryOf(::InventoryItemRepository)
            factoryOf(::InventoryItemService)
            factoryOf(::ImageRepository)
            factoryOf(::ImageService)
            single<ConfigurationService> { ConfigurationService(environment) }
            singleOf(::DynamicConfigurationService)
        })
    }

    monitor.subscribe(ApplicationStopped) {
        getKoin().get<DynamicConfigurationService>().close()
    }
}
