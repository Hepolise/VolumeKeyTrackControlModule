package ru.hepolise.volumekeytrackcontrol.util

import ru.hepolise.volumekeytrackcontrolmodule.R

enum class AppFilterType(
    val key: String,
    val resourceId: Int
) {
    DISABLED("disabled", R.string.app_filter_disabled),
    WHITE_LIST("whitelist", R.string.app_filter_white_list),
    BLACK_LIST("blacklist", R.string.app_filter_black_list);

    companion object {
        fun fromKey(key: String?) = entries.find { it.key == key } ?: DISABLED
    }
}