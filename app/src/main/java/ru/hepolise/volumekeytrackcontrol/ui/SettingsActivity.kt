package ru.hepolise.volumekeytrackcontrol.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LONG_PRESS_DURATION
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LONG_PRESS_DURATION_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SELECTED_EFFECT
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SELECTED_EFFECT_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SETTINGS_PREFS_NAME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_LENGTH
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_LENGTH_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_MODE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_MODE_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLongPressDuration
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getSelectedEffect
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationLength
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationMode
import ru.hepolise.volumekeytrackcontrol.util.VibrationMode
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.PredefinedEffects
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.getVibrator
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.triggerVibration
import ru.hepolise.volumekeytrackcontrolmodule.R
import kotlin.system.exitProcess

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = dynamicColorScheme(context = this)
            ) {
                VibrationSettingsScreen(vibrator = getVibrator())
            }
        }
    }
}

private val VibrationEffectTitles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    mapOf(
        VibrationEffect.EFFECT_CLICK to R.string.effect_click,
        VibrationEffect.EFFECT_DOUBLE_CLICK to R.string.effect_double_click,
        VibrationEffect.EFFECT_HEAVY_CLICK to R.string.effect_heavy_click,
        VibrationEffect.EFFECT_TICK to R.string.effect_tick
    )
} else emptyMap()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationSettingsScreen(vibrator: Vibrator?) {
    val context = LocalContext.current

    @SuppressLint("WorldReadableFiles") @Suppress("DEPRECATION")
    val sharedPreferences = try {
        context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_WORLD_READABLE)
    } catch (e: SecurityException) {
        Toast.makeText(context, R.string.module_is_not_enabled, Toast.LENGTH_LONG).show()
        // Clear the application stack and exit
        (context as? Activity)?.finishAffinity()
        exitProcess(0)
    }

    val vibrationOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(
            R.string.predefined to 0,
            R.string.manual to 1
        )
    } else emptyList()

    var vibrationMode by remember { mutableStateOf(sharedPreferences.getVibrationMode()) }
    var selectedEffect by remember { mutableIntStateOf(sharedPreferences.getSelectedEffect()) }
    var vibrationLength by remember { mutableLongStateOf(sharedPreferences.getVibrationLength()) }
    var longPressDuration by remember { mutableLongStateOf(sharedPreferences.getLongPressDuration()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(text = stringResource(R.string.long_press_settings), fontSize = 20.sp)

            Slider(
                value = longPressDuration.toFloat(),
                onValueChange = {
                    longPressDuration = it.toLong()
                },
                valueRange = 50f..500f,
                onValueChangeFinished = {
                    sharedPreferences.edit().putLong(LONG_PRESS_DURATION, longPressDuration).apply()
                }
            )
            Text(stringResource(R.string.long_press_duration, longPressDuration))

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = stringResource(R.string.vibration_settings), fontSize = 20.sp)

            if (vibrationOptions.isNotEmpty()) {
                // Vibration Mode Selector
                var vibrationExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = vibrationExpanded,
                    onExpandedChange = { vibrationExpanded = !vibrationExpanded }
                ) {
                    TextField(
                        value = stringResource(vibrationOptions[vibrationMode.mode].first),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = vibrationExpanded)
                        },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = vibrationExpanded,
                        onDismissRequest = { vibrationExpanded = false }) {
                        vibrationOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.first)) },
                                onClick = {
                                    vibrationMode = VibrationMode.fromInt(index)
                                    sharedPreferences.edit().putInt(VIBRATION_MODE, index).apply()
                                    vibrationExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (vibrationMode == VibrationMode.PREDEFINED && PredefinedEffects.isNotEmpty()) {
                var effectExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = effectExpanded,
                    onExpandedChange = { effectExpanded = !effectExpanded }
                ) {
                    TextField(
                        value = stringResource(VibrationEffectTitles[PredefinedEffects[selectedEffect]]!!),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = effectExpanded)
                        },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = effectExpanded,
                        onDismissRequest = { effectExpanded = false }) {
                        PredefinedEffects.forEachIndexed { index, effect ->
                            DropdownMenuItem(
                                text = { Text(stringResource(VibrationEffectTitles[effect]!!)) },
                                onClick = {
                                    selectedEffect = index
                                    sharedPreferences.edit().putInt(SELECTED_EFFECT, index).apply()
                                    effectExpanded = false
                                }
                            )
                        }
                    }
                }
            } else if (vibrationMode == VibrationMode.MANUAL) {
                Slider(
                    value = vibrationLength.toFloat(),
                    onValueChange = {
                        vibrationLength = it.toLong()
                    },
                    valueRange = 10f..500f,
                    onValueChangeFinished = {
                        sharedPreferences.edit().putLong(VIBRATION_LENGTH, vibrationLength).apply()
                    }
                )
                Text(stringResource(R.string.vibration_length, vibrationLength))
            }

            Button(onClick = {
                vibrator?.triggerVibration(sharedPreferences)
            }) {
                Text(stringResource(R.string.test_vibration))
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth()) {

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    sharedPreferences.edit().clear().apply()
                    vibrationMode = VIBRATION_MODE_DEFAULT_VALUE
                    selectedEffect = SELECTED_EFFECT_DEFAULT_VALUE
                    vibrationLength = VIBRATION_LENGTH_DEFAULT_VALUE
                    longPressDuration = LONG_PRESS_DURATION_DEFAULT_VALUE
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_reset),
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text(stringResource(R.string.restore_default))
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data =
                        Uri.parse("https://github.com/Hepolise/VolumeKeyTrackControlModule")
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.about))
                }

                Spacer(modifier = Modifier.width(8.dp))
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

@Preview(showBackground = true)
@Composable
fun PreviewVibrationSettingsScreen() {
    VibrationSettingsScreen(null)
}
