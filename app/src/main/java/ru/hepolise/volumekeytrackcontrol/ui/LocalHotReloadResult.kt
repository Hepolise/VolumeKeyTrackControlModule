package ru.hepolise.volumekeytrackcontrol.ui

import androidx.compose.runtime.compositionLocalOf
import io.github.libxposed.service.HotReloadResult

val LocalHotReloadResult = compositionLocalOf<HotReloadResult.Status?> { null }