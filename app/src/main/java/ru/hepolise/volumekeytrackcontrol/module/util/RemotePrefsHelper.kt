package ru.hepolise.volumekeytrackcontrol.module.util

import android.content.Context
import android.content.SharedPreferences
import com.crossbowffs.remotepreferences.RemotePreferences
import ru.hepolise.volumekeytrackcontrol.BuildConfig
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil

object RemotePrefsHelper {
    fun withRemotePrefs(context: Context, block: SharedPreferences.() -> Unit) {
        val prefs = RemotePreferences(
            context,
            BuildConfig.APPLICATION_ID,
            SharedPreferencesUtil.STATUS_PREFS,
            true
        )
        block.invoke(prefs)
    }
}