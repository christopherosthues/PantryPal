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
    
    private val rawPostgresUrl = environment.config.property("postgres.url").getString()
    val postgresUser = environment.config.property("postgres.user").getString()
    val postgresPassword = environment.config.property("postgres.password").getString()
    val postgresSchema = environment.config.property("postgres.schema").getString()
    val generateMigration = environment.config.property("postgres.generateMigration").getString().toBoolean()
    val migrationsPath = environment.config.property("postgres.migrationsPath").getString()

    val postgresUrl = if (rawPostgresUrl.contains("?")) {
        "$rawPostgresUrl&currentSchema=$postgresSchema"
    } else {
        "$rawPostgresUrl?currentSchema=$postgresSchema"
    }

    val imagesPath = environment.config.property("images.path").getString()
    val foodImagesPath = imagesPath + "food/"
    val inventoryImagesPath = imagesPath + "inventory/"

    init {
        validateConfig()
        logger.info("ConfigurationService initialized with Keycloak URL: {}", keycloakBaseUrl)
        logger.info("ConfigurationService initialized with external Keycloak URL: {}", keycloakExternalBaseUrl)
        logger.info("Food images path: {}", foodImagesPath)
        logger.info("Inventory images path: {}", inventoryImagesPath)
    }

    private fun validateConfig() {
        check(keycloakBaseUrl.isNotBlank()) { "keycloak.baseUrl must not be blank" }
        check(keycloakClientId.isNotBlank()) { "keycloak.clientId must not be blank" }
        check(keycloakRealm.isNotBlank()) { "keycloak.realm must not be blank" }
        check(keycloakAdminUser.isNotBlank()) { "keycloak.adminUser must not be blank" }
        check(keycloakAdminPassword.isNotBlank()) { "keycloak.adminPassword must not be blank" }
        check(postgresUrl.isNotBlank()) { "postgres.url must not be blank" }
    }
}
