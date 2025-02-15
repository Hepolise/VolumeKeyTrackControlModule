package ru.hepolise.volumekeytrackcontrol.ui.component

import android.content.SharedPreferences
import android.os.Build
import android.os.Vibrator
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SELECTED_EFFECT
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_AMPLITUDE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_LENGTH
import ru.hepolise.volumekeytrackcontrol.util.VibrationType
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.triggerVibration
import ru.hepolise.volumekeytrackcontrolmodule.R

data class VibrationSettingData(
    val vibrationType: VibrationType,
    val vibrationLength: Int,
    val vibrationAmplitude: Int
)

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
fun VibrationEffectSetting(
    value: VibrationSettingData,
    vibrator: Vibrator?,
    sharedPreferences: SharedPreferences,
    onValueChange: (VibrationSettingData) -> Unit
) {
    val (vibrationType, vibrationLength, vibrationAmplitude) = value
    Text(text = stringResource(R.string.vibration_settings), fontSize = 20.sp)

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
            VibrationType.values.forEach { effect ->
                DropdownMenuItem(
                    text = { Text(stringResource(VibrationEffectTitles[effect]!!)) },
                    onClick = {
                        onValueChange(value.copy(vibrationType = effect))
                        sharedPreferences.edit().putString(SELECTED_EFFECT, effect.key)
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
                onValueChange(value.copy(vibrationLength = it.toInt()))
            },
            valueRange = 10f..500f,
            onValueChangeFinished = {
                sharedPreferences.edit().putInt(VIBRATION_LENGTH, vibrationLength)
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
                    onValueChange(value.copy(vibrationLength = it))
                    sharedPreferences.edit().putInt(VIBRATION_LENGTH, it).apply()
                    showManualVibrationLengthDialog = false
                }
            )
        }

        Slider(
            value = vibrationAmplitude.toFloat(),
            onValueChange = {
                onValueChange(value.copy(vibrationAmplitude = it.toInt()))
            },
            valueRange = 1f..255f,
            onValueChangeFinished = {
                sharedPreferences.edit().putInt(VIBRATION_AMPLITUDE, vibrationAmplitude)
                    .apply()
            },
            modifier = Modifier.widthIn(max = 300.dp)
        )

        var showVibrationAmplitudeDialog by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.vibration_amplitude, vibrationAmplitude))
            IconButton(
                onClick = {
                    showVibrationAmplitudeDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit)
                )
            }
        }

        if (showVibrationAmplitudeDialog) {
            NumberAlertDialog(
                title = stringResource(R.string.vibration_amplitude_dialog_title),
                defaultValue = vibrationAmplitude,
                minValue = 1,
                maxValue = 255,
                onDismissRequest = { showVibrationAmplitudeDialog = false },
                onConfirm = {
                    onValueChange(value.copy(vibrationAmplitude = it))
                    sharedPreferences.edit().putInt(VIBRATION_AMPLITUDE, it)
                        .apply()
                    showVibrationAmplitudeDialog = false
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
}