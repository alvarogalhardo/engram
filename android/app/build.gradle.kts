plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val buildVersionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1

android {
    namespace = "com.alvarogalhardo.engram"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.alvarogalhardo.engram"
        minSdk = 33
        targetSdk = 35
        versionCode = buildVersionCode
        versionName = "1.0.$buildVersionCode"
    }

    // Keystore NUNCA entra no repo: o CI de release injeta via secrets
    // (ENGRAM_KEYSTORE_*); sem as variáveis, release usa assinatura de debug.
    val keystorePath: String? = System.getenv("ENGRAM_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ENGRAM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ENGRAM_KEY_ALIAS") ?: "engram"
                keyPassword = System.getenv("ENGRAM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.webkit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.commonmark)
    implementation(libs.commonmark.gfm.tables)
    testImplementation(libs.junit)
}
