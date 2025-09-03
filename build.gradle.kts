plugins {
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.12.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}

val versionName = "1.15.7"
val versionCode = 14

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
