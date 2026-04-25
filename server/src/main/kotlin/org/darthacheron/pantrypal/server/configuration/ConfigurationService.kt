package org.darthacheron.pantrypal.server.configuration

import io.ktor.server.application.ApplicationEnvironment
import org.slf4j.LoggerFactory

class ConfigurationService(environment: ApplicationEnvironment) {
    private val logger = LoggerFactory.getLogger(ConfigurationService::class.java)

    val keycloakBaseUrl = environment.config.property("keycloak.baseUrl").getString()
    val keycloakExternalBaseUrl = environment.config.property("keycloak.externalBaseUrl").getString()
    val keycloakClientId = environment.config.property("keycloak.clientId").getString()
    val keycloakClientSecret = environment.config.property("keycloak.clientSecret").getString()
    val keycloakRealm = environment.config.property("keycloak.realm").getString()
    val keycloakAudience = environment.config.property("keycloak.audience").getString()
    val keycloakAdminUser = environment.config.property("keycloak.adminUser").getString()
    val keycloakAdminPassword = environment.config.property("keycloak.adminPassword").getString()
    val keycloakIssuer = "$keycloakExternalBaseUrl/realms/$keycloakRealm"
    val imagesPath = environment.config.property("images.path").getString()
    val foodImagesPath = imagesPath + "food/"
    val inventoryImagesPath = imagesPath + "inventory/"

    init {
        logger.info("ConfigurationService initialized with Keycloak URL: {}", keycloakBaseUrl)
        logger.info("ConfigurationService initialized with external Keycloak URL: {}", keycloakExternalBaseUrl)
        logger.info("Food images path: {}", foodImagesPath)
        logger.info("Inventory images path: {}", inventoryImagesPath)
    }
}
