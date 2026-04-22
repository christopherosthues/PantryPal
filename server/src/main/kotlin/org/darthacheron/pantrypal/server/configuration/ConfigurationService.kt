package org.darthacheron.pantrypal.server.configuration

import io.ktor.server.application.ApplicationEnvironment
import org.slf4j.LoggerFactory

class ConfigurationService(environment: ApplicationEnvironment) {
    private val logger = LoggerFactory.getLogger(ConfigurationService::class.java)

    val keycloakBaseUrl = environment.config.property("keycloak.baseUrl").getString()
    val keycloakClientId = environment.config.property("keycloak.clientId").getString()
    val keycloakRealm = environment.config.property("keycloak.realm").getString()
    val keycloakAdminUser = environment.config.property("keycloak.adminUser").getString()
    val keycloakAdminPassword = environment.config.property("keycloak.adminPassword").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtDomain = environment.config.property("jwt.domain").getString()
    val foodImagesPath = (System.getenv("FOOD_IMAGES_PATH") ?: "food-images").also {
        logger.info("Food images path: {}", it)
    }
    val inventoryImagesPath = (System.getenv("INVENTORY_IMAGES_PATH") ?: "inventory-images").also {
        logger.info("Inventory images path: {}", it)
    }

    init {
        logger.info("ConfigurationService initialized with Keycloak URL: {}", keycloakBaseUrl)
    }
}
