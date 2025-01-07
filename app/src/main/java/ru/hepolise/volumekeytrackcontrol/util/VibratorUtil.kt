package ru.hepolise.volumekeytrackcontrol.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getSelectedEffect
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationLength
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationMode

object VibratorUtil {

    val PredefinedEffects = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(
            VibrationEffect.EFFECT_CLICK,
            VibrationEffect.EFFECT_DOUBLE_CLICK,
            VibrationEffect.EFFECT_HEAVY_CLICK,
            VibrationEffect.EFFECT_TICK
        )
    } else emptyList()

    fun Context.getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun Vibrator.triggerVibration(prefs: SharedPreferences? = SharedPreferencesUtil.prefs()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && prefs.getVibrationMode() == VibrationMode.PREDEFINED) {
            this.vibrate(
                VibrationEffect.createPredefined(PredefinedEffects[prefs.getSelectedEffect()])
            )
        } else {
            val millis = prefs.getVibrationLength()
            this.vibrate(
                VibrationEffect.createOneShot(
                    millis,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

}