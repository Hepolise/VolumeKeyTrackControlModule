package ru.hepolise.volumekeytrackcontrol.module.util

import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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
        } catch (t: Throwable) {
            log("get $key failed: ${t.message}")
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

    class Delegate<T>(
        private val key: String,
        private val default: T
    ) : ReadWriteProperty<Any?, T> {
        companion object {
            private fun readBootIdShort(): String? {
                try {
                    val f = File("/proc/sys/kernel/random/boot_id")
                    if (f.exists()) {
                        val id = f.readText().trim()
                        if (id.isNotEmpty()) {
                            return sha256(id).substring(0, 8).lowercase(Locale.US)
                        }
                    }
                } catch (_: Throwable) { /* ignore */
                }
                return null
            }

            private fun sha256(input: String): String {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(input.toByteArray(Charsets.UTF_8))
                return digest.joinToString("") { "%02x".format(it) }
            }
        }

        private fun dynamicKey(): String {
            val base = key.trim().removePrefix(".")
            val suffix = readBootIdShort() ?: "noboot"
            return "sys.$base.$suffix".also { log("dynamicKey: $it") }
        }

        @Suppress("UNCHECKED_CAST")
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val value = SystemProps.get(dynamicKey(), default.toString())
            return when (default) {
                is Boolean -> (value == "1" || value.equals("true", ignoreCase = true)) as T
                is Int -> value.toIntOrNull() as? T ?: default
                else -> value as T
            }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val str = when (value) {
                is Boolean -> if (value) "1" else "0"
                else -> value.toString()
            }
            SystemProps.set(dynamicKey(), str)
        }
    }
}