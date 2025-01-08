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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Switch
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
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_PREDEFINED_MODE_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLongPressDuration
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getSelectedEffect
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationLength
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isVibrationModePredefined
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.PredefinedEffects
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.getVibrator
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.triggerVibration
import ru.hepolise.volumekeytrackcontrolmodule.R
import kotlin.system.exitProcess

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    var isVibrationModePredefined by remember { mutableStateOf(sharedPreferences.isVibrationModePredefined()) }
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
                valueRange = 100f..1000f,
                onValueChangeFinished = {
                    sharedPreferences.edit().putLong(LONG_PRESS_DURATION, longPressDuration).apply()
                },
                modifier = Modifier.widthIn(max = 300.dp)
            )
            Text(stringResource(R.string.long_press_duration, longPressDuration))

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = stringResource(R.string.vibration_settings), fontSize = 20.sp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.widthIn(min = 150.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (isVibrationModePredefined) {
                                    stringResource(id = R.string.predefined_vibration)
                                } else {
                                    stringResource(id = R.string.manual_vibration)
                                },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clickable {
                                        isVibrationModePredefined = !isVibrationModePredefined
                                        sharedPreferences
                                            .edit()
                                            .putBoolean(VIBRATION_MODE, isVibrationModePredefined)
                                            .apply()
                                    }
                            )
                        }
                        Switch(
                            checked = isVibrationModePredefined,
                            onCheckedChange = {
                                isVibrationModePredefined = it
                                sharedPreferences.edit().putBoolean(VIBRATION_MODE, it).apply()
                            }
                        )
                    }
                }

                if (isVibrationModePredefined && PredefinedEffects.isNotEmpty()) {
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
                                        sharedPreferences.edit().putInt(SELECTED_EFFECT, index)
                                            .apply()
                                        effectExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else if (!isVibrationModePredefined) {
                    Slider(
                        value = vibrationLength.toFloat(),
                        onValueChange = {
                            vibrationLength = it.toLong()
                        },
                        valueRange = 10f..500f,
                        onValueChangeFinished = {
                            sharedPreferences.edit().putLong(VIBRATION_LENGTH, vibrationLength)
                                .apply()
                        },
                        modifier = Modifier.widthIn(max = 300.dp)
                    )
                    Text(stringResource(R.string.vibration_length, vibrationLength))
                }
            }

            Button(onClick = {
                vibrator?.triggerVibration(sharedPreferences)
            }) {
                Text(stringResource(R.string.test_vibration))
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth()) {

                Spacer(modifier = Modifier.weight(1f))

                Button(onClick = {
                    sharedPreferences.edit().clear().apply()
                    isVibrationModePredefined = VIBRATION_PREDEFINED_MODE_DEFAULT_VALUE
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

                Spacer(modifier = Modifier.width(8.dp))

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
