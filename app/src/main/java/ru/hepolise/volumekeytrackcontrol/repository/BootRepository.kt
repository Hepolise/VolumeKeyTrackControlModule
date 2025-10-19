package ru.hepolise.volumekeytrackcontrol.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import ru.hepolise.volumekeytrackcontrol.util.LSPosedLogger
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.LAST_BOOT_COMPLETED_TIME
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getStatusSharedPreferences
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isBootCompleted
import ru.hepolise.volumekeytrackcontrol.util.StatusSysPropsHelper

class BootRepository private constructor(private val sharedPreferences: SharedPreferences) {

    companion object {
        private var _bootRepository: BootRepository? = null

        fun getBootRepository(context: Context): BootRepository {
            return _bootRepository ?: BootRepository(
                context.getStatusSharedPreferences()
            ).also { _bootRepository = it }
        }
    }


    fun isBootCompleted(): Boolean {
        return sharedPreferences.isBootCompleted()
    }

    fun setBootCompleted() {
        sharedPreferences.edit {
            putLong(LAST_BOOT_COMPLETED_TIME, System.currentTimeMillis())
        }
    }

    fun observeBootCompleted(): Flow<Boolean> = callbackFlow {
        try {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                LSPosedLogger.log("Observer: pref changed: $key")
                if (key == LAST_BOOT_COMPLETED_TIME) {
                    LSPosedLogger.log("Boot completed is changed")
                    launch {
                        delay(5_000)
                    }.invokeOnCompletion {
                        StatusSysPropsHelper.refreshIsHooked()
                        trySend(isBootCompleted())
                    }
                }
            }

            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

            if (isBootCompleted()) trySend(true)

            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        } catch (e: Exception) {
            close(e)
        }
    }
}