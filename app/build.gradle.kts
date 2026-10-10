plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "it.fabio.musilab"
    compileSdk = 35

    defaultConfig {
        applicationId = "it.fabio.musilab"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "0.3"
    }

    // Chiave fissa SOLO per le prove di laboratorio (non è segreta):
    // serve perché ogni nuova versione si installi sopra la precedente.
    signingConfigs {
        getByName("debug") {
            storeFile = file("lab.keystore")
            storePassword = "android"
            keyAlias = "lab"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
}
