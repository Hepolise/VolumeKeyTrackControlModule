package ru.hepolise.volumekeytrackcontrol.ui.component

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LONG_PRESS_DURATION
import ru.hepolise.volumekeytrackcontrolmodule.R

@Composable
fun LongPressSetting(
    longPressDuration: Int,
    sharedPreferences: SharedPreferences,
    onValueChange: (Int) -> Unit
) {
    var showLongPressTimeoutDialog by remember { mutableStateOf(false) }

    PrefsSlider(
        value = longPressDuration,
        onValueChange = { onValueChange(it) },
        valueRange = 100f..1000f,
        prefKey = LONG_PRESS_DURATION,
        sharedPreferences = sharedPreferences
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.long_press_duration, longPressDuration),
            modifier = Modifier.clickable { showLongPressTimeoutDialog = true }
        )
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
                onValueChange(it)
                sharedPreferences.edit { putInt(LONG_PRESS_DURATION, it) }
                showLongPressTimeoutDialog = false
            }
        )
    }
}