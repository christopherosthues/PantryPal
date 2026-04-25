package org.darthacheron.pantrypal.server

import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.darthacheron.pantrypal.server.camera.ImagesTable
import org.darthacheron.pantrypal.server.configuration.ConfigurationService
import org.darthacheron.pantrypal.server.food.FoodsTable
import org.darthacheron.pantrypal.server.inventory.InventoryItemsTable
import org.darthacheron.pantrypal.server.profile.ProfilesTable
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.koin.ktor.ext.inject
import kotlin.system.exitProcess

@OptIn(ExperimentalDatabaseMigrationApi::class)
fun Application.configureDatabases() {
    val config by inject<ConfigurationService>()

    Database.connect(
        url = config.postgresUrl,
        driver = "org.postgresql.Driver",
        user = config.postgresUser,
        password = config.postgresPassword
    )

    if (config.generateMigration) {
        log.info("Generating migration script to: ${config.migrationsPath}")
        
        transaction {
            val schema = Schema(config.postgresSchema)
            SchemaUtils.createSchema(schema)
            SchemaUtils.setSchema(schema)

            val timestamp = System.currentTimeMillis()
            MigrationUtils.generateMigrationScript(
                ProfilesTable,
                FoodsTable,
                InventoryItemsTable,
                ImagesTable,
                scriptDirectory = config.migrationsPath,
                scriptName = "migration_$timestamp"
            )
        }
        
        log.info("Migration script generated successfully. Exiting.")
        exitProcess(0)
    }

    // Normal startup - we don't use createMissingTablesAndColumns anymore as per best practice.
    // We assume migrations are applied externally (e.g. by admin via PGAdmin using generated script).
    transaction {
        val schema = Schema(config.postgresSchema)
        SchemaUtils.createSchema(schema)
    }
}
