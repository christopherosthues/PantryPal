import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

fun properties(key: String) = providers.gradleProperty(key)

val appVersionName = project.property("appVersionName") as String

kotlin {
    dependencies {
        implementation(projects.shared)
        implementation(libs.compose.components.resources)
        implementation(libs.filekit.core)
        implementation(compose.desktop.currentOs)
        implementation(libs.kotlinx.coroutinesSwing)
        testImplementation(kotlin("test"))
    }

    jvmToolchain(25)
    compilerOptions {
        version = JavaVersion.VERSION_25
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "org.darthacheron.pantrypal.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = properties("appName").get()
            packageVersion = appVersionName
        }
    }
}