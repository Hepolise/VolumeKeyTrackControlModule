package ru.hepolise.volumekeytrackcontrol.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationAmplitude
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationLength
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationType

object VibratorUtil {

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
        val vibrationType = prefs.getVibrationType()
        if (vibrationType == VibrationType.Disabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && vibrationType != VibrationType.Manual) {
            val vibe = VibrationEffect.createPredefined(vibrationType.value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val attributes = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    this.vibrate(vibe, attributes)
                } catch (_: Exception) {
                    this.vibrate(vibe)
                }
            } else {
                this.vibrate(vibe)
            }
        } else {
            this.vibrate(
                VibrationEffect.createOneShot(
                    prefs.getVibrationLength().toLong(),
                    prefs.getVibrationAmplitude()
                )
            )
        }
    }
}


sealed class VibrationType(val value: Int, val key: String) {
    data object Disabled : VibrationType(-1, "disabled")
    data object Manual : VibrationType(-1, "manual")

    companion object {
        val values: List<VibrationType> by lazy {
            mutableListOf<VibrationType>().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Click)
                    add(DoubleClick)
                    add(HeavyClick)
                    add(Tick)
                }

                add(Manual)
                add(Disabled)
            }
        }

        fun fromKey(key: String): VibrationType = values.single { it.key == key }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    data object Click : VibrationType(VibrationEffect.EFFECT_CLICK, "click")

    @RequiresApi(Build.VERSION_CODES.Q)
    data object DoubleClick : VibrationType(VibrationEffect.EFFECT_DOUBLE_CLICK, "double_click")

    @RequiresApi(Build.VERSION_CODES.Q)
    data object HeavyClick : VibrationType(VibrationEffect.EFFECT_HEAVY_CLICK, "heavy_click")

    @RequiresApi(Build.VERSION_CODES.Q)
    data object Tick : VibrationType(VibrationEffect.EFFECT_TICK, "tick")
}