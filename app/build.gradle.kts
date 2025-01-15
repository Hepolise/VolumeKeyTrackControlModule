plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.hepolise.volumekeymusicmanagermodule"
        minSdk = 27
        targetSdk = 35
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

    kotlinOptions {
        jvmTarget = "17"
    }

    namespace = "ru.hepolise.volumekeytrackcontrolmodule"
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.1.0"))
    implementation("androidx.core:core-ktx:1.15.0")

    // Compose BOM (Bill of Materials)
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))

    // Compose dependencies
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")

    // Required for preview support
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Xposed Framework API dependencies
    compileOnly("de.robv.android.xposed:api:82")
}
