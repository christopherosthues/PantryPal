import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.jetbrains.kotlin.serialization)
//    alias(libs.plugins.ksp)
//    alias(libs.plugins.room)
}

val appVersionName = project.property("appVersionName") as String

kotlin {
    androidLibrary {
        namespace = "compose.org.darchacheron.pantrypal"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

//        withJava()
        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
//        withDeviceTest {
//            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//            execution = "HOST"
//        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }

        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }

//    js {
//        browser()
//        binaries.executable()
//    }
//
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs {
//        browser()
//        binaries.executable()
//    }

//    room {
//        schemaDirectory("$projectDir/schemas")
//    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
//            implementation(libs.androidx.room.runtime)
//            implementation(libs.sqlite.bundled)

//            api(libs.androidx.datastore)
//            api(libs.androidx.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)

//            implementation(libs.kotlinx.coroutines.test)
//            implementation(libs.androidx.room.testing)
//            implementation(libs.turbine.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)

//    add("kspAndroid", libs.androidx.room.compiler)
////    add("kspIosX64", libs.androidx.room.compiler)
//    add("kspIosArm64", libs.androidx.room.compiler)
//    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
//    add("kspJvm", libs.androidx.room.compiler)
////    add("kspJs", libs.androidx.room.compiler)
////    add("kspWasmJs", libs.androidx.room.compiler)
}

compose.desktop {
    application {
        mainClass = "org.darchacheron.pantrypal.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.darchacheron.pantrypal"
            packageVersion = appVersionName
        }
    }
}
