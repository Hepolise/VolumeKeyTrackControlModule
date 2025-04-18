package ru.hepolise.volumekeytrackcontrol.ui.screen

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavController
import ru.hepolise.volumekeytrackcontrol.ui.component.AppFilterSetting
import ru.hepolise.volumekeytrackcontrol.ui.component.LongPressSetting
import ru.hepolise.volumekeytrackcontrol.ui.component.SwapButtonsSetting
import ru.hepolise.volumekeytrackcontrol.ui.component.VibrationEffectSetting
import ru.hepolise.volumekeytrackcontrol.ui.component.VibrationSettingData
import ru.hepolise.volumekeytrackcontrol.util.Constants
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.APP_FILTER_TYPE_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.EFFECT_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.IS_SWAP_BUTTONS_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LONG_PRESS_DURATION_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SETTINGS_PREFS_NAME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_AMPLITUDE_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.VIBRATION_LENGTH_DEFAULT_VALUE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getAppFilterType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLongPressDuration
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationAmplitude
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationLength
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getVibrationType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isSwapButtons
import ru.hepolise.volumekeytrackcontrol.util.VibrationType
import ru.hepolise.volumekeytrackcontrolmodule.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController?,
    sharedPreferences: SharedPreferences,
    vibrator: Vibrator?
) {
    val context = LocalContext.current

    var longPressDuration by remember { mutableIntStateOf(sharedPreferences.getLongPressDuration()) }
    var vibrationType by remember { mutableStateOf(sharedPreferences.getVibrationType()) }
    var vibrationLength by remember { mutableIntStateOf(sharedPreferences.getVibrationLength()) }
    var vibrationAmplitude by remember { mutableIntStateOf(sharedPreferences.getVibrationAmplitude()) }
    var isSwapButtons by remember { mutableStateOf(sharedPreferences.isSwapButtons()) }
    var appFilterType by remember { mutableStateOf(sharedPreferences.getAppFilterType()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                LongPressSetting(longPressDuration, sharedPreferences) {
                    longPressDuration = it
                }

                HorizontalDivider(modifier = Modifier.widthIn(max = 300.dp))

                VibrationEffectSetting(
                    value = VibrationSettingData(
                        vibrationType, vibrationLength, vibrationAmplitude
                    ),
                    vibrator = vibrator,
                    sharedPreferences = sharedPreferences
                ) {
                    vibrationType = it.vibrationType
                    vibrationLength = it.vibrationLength
                    vibrationAmplitude = it.vibrationAmplitude
                }

                HorizontalDivider(modifier = Modifier.widthIn(max = 300.dp))

                AppFilterSetting(
                    value = appFilterType,
                    sharedPreferences = sharedPreferences,
                    onValueChange = { newAppFilterType -> appFilterType = newAppFilterType },
                ) {
                    navController?.navigate("appFilter/${appFilterType.key}")
                }

                HorizontalDivider(modifier = Modifier.widthIn(max = 300.dp))

                Text(text = stringResource(R.string.other_settings), fontSize = 20.sp)

                SwapButtonsSetting(
                    isSwapButtons = isSwapButtons,
                    sharedPreferences = sharedPreferences
                ) {
                    isSwapButtons = it
                }

            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(end = 16.dp, bottom = 8.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                var showResetSettingsDialog by remember { mutableStateOf(false) }
                Button(onClick = {
                    showResetSettingsDialog = true
                }) {
                    Text(stringResource(R.string.settings_reset))
                }

                if (showResetSettingsDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetSettingsDialog = false },
                        title = { Text(stringResource(R.string.settings_reset)) },
                        text = { Text(stringResource(R.string.settings_reset_message)) },
                        confirmButton = {
                            Button(onClick = {
                                showResetSettingsDialog = false
                                sharedPreferences.edit { clear() }
                                vibrationType = VibrationType.fromKey(EFFECT_DEFAULT_VALUE)
                                vibrationLength = VIBRATION_LENGTH_DEFAULT_VALUE
                                vibrationAmplitude = VIBRATION_AMPLITUDE_DEFAULT_VALUE
                                longPressDuration = LONG_PRESS_DURATION_DEFAULT_VALUE
                                isSwapButtons = IS_SWAP_BUTTONS_DEFAULT_VALUE
                                appFilterType = SharedPreferencesUtil.AppFilterType.fromKey(
                                    APP_FILTER_TYPE_DEFAULT_VALUE
                                )
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_reset_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Text(stringResource(R.string.yes))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetSettingsDialog = false }) {
                                Text(stringResource(R.string.no))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Constants.GITHUB_URL.toUri()
                    context.startActivity(intent)
                }) {
                    Text(stringResource(R.string.about))
                }

                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingsScreen() {
    SettingsScreen(
        navController = null,
        sharedPreferences = LocalContext.current.getSharedPreferences(
            SETTINGS_PREFS_NAME,
            Context.MODE_PRIVATE
        ),
        vibrator = null
    )
}