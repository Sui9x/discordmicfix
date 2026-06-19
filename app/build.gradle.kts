plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sui.discordmicfix"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sui.discordmicfix"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation("com.google.android.material:material:1.12.0")
}
