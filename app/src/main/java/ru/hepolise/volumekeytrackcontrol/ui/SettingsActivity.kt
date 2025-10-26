package ru.hepolise.volumekeytrackcontrol.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ru.hepolise.volumekeytrackcontrol.ui.navigation.AppNavigation
import ru.hepolise.volumekeytrackcontrol.util.LSPosedLogger
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getSettingsSharedPreferences
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getStatusSharedPreferences
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isHooked
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.getVibrator
import ru.hepolise.volumekeytrackcontrol.viewmodel.BootViewModel
import ru.hepolise.volumekeytrackcontrol.viewmodel.BootViewModelFactory
import kotlin.system.exitProcess


class SettingsActivity : ComponentActivity() {

    private val bootViewModel: BootViewModel by viewModels {
        BootViewModelFactory(applicationContext)
    }

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
            val hookPrefs = getStatusSharedPreferences()
            val prefs = getSettingsSharedPreferences()

            val isLoading by bootViewModel.isLoading.collectAsState()

            LaunchedEffect(hookPrefs, prefs, isLoading) {
                LSPosedLogger.log("Updating shouldRemoveFromRecents")
                shouldRemoveFromRecents = !hookPrefs.isHooked() || prefs == null
            }

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
