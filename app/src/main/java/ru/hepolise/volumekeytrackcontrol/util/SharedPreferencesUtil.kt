package ru.hepolise.volumekeytrackcontrol.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Context.MODE_WORLD_READABLE
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.view.ViewConfiguration
import de.robv.android.xposed.XSharedPreferences
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

object SharedPreferencesUtil {
    const val SETTINGS_PREFS = "settings_prefs"
    const val STATUS_PREFS = "status_prefs"

    const val EFFECT = "selectedEffect"
    const val VIBRATION_LENGTH = "vibrationLength"
    const val VIBRATION_AMPLITUDE = "vibrationAmplitude"
    const val LONG_PRESS_DURATION = "longPressDuration"
    const val IS_SWAP_BUTTONS = "isSwapButtons"
    const val APP_FILTER_TYPE = "appFilterType"
    const val WHITE_LIST_APPS = "whiteListApps"
    const val BLACK_LIST_APPS = "blackListApps"

    const val LAST_INIT_HOOK_TIME = "lastInitHookTime"
    const val LAUNCHED_COUNT = "launchedCount"
    const val LAST_BOOT_COMPLETED_TIME = "lastBootCompletedTime"

    val EFFECT_DEFAULT_VALUE =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationType.Click.key else VibrationType.Manual.key
    const val VIBRATION_LENGTH_DEFAULT_VALUE = 50
    const val VIBRATION_AMPLITUDE_DEFAULT_VALUE = 128
    val LONG_PRESS_DURATION_DEFAULT_VALUE = ViewConfiguration.getLongPressTimeout()
    const val IS_SWAP_BUTTONS_DEFAULT_VALUE = false
    val APP_FILTER_TYPE_DEFAULT_VALUE = AppFilterType.DISABLED.key

    const val LAUNCHED_COUNT_DEFAULT_VALUE = 0

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

    fun SharedPreferences.isHooked(): Boolean {
        val hookTime by lazy {
            getLong(LAST_INIT_HOOK_TIME, 0L)
        }
        return StatusSysPropsHelper.isHooked
                || hookTime >= (System.currentTimeMillis() - SystemClock.elapsedRealtime())
    }

    fun SharedPreferences.isBootCompleted() =
        getLong(
            LAST_BOOT_COMPLETED_TIME,
            0L
        ) >= (System.currentTimeMillis() - SystemClock.elapsedRealtime())

    fun SharedPreferences.getLaunchedCount(): Int =
        this.getInt(LAUNCHED_COUNT, LAUNCHED_COUNT_DEFAULT_VALUE)

    private var _prefs: SharedPreferences? = null

    fun prefs(): SharedPreferences? =
        XSharedPreferences(BuildConfig.APPLICATION_ID, SETTINGS_PREFS)
            .takeIf { it.file.canRead() }
            ?.also { _prefs = it } ?: _prefs

    fun Context.getSettingsSharedPreferences(): SharedPreferences? = runCatching {
        @SuppressLint("WorldReadableFiles")
        @Suppress("DEPRECATION")
        return getSharedPreferences(SETTINGS_PREFS, MODE_WORLD_READABLE)
    }.getOrNull()

    fun Context.getStatusSharedPreferences(): SharedPreferences =
        getSharedPreferences(STATUS_PREFS, MODE_PRIVATE)

}