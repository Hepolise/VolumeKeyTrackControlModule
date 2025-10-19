package ru.hepolise.volumekeytrackcontrol.provider

import com.crossbowffs.remotepreferences.RemotePreferenceFile
import com.crossbowffs.remotepreferences.RemotePreferenceProvider
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil
import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

class RemotePrefProvider : RemotePreferenceProvider(
    BuildConfig.APPLICATION_ID,
    arrayOf(
        RemotePreferenceFile(
            SharedPreferencesUtil.STATUS_PREFS,
            true
        )
    )
)