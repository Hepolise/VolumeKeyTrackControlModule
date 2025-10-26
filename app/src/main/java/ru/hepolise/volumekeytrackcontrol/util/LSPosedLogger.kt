package ru.hepolise.volumekeytrackcontrol.util

import android.util.Log
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

object LSPosedLogger {
    fun log(text: String) {
        if (BuildConfig.DEBUG) Log.d("LSPosed-Bridge", text)
    }
}