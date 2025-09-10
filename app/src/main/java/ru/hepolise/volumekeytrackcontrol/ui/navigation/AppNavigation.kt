package ru.hepolise.volumekeytrackcontrol.ui.navigation

import android.content.SharedPreferences
import android.os.Vibrator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.hepolise.volumekeytrackcontrol.ui.screen.AppFilterScreen
import ru.hepolise.volumekeytrackcontrol.ui.screen.SettingsScreen
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil

@Composable
fun AppNavigation(settingsPrefs: SharedPreferences?, vibrator: Vibrator) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable(
            route = "main",
        ) {
            SettingsScreen(
                settingsPrefs = settingsPrefs,
                navController = navController,
                vibrator = vibrator
            )
        }

        settingsPrefs?.also { sharedPreferences ->
            composable(
                route = "appFilter/{filterType}",
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = 300)
                    )
                }
            ) { backStackEntry ->
                val filterType = SharedPreferencesUtil.AppFilterType.fromKey(
                    backStackEntry.arguments?.getString("filterType")
                )
                AppFilterScreen(
                    filterType = filterType,
                    sharedPreferences = sharedPreferences,
                    navController = navController
                )
            }
        }
    }
}