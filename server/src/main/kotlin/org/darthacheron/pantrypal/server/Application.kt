package org.darthacheron.pantrypal.server

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSecurity()
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureFrameworks()
    configureSockets()
    configureAdministration()
    configureRouting()
}
