package ru.hepolise.volumekeytrackcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.util.Constants
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.HOOK_PREFS_NAME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LAST_INIT_HOOK_TIME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LAUNCHED_COUNT
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLaunchedCount

class HookBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.HOOK_UPDATE) {
            val prefs = context.getSharedPreferences(HOOK_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                if (intent.getBooleanExtra(Constants.HOOKED, false)) {
                    putLong(LAST_INIT_HOOK_TIME, System.currentTimeMillis())
                }
                if (intent.getBooleanExtra(Constants.INCREMENT_LAUNCH_COUNT, false)) {
                    val current = prefs.getLaunchedCount()
                    putInt(LAUNCHED_COUNT, current + 1)
                }
            }
        }
    }
}

