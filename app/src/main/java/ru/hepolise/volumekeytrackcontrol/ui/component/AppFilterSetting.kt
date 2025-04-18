package ru.hepolise.volumekeytrackcontrol.ui.component

import android.content.SharedPreferences
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.APP_FILTER_TYPE
import ru.hepolise.volumekeytrackcontrolmodule.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterSetting(
    value: SharedPreferencesUtil.AppFilterType,
    sharedPreferences: SharedPreferences,
    onValueChange: (SharedPreferencesUtil.AppFilterType) -> Unit,
    onNavigateToAppFilter: () -> Unit
) {
    Text(text = stringResource(R.string.app_filter), fontSize = 20.sp)

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = stringResource(value.resourceId),
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }) {
            SharedPreferencesUtil.AppFilterType.values.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.resourceId)) },
                    onClick = {
                        onValueChange(type)
                        sharedPreferences.edit {
                            putString(APP_FILTER_TYPE, type.key)
                        }
                        expanded = false
                    }
                )
            }
        }
    }

    if (value == SharedPreferencesUtil.AppFilterType.WhiteList || value == SharedPreferencesUtil.AppFilterType.BlackList) {
        Button(
            onClick = onNavigateToAppFilter,
        ) {
            Text(text = stringResource(R.string.manage_apps, stringResource(value.resourceId)))
        }
    }
}

