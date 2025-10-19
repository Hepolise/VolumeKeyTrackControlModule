package ru.hepolise.volumekeytrackcontrol.module.util

import android.content.Context
import android.content.SharedPreferences
import com.crossbowffs.remotepreferences.RemotePreferences
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

object RemotePrefsHelper {
    private fun log(text: String) = LogHelper.log(RemotePrefsHelper::class.java.simpleName, text)

    fun withRemotePrefs(context: Context, block: SharedPreferences.() -> Unit) {
        try {
            val prefs = RemotePreferences(
                context,
                BuildConfig.APPLICATION_ID,
                SharedPreferencesUtil.STATUS_PREFS,
                true
            )
            block.invoke(prefs)
        } catch (e: Exception) {
            log("remote preferences failed")
            log(e.message ?: "unknown")
        }
    }
}