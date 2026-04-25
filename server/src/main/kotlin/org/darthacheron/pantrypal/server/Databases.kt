package org.darthacheron.pantrypal.server

import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.darthacheron.pantrypal.server.food.FoodsTable
import org.darthacheron.pantrypal.server.inventory.InventoryItemsTable
import org.darthacheron.pantrypal.server.profile.ProfilesTable
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import java.sql.Connection
import java.sql.DriverManager

fun Application.configureDatabases() {
    val dbConnection: Connection = connectToPostgres()

    transaction {
        SchemaUtils.createSchema()

        MigrationUtils.statementsRequiredForDatabaseMigration(
            ProfilesTable,
            FoodsTable,
            InventoryItemsTable,
            withLogs = true
        )
    }
}

/**
 * Makes a connection to a Postgres database.
 *
 * In order to connect to your running Postgres process,
 * please specify the following parameters in your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 * If you don't have a database process running yet, you may need to [download]((https://www.postgresql.org/download/))
 * and install Postgres and follow the instructions [here](https://postgresapp.com/).
 * Then, you would be able to edit your url,  which is usually "jdbc:postgresql://host:port/database", as well as
 * user and password values.
 *
 *
 * @return [Connection] that represent connection to the database. Please, don't forget to close this connection when
 * your application shuts down by calling [Connection.close]
 * */
fun Application.connectToPostgres(): Connection {
    Class.forName("org.postgresql.Driver")
    val url = environment.config.property("postgres.connection.string").getString()
    log.info("Connecting to postgres database at $url")
    val user = environment.config.property("postgres.user").getString()
    val password = environment.config.property("postgres.password").getString()

    Database.connect(
        url,
        driver = "org.postgresql.Driver",
        user = user,
        password = password
    )

    return DriverManager.getConnection(url, user, password)
}
