package ru.hepolise.volumekeytrackcontrol.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLongPressDuration
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getSelectedEffect
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationLength
import ru.hepolise.volumekeytrackcontrol.util.VibrationType
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

private val VibrationEffectTitles = VibrationType.values.associateWith {
    when (it) {
        VibrationType.Disabled -> R.string.vibration_disabled
        VibrationType.Manual -> R.string.vibration_manual
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                when (it) {
                    VibrationType.Click -> R.string.vibration_effect_click
                    VibrationType.DoubleClick -> R.string.vibration_effect_double_click
                    VibrationType.HeavyClick -> R.string.vibration_effect_heavy_click
                    VibrationType.Tick -> R.string.vibration_effect_tick
                    else -> throw IllegalStateException("Unknown VibrationType: $it")
                }
            } else {
                throw IllegalStateException("VibrationType is not supported on this API level")
            }
        }
    }
}

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

            var showLongPressTimeoutDialog by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.long_press_duration, longPressDuration))
                IconButton(
                    onClick = {
                        showLongPressTimeoutDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit)
                    )
                }
            }

            if (showLongPressTimeoutDialog) {
                NumberAlertDialog(
                    title = stringResource(R.string.long_press_duration_dialog_title),
                    defaultValue = longPressDuration,
                    minValue = 100,
                    maxValue = 1000,
                    onDismissRequest = { showLongPressTimeoutDialog = false },
                    onConfirm = {
                        longPressDuration = it
                        sharedPreferences.edit().putLong(LONG_PRESS_DURATION, it).apply()
                        showLongPressTimeoutDialog = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = stringResource(R.string.vibration_settings), fontSize = 20.sp)

            val vibrationType = VibrationType.values[selectedEffect]
            var effectExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = effectExpanded,
                onExpandedChange = { effectExpanded = !effectExpanded }
            ) {
                TextField(
                    value = stringResource(VibrationEffectTitles[vibrationType]!!),
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
                    VibrationType.values.forEachIndexed { index, effect ->
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

            if (vibrationType == VibrationType.Manual) {
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


                var showManualVibrationLengthDialog by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.vibration_length, vibrationLength))
                    IconButton(
                        onClick = {
                            showManualVibrationLengthDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit)
                        )
                    }
                }

                if (showManualVibrationLengthDialog) {
                    NumberAlertDialog(
                        title = stringResource(R.string.vibration_length_dialog_title),
                        defaultValue = vibrationLength,
                        minValue = 10,
                        maxValue = 500,
                        onDismissRequest = { showManualVibrationLengthDialog = false },
                        onConfirm = {
                            vibrationLength = it
                            sharedPreferences.edit().putLong(VIBRATION_LENGTH, it).apply()
                            showManualVibrationLengthDialog = false
                        }
                    )
                }

            }

            if (vibrationType != VibrationType.Disabled) {
                Button(onClick = {
                    vibrator?.triggerVibration(sharedPreferences)
                }) {
                    Text(stringResource(R.string.test_vibration))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth()) {

                Spacer(modifier = Modifier.weight(1f))

                Button(onClick = {
                    sharedPreferences.edit().clear().apply()
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

@Composable
fun NumberAlertDialog(
    title: String,
    defaultValue: Long,
    onDismissRequest: () -> Unit,
    onConfirm: (Long) -> Unit,
    minValue: Long = 0,
    maxValue: Long = Long.MAX_VALUE,
    validate: (Long) -> Boolean = { it in minValue..maxValue }
) {
    var value by remember { mutableStateOf(defaultValue.toString()) }
    val focusRequester = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.value_in_range, minValue, maxValue)) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    isError = value.toLongOrNull() == null || !validate(value.toLong()),
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(value.toLong())
                },
                enabled = value.toLongOrNull() != null && validate(value.toLong())
            ) {
                Text(text = stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVibrationSettingsScreen() {
    VibrationSettingsScreen(null)
}
