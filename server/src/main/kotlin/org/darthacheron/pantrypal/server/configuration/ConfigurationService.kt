package org.darthacheron.pantrypal.server.configuration

import io.ktor.server.application.ApplicationEnvironment

class ConfigurationService(environment: ApplicationEnvironment) {
    val keycloakBaseUrl = environment.config.property("keycloak.baseUrl").getString()
    val keycloakClientId = environment.config.property("keycloak.clientId").getString()
    val keycloakRealm = environment.config.property("keycloak.realm").getString()
    val keycloakAdminUser = environment.config.property("keycloak.adminUser").getString()
    val keycloakAdminPassword = environment.config.property("keycloak.adminPassword").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtDomain = environment.config.property("jwt.domain").getString()
    val foodImagesPath = System.getenv("FOOD_IMAGES_PATH") ?: "food-images"
    val inventoryImagesPath = System.getenv("INVENTORY_IMAGES_PATH") ?: "inventory-images"
}
