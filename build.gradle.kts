plugins {
    id("com.android.application") version "9.0.0" apply false
    id("com.android.library") version "9.0.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
}

val versionName = "1.16.2"
val versionCode = 17

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
