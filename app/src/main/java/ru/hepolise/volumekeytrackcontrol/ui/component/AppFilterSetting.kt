package ru.hepolise.volumekeytrackcontrol.ui.component

import android.content.SharedPreferences
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.APP_FILTER_TYPE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.AppFilterType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterSetting(
    value: AppFilterType,
    sharedPreferences: SharedPreferences,
    onValueChange: (AppFilterType) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        AppFilterType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                selected = value == type,
                onClick = {
                    onValueChange(type)
                    sharedPreferences.edit {
                        putString(APP_FILTER_TYPE, type.key)
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = AppFilterType.entries.size
                )
            ) {
                Text(text = stringResource(type.resourceId))
            }
        }
    }
}

