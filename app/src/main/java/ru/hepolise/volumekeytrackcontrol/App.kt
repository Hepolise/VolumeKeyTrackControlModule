package ru.hepolise.volumekeytrackcontrol

import android.app.Application
import androidx.core.content.edit
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getStatusSharedPreferences
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.Volatile

class App : Application(), XposedServiceHelper.OnServiceListener {

    companion object {
        @Volatile
        var xposedService: XposedService? = null
            private set
        private val serviceStateListeners =
            CopyOnWriteArraySet<ServiceStateListener>()

        private fun dispatchServiceState(
            listener: ServiceStateListener,
            service: XposedService?
        ) {
            if (serviceStateListeners.contains(listener)) {
                listener.onServiceStateChanged(service)
            }
        }

        fun addServiceStateListener(
            listener: ServiceStateListener,
            notifyImmediately: Boolean
        ) {
            serviceStateListeners.add(listener)
            if (notifyImmediately) {
                dispatchServiceState(listener, xposedService)
            }
        }

        fun removeServiceStateListener(listener: ServiceStateListener) {
            serviceStateListeners.remove(listener)
        }
    }

    private fun notifyServiceStateChanged(service: XposedService?) {
        for (listener in serviceStateListeners) {
            dispatchServiceState(listener, service)
        }
    }

    override fun onCreate() {
        super.onCreate()
        migratePreferences()
        XposedServiceHelper.registerListener(this)
    }

    interface ServiceStateListener {
        fun onServiceStateChanged(service: XposedService?)
    }

    override fun onServiceBind(service: XposedService) {
        xposedService = service
        notifyServiceStateChanged(xposedService)
    }

    override fun onServiceDied(service: XposedService) {
        xposedService = null
        notifyServiceStateChanged(xposedService)
    }

    private fun migratePreferences() {
        val prefs = getStatusSharedPreferences()
        val currentVersion = BuildConfig.VERSION_CODE
        val savedVersion = prefs.getInt("app_version", 0)

        if (savedVersion < 19) {
            prefs.edit {
                clear()
                putInt("app_version", currentVersion)
            }
        }
    }
}