pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://api.xposed.info/")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "VolumeKeyTrackControlModule"
include(":app")
