package ru.hepolise.volumekeytrackcontrol.util

import android.content.SharedPreferences
import android.os.Build
import android.view.ViewConfiguration
import de.robv.android.xposed.XSharedPreferences
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

object SharedPreferencesUtil {
    const val SETTINGS_PREFS_NAME = "settings_prefs"

    const val VIBRATION_MODE = "vibrationMode"
    const val SELECTED_EFFECT = "selectedEffect"
    const val VIBRATION_LENGTH = "vibrationLength"
    const val LONG_PRESS_DURATION = "longPressDuration"

    val VIBRATION_PREDEFINED_MODE_DEFAULT_VALUE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    const val SELECTED_EFFECT_DEFAULT_VALUE = 0
    const val VIBRATION_LENGTH_DEFAULT_VALUE = 50L
    val LONG_PRESS_DURATION_DEFAULT_VALUE = ViewConfiguration.getLongPressTimeout().toLong()

    fun SharedPreferences?.isVibrationModePredefined(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val defaultValue = VIBRATION_PREDEFINED_MODE_DEFAULT_VALUE
        return this?.getBoolean(VIBRATION_MODE, defaultValue) ?: defaultValue
    }

    fun SharedPreferences?.getSelectedEffect(): Int {
        val defaultValue = SELECTED_EFFECT_DEFAULT_VALUE
        return this?.getInt(SELECTED_EFFECT, defaultValue) ?: defaultValue
    }

    fun SharedPreferences?.getVibrationLength(): Long {
        val defaultValue = VIBRATION_LENGTH_DEFAULT_VALUE
        return this?.getLong(VIBRATION_LENGTH, defaultValue) ?: defaultValue
    }

    fun SharedPreferences?.getLongPressDuration(): Long {
        val defaultValue = LONG_PRESS_DURATION_DEFAULT_VALUE
        return this?.getLong(LONG_PRESS_DURATION, defaultValue) ?: defaultValue
    }

    fun prefs(): SharedPreferences? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, SETTINGS_PREFS_NAME)
        return if (pref.file.canRead()) pref else null
    }

}