package ru.hepolise.volumekeytrackcontrol.util

import ru.hepolise.volumekeytrackcontrol.module.util.LogHelper

@Suppress("PrivateApi")
object SystemProps {
    private fun log(text: String) =
        LogHelper.log(SystemProps::class.java.simpleName, text)

    private val clazz by lazy { Class.forName("android.os.SystemProperties") }
    private val getString by lazy { clazz.getMethod("get", String::class.java, String::class.java) }
    private val setString by lazy { clazz.getMethod("set", String::class.java, String::class.java) }

    fun get(key: String, def: String = ""): String {
        return try {
            getString.invoke(null, key, def) as String
        } catch (_: Throwable) {
            def
        }
    }

    fun set(key: String, value: String) {
        try {
            setString.invoke(null, key, value)
            log("set $key to $value")
        } catch (t: Throwable) {
            log("set $key to $value failed: ${t.message}")
        }
    }
}