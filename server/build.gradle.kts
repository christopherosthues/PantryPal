//val h2_version: String by project
//val koin_version: String by project
//val kotlin_version: String by project
//val ktor_version: String by project
//val logback_version: String by project
//val postgres_version: String by project

plugins {
    alias(libs.plugins.kotlinJvm)
//    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ktor)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    application
}

group = "org.darchacheron.pantrypal.server"
version = "1.0.0"

application {
    mainClass = "io.ktor.server.cio.EngineMain"
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(libs.opentelemetry.autoconfigure)
    api(libs.opentelemetry.semconv)
    api(libs.opentelemetry.exporter.otlp)
    api(libs.opentelemetry.ktor)

    implementation(libs.openfolder.asyncapi.ktor)
    implementation(libs.ucasoft.ktor.simple.cache)
    implementation(libs.ucasoft.ktor.simple.memory.cache)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.datetime)
    implementation(libs.postgresql)
    implementation(libs.flaxoos.ktor.server.rate.limiting)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.caching.headers)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.routing.openapi)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.csrf)
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.metrics)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.logback.classic)
    implementation(libs.jwks.rsa)
    implementation(projects.shared)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.testJunit)
}
