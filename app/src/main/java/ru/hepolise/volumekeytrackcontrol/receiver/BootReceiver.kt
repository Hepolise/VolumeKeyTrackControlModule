package ru.hepolise.volumekeytrackcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.util.LSPosedLogger
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LAST_BOOT_COMPLETED_TIME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getStatusSharedPreferences


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            LSPosedLogger.log("Setting last boot completed time from receiver")
            context.getStatusSharedPreferences().edit {
                putLong(LAST_BOOT_COMPLETED_TIME, System.currentTimeMillis())
            }
        }
    }
}