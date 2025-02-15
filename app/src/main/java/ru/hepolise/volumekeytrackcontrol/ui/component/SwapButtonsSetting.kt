package ru.hepolise.volumekeytrackcontrol.ui.component

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.IS_SWAP_BUTTONS
import ru.hepolise.volumekeytrackcontrolmodule.R

@Composable
fun SwapButtonsSetting(
    isSwapButtons: Boolean,
    sharedPreferences: SharedPreferences,
    onValueChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = isSwapButtons,
            onCheckedChange = {
                onValueChange(it)
                sharedPreferences.edit().putBoolean(IS_SWAP_BUTTONS, it).apply()
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.swap_buttons),
            modifier = Modifier.clickable {
                onValueChange(!isSwapButtons)
                sharedPreferences.edit().putBoolean(IS_SWAP_BUTTONS, isSwapButtons).apply()
            }
        )
    }
}