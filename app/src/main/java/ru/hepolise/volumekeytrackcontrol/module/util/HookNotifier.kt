package ru.hepolise.volumekeytrackcontrol.module.util

import android.content.Context
import androidx.core.net.toUri
import ru.hepolise.volumekeytrackcontrol.util.Constants
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

object HookNotifier {

    private val authorityUri = "content://${BuildConfig.APPLICATION_ID}.hookstatusprovider".toUri()

    fun notifyHooked(context: Context) {
        runCatching {
            context.contentResolver.call(authorityUri, Constants.SET_HOOKED, null, null)
        }
    }

    fun incrementLaunchCount(context: Context) {
        runCatching {
            context.contentResolver.call(authorityUri, Constants.INCREMENT_LAUNCH_COUNT, null, null)
        }
    }

}