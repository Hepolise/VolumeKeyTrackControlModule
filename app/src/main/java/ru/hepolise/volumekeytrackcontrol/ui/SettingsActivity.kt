package ru.hepolise.volumekeytrackcontrol.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.hepolise.volumekeytrackcontrol.ui.navigation.AppNavigation
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.HOOK_PREFS_NAME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SETTINGS_PREFS_NAME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isHooked
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.getVibrator
import kotlin.system.exitProcess


class SettingsActivity : ComponentActivity() {

    @Volatile
    private var shouldRemoveFromRecents = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashscreen = installSplashScreen()
        var keepSplashScreen = true
        super.onCreate(savedInstanceState)
        splashscreen.setKeepOnScreenCondition { keepSplashScreen }
        keepSplashScreen = false
        setUpSplashScreenAnimation()
        enableEdgeToEdge()
        setContent {
            val prefs = tryLoadPrefs()
            MaterialTheme(colorScheme = dynamicColorScheme(context = this)) {
                AppNavigation(settingsPrefs = prefs, vibrator = getVibrator())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (shouldRemoveFromRecents) {
            exitProcess(0)
        }
    }

    private fun Context.tryLoadPrefs(): SharedPreferences? = try {
        isHooked = getSharedPreferences(HOOK_PREFS_NAME, MODE_PRIVATE).isHooked()
        shouldRemoveFromRecents = !isHooked
        @SuppressLint("WorldReadableFiles")
        @Suppress("DEPRECATION")
        getSharedPreferences(SETTINGS_PREFS_NAME, MODE_WORLD_READABLE)
    } catch (_: SecurityException) {
        shouldRemoveFromRecents = true
        null
    }

    private fun setUpSplashScreenAnimation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val scaleX = ObjectAnimator.ofFloat(splashScreenView, View.SCALE_X, 1f, 0.6f)
            val scaleY = ObjectAnimator.ofFloat(splashScreenView, View.SCALE_Y, 1f, 0.6f)

            val fadeOut = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f)

            scaleX.interpolator = AnticipateInterpolator()
            scaleY.interpolator = AnticipateInterpolator()
            fadeOut.interpolator = AccelerateInterpolator()

            val duration = 400L
            scaleX.duration = duration
            scaleY.duration = duration
            fadeOut.duration = duration

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, fadeOut)
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }
    }
}

@Composable
fun dynamicColorScheme(context: Context): ColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Dynamic colors for Android 12+
        if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    } else {
        if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    }
}
