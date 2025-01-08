package ru.hepolise.volumekeytrackcontrol.module.util

import de.robv.android.xposed.XposedBridge
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

object LogHelper {
    fun log(prefix: String, text: String) {
        if (BuildConfig.DEBUG) XposedBridge.log("[$prefix] $text")
    }
}