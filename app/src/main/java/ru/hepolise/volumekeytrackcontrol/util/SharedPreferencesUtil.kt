package ru.hepolise.volumekeytrackcontrol.util

import android.content.SharedPreferences
import android.os.Build
import android.view.ViewConfiguration
import de.robv.android.xposed.XSharedPreferences
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

object SharedPreferencesUtil {
    const val SETTINGS_PREFS_NAME = "settings_prefs"

    const val SELECTED_EFFECT = "selectedEffect"
    const val VIBRATION_LENGTH = "vibrationLength"
    const val VIBRATION_AMPLITUDE = "vibrationAmplitude"
    const val LONG_PRESS_DURATION = "longPressDuration"
    const val IS_SWAP_BUTTONS = "isSwapButtons"

    val SELECTED_EFFECT_DEFAULT_VALUE =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationType.Click.key else VibrationType.Manual.key
    const val VIBRATION_LENGTH_DEFAULT_VALUE = 50
    const val VIBRATION_AMPLITUDE_DEFAULT_VALUE = 128
    val LONG_PRESS_DURATION_DEFAULT_VALUE = ViewConfiguration.getLongPressTimeout()
    const val IS_SWAP_BUTTONS_DEFAULT_VALUE = false

    fun SharedPreferences?.getVibrationType(): VibrationType {
        val defaultValue = SELECTED_EFFECT_DEFAULT_VALUE
        return VibrationType.fromKey(this?.getString(SELECTED_EFFECT, defaultValue) ?: defaultValue)
    }

    fun SharedPreferences?.getVibrationLength(): Int {
        val defaultValue = VIBRATION_LENGTH_DEFAULT_VALUE
        return this?.getInt(VIBRATION_LENGTH, defaultValue) ?: defaultValue
    }

    fun SharedPreferences?.getVibrationAmplitude(): Int {
        val defaultValue = VIBRATION_AMPLITUDE_DEFAULT_VALUE
        return this?.getInt(VIBRATION_AMPLITUDE, defaultValue) ?: defaultValue
    }

    fun SharedPreferences?.getLongPressDuration(): Int {
        val defaultValue = LONG_PRESS_DURATION_DEFAULT_VALUE
        return this?.getInt(LONG_PRESS_DURATION, defaultValue) ?: defaultValue
    }

    fun SharedPreferences?.isSwapButtons(): Boolean {
        val defaultValue = IS_SWAP_BUTTONS_DEFAULT_VALUE
        return this?.getBoolean(IS_SWAP_BUTTONS, defaultValue) ?: defaultValue
    }

    fun prefs(): SharedPreferences? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, SETTINGS_PREFS_NAME)
        return if (pref.file.canRead()) pref else null
    }

}