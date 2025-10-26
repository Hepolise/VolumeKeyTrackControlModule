package ru.hepolise.volumekeytrackcontrol.module.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LAST_INIT_HOOK_TIME
import ru.hepolise.volumekeytrackcontrol.util.StatusSysPropsHelper.dynamicKey
import ru.hepolise.volumekeytrackcontrol.util.SystemProps

object StatusHelper {
    private fun log(text: String) = LogHelper.log(StatusHelper::class.java.simpleName, text)

    fun handleSuccessHook(context: Context) {
        val eventsLock = Any()
        var bootReceived = false
        var unlockReceived = false
        var anySuccess = false
        var remotePrefsSuccess = false

        fun handleEvent(ctx: Context, action: String) {
            synchronized(eventsLock) {
                if (!remotePrefsSuccess) {
                    try {
                        RemotePrefsHelper.withRemotePrefs(ctx) {
                            edit {
                                putLong(LAST_INIT_HOOK_TIME, System.currentTimeMillis())
                            }
                        }
                        remotePrefsSuccess = true
                        log("Remote prefs updated successfully for $action")
                    } catch (t: Throwable) {
                        log("Remote preferences failed for $action (${t.message})")
                    }
                }

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED -> bootReceived = true
                    Intent.ACTION_USER_UNLOCKED -> unlockReceived = true
                }
                anySuccess = anySuccess || remotePrefsSuccess

                if (bootReceived && unlockReceived) {
                    if (!anySuccess) {
                        log("Neither event could connect to content-provider, writing to sysprops")
                        try {
                            SystemProps.set(dynamicKey(), "1")
                        } catch (t: Throwable) {
                            log("Failed to write to sysprops (${t.message})")
                        }
                    }
                }
            }
        }

        listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_USER_UNLOCKED).forEach { intentAction ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val action = intent.action.also { log("onReceive: $it") } ?: return
                    handleEvent(ctx, action)
                    ctx.unregisterReceiver(this)
                }
            }
            context.registerReceiver(receiver, IntentFilter(intentAction))
        }
    }
}