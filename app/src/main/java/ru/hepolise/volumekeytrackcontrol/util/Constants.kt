package ru.hepolise.volumekeytrackcontrol.util

import ru.hepolise.volumekeytrackcontrolmodule.BuildConfig

object Constants {
    const val GITHUB_URL = "https://github.com/Hepolise/VolumeKeyTrackControlModule"
    const val GITHUB_NEW_ISSUE_URL =
        "https://github.com/Hepolise/VolumeKeyTrackControlModule/issues/new/choose"
    const val LSPOSED_GITHUB_URL =
        "https://github.com/JingMatrix/LSPosed/actions/workflows/core.yml"

    const val HOOK_UPDATE = BuildConfig.APPLICATION_ID + ".HOOK_UPDATE"
    const val HOOKED = "hooked"
    const val INCREMENT_LAUNCH_COUNT = "incrementLaunchCount"
}