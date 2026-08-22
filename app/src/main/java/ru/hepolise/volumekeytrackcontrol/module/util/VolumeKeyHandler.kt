package ru.hepolise.volumekeytrackcontrol.module.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.session.MediaController
import android.os.Handler
import android.view.KeyEvent
import ru.hepolise.volumekeytrackcontrol.module.ExecutionContext
import ru.hepolise.volumekeytrackcontrol.module.MediaEvent
import ru.hepolise.volumekeytrackcontrol.util.RewindActionType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLongPressDuration
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getRewindActionType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isAddSecondaryAction
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isSwapButtons
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.getVibrator
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.triggerVibration

class VolumeKeyHandler(
    private val context: Context,
    private val handler: Handler,
    private val stateManager: StateManager,
    private val mediaSessionManager: MediaSessionManager,
    private val prefs: SharedPreferences,
    private val logger: (String) -> Unit
) {
    private val pendingRunnables = mutableMapOf<MediaEvent, Runnable>()

    fun refreshControllers() {
        mediaSessionManager.refreshControllers()
    }

    private fun logDecision(
        title: String,
        keyCode: Int,
        displayInteractive: Boolean,
        audioMode: Int,
        isDownPressed: Boolean,
        isUpPressed: Boolean,
        hasPendingEvent: Boolean,
        controller: MediaController?
    ) {
        logger("======== $title ========")
        logger("audioManager mode: $audioMode, required: ${AudioManager.MODE_NORMAL}")
        logger("keyCode: $keyCode, required: ${KeyEvent.KEYCODE_VOLUME_DOWN} or ${KeyEvent.KEYCODE_VOLUME_UP}")
        logger("displayInteractive: $displayInteractive, required: false")
        logger("isDownPressed: $isDownPressed")
        logger("isUpPressed: $isUpPressed")
        logger("hasPendingEvent: $hasPendingEvent")
        logger("controller: $controller, required: not null")
        logger("packageName: ${controller?.packageName}")
        logger("=========================================")
    }

    fun logInterceptDecision(event: KeyEvent) {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP) return

        val displayInteractive = mediaSessionManager.isDisplayInteractive()
        val audioMode = mediaSessionManager.audioManager.mode
        val isDownPressed = stateManager.isDownPressed
        val isUpPressed = stateManager.isUpPressed
        val hasPendingEvent = stateManager.pendingEventInfo != null
        val controller = mediaSessionManager.getActiveMediaController(prefs)

        logDecision(
            title = "LOG INTERCEPT DECISION",
            keyCode = keyCode,
            displayInteractive = displayInteractive,
            audioMode = audioMode,
            isDownPressed = isDownPressed,
            isUpPressed = isUpPressed,
            hasPendingEvent = hasPendingEvent,
            controller = controller,
        )
    }

    fun shouldIntercept(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN && keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.flags and KeyEvent.FLAG_FROM_SYSTEM == 0) return false

        val displayInteractive = mediaSessionManager.isDisplayInteractive()
        if (displayInteractive) return false

        val audioMode = mediaSessionManager.audioManager.mode
        if (audioMode != AudioManager.MODE_NORMAL) return false

        val isDownPressed = stateManager.isDownPressed
        val isUpPressed = stateManager.isUpPressed
        val hasPendingEvent = stateManager.pendingEventInfo != null

        val controller = mediaSessionManager.getActiveMediaController(prefs)

        logDecision(
            title = "NEED HOOK CHECK",
            keyCode = keyCode,
            displayInteractive = false,
            audioMode = audioMode,
            isDownPressed = isDownPressed,
            isUpPressed = isUpPressed,
            hasPendingEvent = hasPendingEvent,
            controller = controller,
        )

        return controller != null
    }

    fun handleKeyEvent(event: KeyEvent) {
        updateState(event)

        stateManager.pendingEventInfo?.let { pendingInfo ->
            logger("Executing pending event: ${pendingInfo.event::class.simpleName}")
            executeEvent(pendingInfo.event, pendingInfo.isPrimary)
            stateManager.update { pendingEventInfoHolder = PendingEventInfoHolder(null) }
            return
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> onPressed(event)
            KeyEvent.ACTION_UP -> onReleased(event)
        }
    }

    private fun updateState(event: KeyEvent) {
        val pressed = event.action == KeyEvent.ACTION_DOWN
        val isUp = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        stateManager.update {
            if (isUp) isUpPressed = pressed else isDownPressed = pressed
        }
        if (pressed) {
            stateManager.update { isLongPress = false }
        }
        logger("State updated: down=${stateManager.isDownPressed}, up=${stateManager.isUpPressed}, long=${stateManager.isLongPress}")
    }

    private fun onPressed(event: KeyEvent) {
        logger("Volume pressed action received, down: ${stateManager.isDownPressed}, up: ${stateManager.isUpPressed}")

        if (stateManager.isDownPressed && stateManager.isUpPressed) {
            logger("Both buttons pressed, aborting skip")
            abortSkip()
            return
        }

        val controller = mediaSessionManager.getActiveMediaController(prefs)
        if (controller != null && mediaSessionManager.isMusicActive(controller)) {
            logger("Music is active, creating delayed skip")
            val primary = resolvePrimaryEvent(event.keyCode)
            val hasSecondary = getSecondaryEvent(primary) != null
            scheduleEvent(primary, isPrimary = hasSecondary, hasSecondary = hasSecondary)
            getSecondaryEvent(primary)?.let { secondary ->
                logger("Scheduling secondary event: ${secondary::class.simpleName} with delay")
                scheduleEvent(secondary, isPrimary = false, hasSecondary = false, multiplier = 1.4)
            }
        }
        logger("Creating delayed play pause")
        scheduleEvent(MediaEvent.PlayPause, isPrimary = true, hasSecondary = false)
    }

    private fun onReleased(event: KeyEvent) {
        logger("Volume unpressed action received, down: ${stateManager.isDownPressed}, up: ${stateManager.isUpPressed}")
        abortAll()
        val controller = mediaSessionManager.getActiveMediaController(prefs)
        val isMusicActive = controller != null && mediaSessionManager.isMusicActive(controller)
        logger("isMusicActive: $isMusicActive")
        if (!stateManager.isLongPress && isMusicActive) {
            logger("Adjusting stream volume")
            mediaSessionManager.adjustStreamVolume(event.keyCode, handler)
        }
    }

    private fun resolvePrimaryEvent(keyCode: Int): MediaEvent {
        val isUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP
        val swapped = prefs.isSwapButtons()
        val actualIsUp = if (swapped) !isUp else isUp
        val isTrackChange = prefs.getRewindActionType() == RewindActionType.TRACK_CHANGE
        return when {
            isTrackChange && actualIsUp -> MediaEvent.Next
            isTrackChange && !actualIsUp -> MediaEvent.Prev
            !isTrackChange && actualIsUp -> MediaEvent.FastForward
            else -> MediaEvent.Rewind
        }
    }

    private fun getSecondaryEvent(primary: MediaEvent): MediaEvent? {
        if (!prefs.isAddSecondaryAction()) return null
        return when (primary) {
            MediaEvent.Next -> MediaEvent.FastForward
            MediaEvent.Prev -> MediaEvent.Rewind
            MediaEvent.FastForward -> MediaEvent.Next
            MediaEvent.Rewind -> MediaEvent.Prev
            else -> null
        }
    }

    private fun scheduleEvent(
        event: MediaEvent,
        isPrimary: Boolean,
        hasSecondary: Boolean,
        multiplier: Double = 0.0
    ) {
        val delay = (prefs.getLongPressDuration() + multiplier * prefs.getLongPressDuration()
            .toDouble()).toLong()
        val runnable = Runnable {
            onDelayedEvent(event, isPrimary, hasSecondary)
        }
        pendingRunnables[event] = runnable
        handler.postDelayed(runnable, delay)
        logger("Scheduled event ${event::class.simpleName} (isPrimary=$isPrimary, hasSecondary=$hasSecondary) with delay $delay ms")
    }

    private fun onDelayedEvent(event: MediaEvent, isPrimary: Boolean, hasSecondary: Boolean) {
        logger("Delayed event triggered: ${event::class.simpleName}")
        context.getVibrator().triggerVibration(prefs)
        stateManager.update { isLongPress = true }

        if (hasSecondary) {
            logger("Event has secondary, setting as pending")
            stateManager.update {
                pendingEventInfoHolder = PendingEventInfoHolder(PendingEventInfo(event, isPrimary))
            }
            logger(stateManager.toString())
        } else {
            logger("Event has no secondary, executing now")
            executeEvent(event, isPrimary)
        }
    }

    @Synchronized
    private fun executeEvent(event: MediaEvent, isPrimary: Boolean) {
        val controller = mediaSessionManager.getActiveMediaController(prefs)
        if (controller == null) {
            logger("No active controller, skipping event")
            return
        }
        val executionContext = ExecutionContext(
            controller = controller,
            controls = controller.transportControls,
            prefs = prefs,
            logger = logger,
            stateManager = stateManager,
            isPrimary = isPrimary
        )
        logger("Executing event ${event::class.simpleName} with isPrimary=$isPrimary")
        if (event.execute(executionContext)) {
            // TODO
//            runCatching {
//                RemotePrefsHelper.withRemotePrefs(context) {
//                    val count = getLaunchedCount()
//                    edit {
//                        putInt(LAUNCHED_COUNT, count + 1)
//                    }
//                }
//            }
            abortAll()
        }
    }

    private fun abortSkip() {
        logger("Aborting skip")
        abortEvents(MediaEvent.Prev, MediaEvent.Next)
    }

    private fun abortAll() {
        logger("Aborting all")
        stateManager.update { pendingEventInfoHolder = PendingEventInfoHolder(null) }
        pendingRunnables.values.forEach { handler.removeCallbacks(it) }
        pendingRunnables.clear()
    }

    private fun abortEvents(vararg events: MediaEvent) {
        events.forEach { event ->
            pendingRunnables[event]?.let {
                handler.removeCallbacks(it)
                pendingRunnables.remove(event)
                logger("Aborted event ${event::class.simpleName}")
            }
        }
    }
}