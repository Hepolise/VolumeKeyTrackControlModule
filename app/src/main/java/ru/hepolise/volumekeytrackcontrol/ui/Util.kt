package ru.hepolise.volumekeytrackcontrol.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
fun <T> State<T>.debounce(
    durationMillis: Long,
    coroutineScope: CoroutineScope
): State<T> {
    val debouncedState = mutableStateOf(this.value)
    coroutineScope.launch {
        snapshotFlow { this@debounce.value }
            .debounce(durationMillis)
            .collect { debouncedState.value = it }
    }
    return debouncedState
}

private fun Context.getAppUpdateTime(): Long {
    return try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.lastUpdateTime
    } catch (e: PackageManager.NameNotFoundException) {
        -1
    }
}

fun Context.isInstalledAfterReboot(): Boolean {
    val installTime = getAppUpdateTime()
    val bootTime = System.currentTimeMillis() - SystemClock.elapsedRealtime()
    return installTime > bootTime
}