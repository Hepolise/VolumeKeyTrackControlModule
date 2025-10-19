package ru.hepolise.volumekeytrackcontrol.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.hepolise.volumekeytrackcontrol.ui.model.AppInfo

class AppFilterViewModel : ViewModel() {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun loadApps(context: Context, refresh: Boolean = false) {
        if (_apps.value.isNotEmpty() && !refresh) return

        _apps.value = emptyList()
        viewModelScope.launch {
            _isRefreshing.value = true
            _apps.value = withContext(Dispatchers.IO) {
                getAllApps(context)
            }
            _isRefreshing.value = false
        }
    }

    private fun getAllApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES)
            .filter { it.applicationInfo != null }
            .map { packageInfo ->
                AppInfo(
                    name = packageInfo.applicationInfo?.loadLabel(packageManager).toString(),
                    packageName = packageInfo.packageName
                )
            }
    }
}