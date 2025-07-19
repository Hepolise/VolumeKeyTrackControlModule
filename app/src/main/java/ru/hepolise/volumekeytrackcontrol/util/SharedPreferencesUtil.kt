package ru.hepolise.volumekeytrackcontrol.util

import android.content.SharedPreferences
import android.os.Build
import android.view.ViewConfiguration
import de.robv.android.xposed.XSharedPreferences
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig
import ru.hepolise.volumekeytrackcontrolmodule.R

object SharedPreferencesUtil {
    const val SETTINGS_PREFS_NAME = "settings_prefs"

    const val EFFECT = "selectedEffect"
    const val VIBRATION_LENGTH = "vibrationLength"
    const val VIBRATION_AMPLITUDE = "vibrationAmplitude"
    const val LONG_PRESS_DURATION = "longPressDuration"
    const val IS_SWAP_BUTTONS = "isSwapButtons"
    const val APP_FILTER_TYPE = "appFilterType"
    const val WHITE_LIST_APPS = "whiteListApps"
    const val BLACK_LIST_APPS = "blackListApps"

    val EFFECT_DEFAULT_VALUE =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationType.Click.key else VibrationType.Manual.key
    const val VIBRATION_LENGTH_DEFAULT_VALUE = 50
    const val VIBRATION_AMPLITUDE_DEFAULT_VALUE = 128
    val LONG_PRESS_DURATION_DEFAULT_VALUE = ViewConfiguration.getLongPressTimeout()
    const val IS_SWAP_BUTTONS_DEFAULT_VALUE = false
    val APP_FILTER_TYPE_DEFAULT_VALUE = AppFilterType.DISABLED.key

    fun SharedPreferences?.getVibrationType(): VibrationType {
        val defaultValue = EFFECT_DEFAULT_VALUE
        return VibrationType.fromKey(this?.getString(EFFECT, defaultValue) ?: defaultValue)
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

    fun SharedPreferences?.getAppFilterType(): AppFilterType {
        val defaultValue = APP_FILTER_TYPE_DEFAULT_VALUE
        return AppFilterType.fromKey(this?.getString(APP_FILTER_TYPE, defaultValue) ?: defaultValue)
    }

    fun SharedPreferences?.getApps(appFilterType: AppFilterType = getAppFilterType()): Set<String> {
        return when (appFilterType) {
            AppFilterType.DISABLED -> emptySet()
            AppFilterType.WHITE_LIST -> this?.getStringSet(WHITE_LIST_APPS, emptySet())
                ?: emptySet()

            AppFilterType.BLACK_LIST -> this?.getStringSet(BLACK_LIST_APPS, emptySet())
                ?: emptySet()
        }
    }


    fun prefs(): SharedPreferences? {
        val pref = XSharedPreferences(BuildConfig.APPLICATION_ID, SETTINGS_PREFS_NAME)
        return if (pref.file.canRead()) pref else null
    }

    enum class AppFilterType(
        val value: Int,
        val key: String,
        val resourceId: Int
    ) {
        DISABLED(0, "disabled", R.string.app_filter_disabled),
        WHITE_LIST(1, "whitelist", R.string.app_filter_white_list),
        BLACK_LIST(2, "blacklist", R.string.app_filter_black_list);

        companion object {
            fun fromKey(key: String) = entries.find { it.key == key } ?: DISABLED
        }
    }

}