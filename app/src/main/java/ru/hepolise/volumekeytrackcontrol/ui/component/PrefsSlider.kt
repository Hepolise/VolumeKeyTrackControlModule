package ru.hepolise.volumekeytrackcontrol.ui.component

import android.content.SharedPreferences
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import ru.hepolise.volumekeytrackcontrol.ui.debounce

@Composable
fun PrefsSlider(
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    prefKey: String,
    sharedPreferences: SharedPreferences,
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
    debounceMillis: Long = 50,
) {
    val scope = rememberCoroutineScope()
    var sliderValue by remember { mutableFloatStateOf(value.toFloat()) }

    val debouncedValue by remember {
        derivedStateOf { sliderValue.toInt() }
    }.debounce(debounceMillis, scope)

    LaunchedEffect(debouncedValue) {
        sharedPreferences.edit {
            putInt(prefKey, debouncedValue)
        }
    }

    Slider(
        value = value.toFloat(),
        onValueChange = {
            sliderValue = it
            onValueChange(it.toInt())
        },
        valueRange = valueRange,
        modifier = modifier.widthIn(max = 300.dp)
    )
}
