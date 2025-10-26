package ru.hepolise.volumekeytrackcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.hepolise.volumekeytrackcontrol.repository.BootRepository
import ru.hepolise.volumekeytrackcontrol.util.LSPosedLogger


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            LSPosedLogger.log("Setting last boot completed time from receiver")
            BootRepository.getBootRepository(context).setBootCompleted()
        }
    }
}