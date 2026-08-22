package ru.hepolise.volumekeytrackcontrol.module

import android.content.SharedPreferences
import android.media.MediaMetadata
import android.media.session.MediaController
import ru.hepolise.volumekeytrackcontrol.module.util.StateManager
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getRewindDuration
import kotlin.math.max
import kotlin.math.min

sealed class MediaEvent {
    abstract fun execute(context: ExecutionContext): Boolean

    abstract fun canHandle(context: ExecutionContext): Boolean

    object PlayPause : MediaEvent() {
        override fun canHandle(context: ExecutionContext): Boolean {
            val state = context.stateManager
            return state.isUpPressed && state.isDownPressed
        }

        override fun execute(context: ExecutionContext): Boolean {
            if (!canHandle(context)) {
                context.logger("PlayPause canHandle = false, not sending")
                return false
            }
            context.logger("Sending PlayPause")
            if (context.controller.isMusicActive()) {
                context.controls.pause()
            } else {
                context.controls.play()
            }
            return true
        }
    }

    abstract class BaseEvent : MediaEvent() {
        abstract val isUpBtnEvent: Boolean

        override fun canHandle(context: ExecutionContext): Boolean {
            context.logger("canHandle: isPrimary=${context.isPrimary}, event=${if (isUpBtnEvent) "UP" else "DOWN"}, state=${context.stateManager}")
            if (!context.isPrimary) return true
            val state = context.stateManager
            val isPressed = if (isUpBtnEvent) state.isUpPressed else state.isDownPressed
            return !isPressed
        }
    }

    object Next : BaseEvent() {
        override val isUpBtnEvent = true

        override fun execute(context: ExecutionContext): Boolean {
            if (!canHandle(context)) {
                context.logger("Next canHandle = false, not sending")
                return false
            }
            context.logger("Sending Next")
            context.controls.skipToNext()
            return true
        }
    }

    object Prev : BaseEvent() {
        override val isUpBtnEvent = false

        override fun execute(context: ExecutionContext): Boolean {
            if (!canHandle(context)) {
                context.logger("Prev canHandle = false, not sending")
                return false
            }
            context.logger("Sending Prev")
            context.controls.skipToPrevious()
            return true
        }
    }

    object FastForward : BaseEvent() {
        override val isUpBtnEvent = true

        override fun execute(context: ExecutionContext): Boolean {
            if (!canHandle(context)) {
                context.logger("FastForward canHandle = false, not sending")
                return false
            }
            context.logger("Sending FastForward")
            val current = context.controller.playbackState?.position ?: 0L
            val duration = context.controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
                ?: Long.MAX_VALUE
            val newPos = min(current + context.prefs.getRewindDuration() * 1000L, duration)
            context.controls.seekTo(newPos)
            return true
        }
    }

    object Rewind : BaseEvent() {
        override val isUpBtnEvent = false

        override fun execute(context: ExecutionContext): Boolean {
            if (!canHandle(context)) {
                context.logger("Rewind canHandle = false, not sending")
                return false
            }
            context.logger("Sending Rewind")
            val current = context.controller.playbackState?.position ?: 0L
            val newPos = max(current - context.prefs.getRewindDuration() * 1000L, 0L)
            context.controls.seekTo(newPos)
            return true
        }
    }
}

data class ExecutionContext(
    val controller: MediaController,
    val controls: MediaController.TransportControls,
    val prefs: SharedPreferences,
    val logger: (String) -> Unit,
    val stateManager: StateManager,
    val isPrimary: Boolean
)

fun MediaController.isMusicActive(): Boolean {
    return when (playbackState?.state) {
        android.media.session.PlaybackState.STATE_PLAYING,
        android.media.session.PlaybackState.STATE_FAST_FORWARDING,
        android.media.session.PlaybackState.STATE_REWINDING,
        android.media.session.PlaybackState.STATE_BUFFERING -> true

        else -> false
    }
}