package ru.hepolise.volumekeytrackcontrol.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppIconViewModel(application: Application) : AndroidViewModel(application) {
    val iconMap = MutableStateFlow<Map<String, Bitmap?>>(emptyMap())

    private val cacheMutex = Mutex()

    fun loadIcon(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            cacheMutex.withLock {
                if (iconMap.value.containsKey(packageName)) return@withLock

                val drawable = try {
                    getApplication<Application>().packageManager
                        .getApplicationInfo(packageName, 0)
                        .loadIcon(getApplication<Application>().packageManager)
                } catch (_: Exception) {
                    null
                }

                val bitmap = drawable?.toBitmap(48, 48)

                iconMap.value = iconMap.value + (packageName to bitmap)
            }
        }
    }

}
