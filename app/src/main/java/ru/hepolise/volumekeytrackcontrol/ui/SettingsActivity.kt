package ru.hepolise.volumekeytrackcontrol.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.hepolise.volumekeytrackcontrol.ui.component.ModuleIsNotEnabled
import ru.hepolise.volumekeytrackcontrol.ui.navigation.AppNavigation
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SETTINGS_PREFS_NAME
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.getVibrator


class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashscreen = installSplashScreen()
        var keepSplashScreen = true
        super.onCreate(savedInstanceState)
        splashscreen.setKeepOnScreenCondition { keepSplashScreen }
        keepSplashScreen = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = dynamicColorScheme(context = this)
            ) {
                val context = LocalContext.current

                @SuppressLint("WorldReadableFiles") @Suppress("DEPRECATION")
                val sharedPreferences = try {
                    context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_WORLD_READABLE)
                } catch (_: SecurityException) {
                    ModuleIsNotEnabled()
                    return@MaterialTheme
                }
                AppNavigation(sharedPreferences = sharedPreferences, vibrator = getVibrator())
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
