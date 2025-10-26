package ru.hepolise.volumekeytrackcontrol.util

import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig
import java.io.File
import java.security.MessageDigest
import java.util.Locale

object StatusSysPropsHelper {

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
        val digest = md.digest("$input:${BuildConfig.APPLICATION_ID}".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun dynamicKey(): String {
        val suffix = readBootIdShort() ?: "noboot"
        return "sys.$suffix"
    }

    private var _isHooked: Boolean? = null
    val isHooked: Boolean
        get() = _isHooked ?: run {
            (SystemProps.get(dynamicKey()) == "1").also { _isHooked = it }
        }

    fun refreshIsHooked() {
        _isHooked = null
    }
}