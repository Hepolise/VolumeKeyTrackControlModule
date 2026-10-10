package ru.hepolise.volumekeytrackcontrol.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import io.github.libxposed.service.HotReloadResult
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.hepolise.volumekeytrackcontrol.App
import ru.hepolise.volumekeytrackcontrol.ui.component.MODULE_SCOPE
import ru.hepolise.volumekeytrackcontrol.ui.navigation.AppNavigation
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getSettingsSharedPreferences
import kotlin.system.exitProcess


class SettingsActivity : ComponentActivity(), App.ServiceStateListener {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private val _xposedService = MutableStateFlow<XposedService?>(null)
    private val xposedService = _xposedService.asStateFlow()

    private val _hotReloadResultStatus = MutableStateFlow<HotReloadResult.Status?>(null)
    private val hotReloadResultStatus = _hotReloadResultStatus.asStateFlow()

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
            val result by hotReloadResultStatus.collectAsState()
            val scope by moduleScope.collectAsState()
            val prefs = xposed?.getSettingsSharedPreferences()

            fun computeIsHooked(): Boolean {
                val hooked = xposed != null
                        && prefs != null
                        && result != null && result != HotReloadResult.Status.FAILED
                        && scope.orEmpty().contains(MODULE_SCOPE)
                Log.d(
                    TAG,
                    "computeIsHooked: xposed=${xposed != null}, prefs=${prefs != null}, " +
                            "result=$result, scope=${scope.orEmpty()}, MODULE_SCOPE=$MODULE_SCOPE -> isHooked=$hooked"
                )
                return hooked
            }

            val isHooked = remember(result, xposed, prefs, scope) {
                Log.d(TAG, "remember recompute: result=$result, xposed=${xposed != null}, prefs=${prefs != null}, scope=$scope")
                computeIsHooked()
            }

            CompositionLocalProvider(
                LocalXposedService provides xposed,
                LocalHotReloadResult provides result,
                LocalModuleScope provides scope
            ) {
                MaterialTheme(colorScheme = dynamicColorScheme(context = this)) {
                    AppNavigation(isHooked) {
                        Log.d(TAG, "retry callback: updateModuleScope + updateHotReloadResult")
                        xposed.updateModuleScope()
                        xposed.updateHotReloadResult()
                    }
                }
            }

            LaunchedEffect(isHooked) {
                Log.d(TAG, "LaunchedEffect(isHooked=$isHooked): shouldRemoveFromRecents=${!isHooked}")
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
        Log.d(TAG, "onServiceStateChanged: service=$service")
        _xposedService.value = service
        service.updateModuleScope()
        service.updateHotReloadResult()
    }

    private fun XposedService?.updateHotReloadResult() {
        Log.d(TAG, "updateHotReloadResult: service=$this, scope=${this?.scope}, runningTargets=${this?.runningTargets}")

        fun fallback(reason: String) {
            Log.d(TAG, "updateHotReloadResult fallback -> FAILED: $reason")
            _hotReloadResultStatus.value = HotReloadResult.Status.FAILED
        }

        val service = this ?: run {
            fallback("service is null")
            return
        }

        if (!service.scope.contains(MODULE_SCOPE)) {
            fallback("scope does not contain MODULE_SCOPE; scope=${service.scope}")
            return
        }

        service.runningTargets.takeIf { it.isNotEmpty() }?.forEach { target ->
            Log.d(TAG, "hotReloadModule: target=$target")
            service.hotReloadModule(target, null) { _, result ->
                Log.d(TAG, "hotReloadModule callback: target=$target, status=${result.status}")
                _hotReloadResultStatus.value = result.status
            }
        } ?: fallback("runningTargets is empty")
    }

    private fun XposedService?.updateModuleScope() {
        Log.d(TAG, "updateModuleScope: scope=${this?.scope}")
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
