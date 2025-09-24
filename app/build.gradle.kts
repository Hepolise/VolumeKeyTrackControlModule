import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.hepolise.volumekeymusicmanagermodule"
        minSdk = 27
        targetSdk = 36
        versionCode = rootProject.ext["appVersionCode"].toString().toInt()
        versionName = rootProject.ext["appVersionName"].toString()
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    namespace = "ru.hepolise.volumekeytrackcontrolmodule"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.2.20"))
    implementation("androidx.core:core-ktx:1.17.0")

    // Compose BOM (Bill of Materials)
    implementation(platform("androidx.compose:compose-bom:2025.09.01"))

    // Compose dependencies
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Compose navigation
    implementation("androidx.navigation:navigation-compose:2.9.5")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Required for preview support
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Xposed Framework API dependencies
    compileOnly("de.robv.android.xposed:api:82")
}
