package ru.hepolise.volumekeytrackcontrol.util

import android.content.SharedPreferences
import android.view.ViewConfiguration
import de.robv.android.xposed.XSharedPreferences
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

object SharedPreferencesUtil {
    const val SETTINGS_PREFS_NAME = "settings_prefs"

    const val SELECTED_EFFECT = "selectedEffect"
    const val VIBRATION_LENGTH = "vibrationLength"
    const val LONG_PRESS_DURATION = "longPressDuration"

    const val SELECTED_EFFECT_DEFAULT_VALUE = 0
    const val VIBRATION_LENGTH_DEFAULT_VALUE = 50L
    val LONG_PRESS_DURATION_DEFAULT_VALUE = ViewConfiguration.getLongPressTimeout().toLong()

    fun SharedPreferences?.getSelectedEffect(): Int {
        val defaultValue = SELECTED_EFFECT_DEFAULT_VALUE
        return this?.getInt(SELECTED_EFFECT, defaultValue) ?: defaultValue
    }

    fun SharedPreferences?.getVibrationType(): VibrationType {
        return VibrationType.values[getSelectedEffect()]
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