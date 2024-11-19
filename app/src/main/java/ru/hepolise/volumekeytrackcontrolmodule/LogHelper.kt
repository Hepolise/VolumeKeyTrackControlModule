package ru.hepolise.volumekeytrackcontrolmodule

import de.robv.android.xposed.XposedBridge

object LogHelper {
    fun log(text: String) {
        if (BuildConfig.DEBUG) XposedBridge.log(text)
    }
}