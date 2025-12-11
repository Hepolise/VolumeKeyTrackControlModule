package ru.hepolise.volumekeytrackcontrol.ui.component

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.util.RewindActionType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.REWIND_ACTION_TYPE
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.REWIND_DURATION
import ru.hepolise.volumekeytrackcontrolmodule.R

data class RewindSettingData(
    val rewindActionType: RewindActionType,
    val rewindDuration: Int
)

@Composable
fun LongPressActionSetting(
    data: RewindSettingData,
    sharedPreferences: SharedPreferences,
    onValueChange: (RewindSettingData) -> Unit
) {
    val rewindActionType = data.rewindActionType
    val rewindDuration = data.rewindDuration

    var showRewindDurationDialog by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        SingleChoiceSegmentedButtonRow {
            RewindActionType.entries.forEachIndexed { index, actionType ->
                SegmentedButton(
                    selected = rewindActionType == actionType,
                    onClick = {
                        onValueChange(data.copy(rewindActionType = actionType))
                        sharedPreferences.edit {
                            putString(REWIND_ACTION_TYPE, actionType.name)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = RewindActionType.entries.size
                    ),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 140.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = when (actionType) {
                            RewindActionType.TRACK_CHANGE -> stringResource(R.string.track_change)
                            RewindActionType.REWIND -> stringResource(R.string.rewind)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false
                    )
                }
            }
        }
    }

    Box {
        AnimatedVisibility(
            visible = rewindActionType == RewindActionType.REWIND,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrefsSlider(
                    value = rewindDuration,
                    onValueChange = {
                        onValueChange(data.copy(rewindDuration = it))
                    },
                    valueRange = 1f..60f,
                    prefKey = REWIND_DURATION,
                    sharedPreferences = sharedPreferences
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.rewind_duration, rewindDuration),
                        modifier = Modifier.clickable { showRewindDurationDialog = true }
                    )
                    IconButton(
                        onClick = {
                            showRewindDurationDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_rewind_duration)
                        )
                    }
                }
            }
        }
    }

    if (showRewindDurationDialog) {
        NumberAlertDialog(
            title = stringResource(R.string.rewind_duration_dialog_title),
            defaultValue = rewindDuration,
            minValue = 1,
            maxValue = 60,
            onDismissRequest = { showRewindDurationDialog = false },
            onConfirm = {
                onValueChange(data.copy(rewindDuration = it))
                sharedPreferences.edit { putInt(REWIND_DURATION, it) }
                showRewindDurationDialog = false
            }
        )
    }
}