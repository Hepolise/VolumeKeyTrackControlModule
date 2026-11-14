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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.hepolise.volumekeytrackcontrol.App
import ru.hepolise.volumekeytrackcontrol.ui.component.MODULE_SCOPE
import ru.hepolise.volumekeytrackcontrol.ui.navigation.AppNavigation
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getSettingsSharedPreferences
import kotlin.system.exitProcess


class SettingsActivity : ComponentActivity(), App.ServiceStateListener {

    private val _xposedService = MutableStateFlow<XposedService?>(null)
    private val xposedService = _xposedService.asStateFlow()

    private val _isModuleRunning = MutableStateFlow<Boolean?>(null)
    private val isModuleRunning = _isModuleRunning.asStateFlow()

    private val _moduleScope = MutableStateFlow<List<String>?>(null)
    private val moduleScope = _moduleScope.asStateFlow()

    override fun onStart() {
        super.onStart()
        App.addServiceStateListener(this, true)
    }

    override fun onStop() {
        App.removeServiceStateListener(this)
        super.onStop()
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
            val xposed by xposedService.collectAsState()
            val running by isModuleRunning.collectAsState()
            val scope by moduleScope.collectAsState()
            val prefs = xposed?.getSettingsSharedPreferences()

            fun computeIsHooked() = xposed != null
                    && prefs != null
                    && running == true
                    && scope.orEmpty().contains(MODULE_SCOPE)

            val isHooked = remember(running, xposed, prefs, scope) {
                computeIsHooked()
            }

            CompositionLocalProvider(
                LocalXposedService provides xposed,
                LocalModuleRunning provides running,
                LocalModuleScope provides scope
            ) {
                MaterialTheme(colorScheme = dynamicColorScheme(context = this)) {
                    AppNavigation(isHooked) {
                        xposed.updateModuleScope()
                        xposed.updateModuleRunning()
                    }
                }
            }

            LaunchedEffect(isHooked) {
                shouldRemoveFromRecents = !isHooked
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (shouldRemoveFromRecents) {
            exitProcess(0)
        }
    }

    override fun onServiceStateChanged(service: XposedService?) {
        _xposedService.value = service
        service.updateModuleScope()
        service.updateModuleRunning()
    }

    private fun XposedService?.updateModuleRunning() {
        val service = this
        _isModuleRunning.value = service != null
                && service.scope.contains(MODULE_SCOPE)
                && runCatching { service.runningTargets.isNotEmpty() }.getOrDefault(false)
    }

    private fun XposedService?.updateModuleScope() {
        _moduleScope.value = this?.scope
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
