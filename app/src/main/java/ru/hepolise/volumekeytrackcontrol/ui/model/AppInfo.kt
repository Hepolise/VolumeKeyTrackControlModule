package ru.hepolise.volumekeytrackcontrol.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppInfo(
    val name: String,
    val packageName: String
)