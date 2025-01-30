plugins {
    id("com.android.application") version "8.8.0" apply false
    id("com.android.library") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
}

val versionName = "1.15.4"
val versionCode = 11

rootProject.ext.set("appVersionName", versionName)
rootProject.ext.set("appVersionCode", versionCode)

tasks.register("getVersion") {
    doLast {
        val versionFile = file("app/build/version.txt")
        versionFile.parentFile.mkdirs()
        if (!versionFile.exists()) {
            versionFile.createNewFile()
        }
        versionFile.writeText(versionName)
    }
}
