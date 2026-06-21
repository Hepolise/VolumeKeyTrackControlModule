package ru.hepolise.volumekeytrackcontrol.ui

import androidx.compose.runtime.compositionLocalOf
import io.github.libxposed.service.XposedService

val LocalXposedService = compositionLocalOf<XposedService?> { null }