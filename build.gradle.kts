plugins {
    id("com.android.application") version "8.9.2" apply false
    id("com.android.library") version "8.10.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}

val versionName = "1.15.6"
val versionCode = 13

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
