plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "ru.hepolise.volumekeymusicmanagermodule"
        minSdk = 27
        targetSdk = 37
        versionCode = rootProject.ext["appVersionCode"].toString().toInt()
        versionName = rootProject.ext["appVersionName"].toString()
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    namespace = "ru.hepolise.volumekeytrackcontrol"
    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.4.0"))
    implementation("androidx.core:core-ktx:1.19.0")

    // Compose BOM (Bill of Materials)
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))

    // Compose dependencies
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.core:core-splashscreen:1.2.0")

    // Compose navigation
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Coil
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Required for preview support
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Xposed Framework API dependencies
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")

    // RemotePreferences
    implementation("com.crossbowffs.remotepreferences:remotepreferences:0.8")
}
