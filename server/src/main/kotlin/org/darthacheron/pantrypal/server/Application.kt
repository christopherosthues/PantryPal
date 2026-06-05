package org.darthacheron.pantrypal.server

import io.ktor.server.application.*
import org.darthacheron.pantrypal.server.plugins.DynamicRateLimiter
import org.darthacheron.pantrypal.server.plugins.MaintenanceMode

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    configureFrameworks()
    install(MaintenanceMode)
    install(DynamicRateLimiter)
    configureHTTP()
    configureSecurity()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureSockets()
    configureAdministration()
    configureRouting()
    configureShutdown()
}
