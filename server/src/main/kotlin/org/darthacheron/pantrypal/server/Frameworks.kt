package org.darthacheron.pantrypal.server

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.darthacheron.pantrypal.server.authentication.AuthenticationService
import org.darthacheron.pantrypal.server.camera.ImageRepository
import org.darthacheron.pantrypal.server.camera.ImageService
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.server.food.FoodRepository
import org.darthacheron.pantrypal.server.food.FoodService
import org.darthacheron.pantrypal.server.inventory.InventoryItemRepository
import org.darthacheron.pantrypal.server.inventory.InventoryItemService
import org.darthacheron.pantrypal.server.keycloak.KeycloakService
import org.darthacheron.pantrypal.server.profile.ProfileRepository
import org.darthacheron.pantrypal.server.profile.ProfileService
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
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
        })
    }
}
