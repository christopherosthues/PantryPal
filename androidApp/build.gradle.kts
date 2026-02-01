import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val appVersionName = project.property("appVersionName") as String

kotlin {
    dependencies {
        implementation(projects.composeApp)
        implementation(libs.compose.uiToolingPreview)
        implementation(libs.androidx.activity.compose)

        implementation(libs.koin.android)
        implementation(libs.koin.androidx.compose)
//            implementation(libs.koin.androidx.compose.navigation)
    }

    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }
}

android {
    namespace = "org.darchacheron.pantrypal"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.darchacheron.pantrypal"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = project.property("appVersionCode").toString().toInt()
        versionName = appVersionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}