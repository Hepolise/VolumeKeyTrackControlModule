plugins {
    id("com.android.application") version "9.5.1" apply false
    id("com.android.library") version "9.4.1" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}

val versionName = "2.17.1"
val versionCode = 21

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
