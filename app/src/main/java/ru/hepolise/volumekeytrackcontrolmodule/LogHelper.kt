package ru.hepolise.volumekeytrackcontrolmodule

import de.robv.android.xposed.XposedBridge

object LogHelper {
    fun log(prefix: String, text: String) {
        if (BuildConfig.DEBUG) XposedBridge.log("[$prefix] $text")
    }
}