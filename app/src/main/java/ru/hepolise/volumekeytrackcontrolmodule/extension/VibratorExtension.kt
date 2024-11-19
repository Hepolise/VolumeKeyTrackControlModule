package ru.hepolise.volumekeytrackcontrolmodule.extension

import android.annotation.SuppressLint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object VibratorExtension {

    @SuppressLint("MissingPermission")
    fun Vibrator.triggerVibration() {
        val millis = 50L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.vibrate(
                VibrationEffect.createOneShot(
                    millis,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("deprecation")
            this.vibrate(millis) // Deprecated in API 26 but still works for lower versions
        }
    }

}