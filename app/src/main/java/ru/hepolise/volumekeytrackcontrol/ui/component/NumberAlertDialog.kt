package ru.hepolise.volumekeytrackcontrol.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import ru.hepolise.volumekeytrackcontrolmodule.R

@Composable
fun NumberAlertDialog(
    title: String,
    defaultValue: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Int) -> Unit,
    minValue: Int,
    maxValue: Int
) {
    fun validate(value: Int) = value in minValue..maxValue
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
                    isError = value.toIntOrNull() == null || !validate(value.toInt()),
                    modifier = Modifier.focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(value.toInt())
                },
                enabled = value.toIntOrNull() != null && validate(value.toInt())
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