package ru.hepolise.volumekeytrackcontrol.module

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.PowerManager
import android.os.Vibrator
import android.view.Display
import android.view.KeyEvent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers
import ru.hepolise.volumekeytrackcontrol.module.util.HookNotifier
import ru.hepolise.volumekeytrackcontrol.module.util.LogHelper
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getAppFilterType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getApps
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLongPressDuration
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.isSwapButtons
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.getVibrator
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.triggerVibration


object VolumeKeyControlModuleHandlers {
    private const val CLASS_MEDIA_SESSION_LEGACY_HELPER =
        "android.media.session.MediaSessionLegacyHelper"
    private const val CLASS_COMPONENT_NAME = "android.content.ComponentName"

    private var isLongPress = false
    private var isDownPressed = false
    private var isUpPressed = false

    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private lateinit var displayManager: DisplayManager
    private lateinit var vibrator: Vibrator

    private var prefs: SharedPreferences? = null

    private var mediaControllers: List<MediaController>? = null

    private fun log(text: String) = LogHelper.log(VolumeControlModule::class.java.simpleName, text)

    val handleInterceptKeyBeforeQueueing: XC_MethodHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            initPrefs()
            with(param) {
                val event = args[0] as KeyEvent
                try {
                    with(getContext()) {
                        initManagers()
                        initControllers()
                    }
                } catch (t: Throwable) {
                    log("init failed")
                    t.printStackTrace()
                    return
                }
                if (needHook(event)) {
                    doHook(event)
                }
            }
        }
    }

    val handleConstructPhoneWindowManager: XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            log("handleConstructPhoneWindowManager: initialized")
            val context = param.getContext()
            MediaEvent.entries.forEach { event ->
                val runnable = Runnable { event.handle(context) }
                XposedHelpers.setAdditionalInstanceField(param.thisObject, event.field, runnable)
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BOOT_COMPLETED)
                addAction(Intent.ACTION_USER_UNLOCKED)
            }
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    log("onReceive: ${intent.action}")
                    HookNotifier.notifyHooked(context)
                    ctx.unregisterReceiver(this)
                }
            }, filter)
        }
    }

    private fun needHook(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        log("========")
        log("audioManager mode: ${audioManager.mode}, required: ${AudioManager.MODE_NORMAL}")
        log("keyCode: ${keyCode}, required: ${KeyEvent.KEYCODE_VOLUME_DOWN} or ${KeyEvent.KEYCODE_VOLUME_UP}")
        log("displayInteractive: ${isDisplayInteractive()}, required: false")
        log("isDownPressed: $isDownPressed")
        log("isUpPressed: $isUpPressed")
        log("hasActiveMediaController: ${hasActiveMediaController()}, required: true")
        val needHook =
            (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                    && event.flags and KeyEvent.FLAG_FROM_SYSTEM != 0
                    && (!isDisplayInteractive() || isDownPressed || isUpPressed)
                    && audioManager.mode == AudioManager.MODE_NORMAL
                    && hasActiveMediaController()
        log("needHook: $needHook")
        return needHook
    }

    private fun isDisplayInteractive(): Boolean {
        log("powerManager.isInteractive: ${powerManager.isInteractive}")
        if (!powerManager.isInteractive) {
            return false
        }
        log("Displays count: ${displayManager.displays.size}")
        // TODO
        if (displayManager.displays.size > 1) {
            return true
        }
        val display = displayManager.displays[0]
        val disabledStates =
            listOf(Display.STATE_OFF, Display.STATE_DOZE, Display.STATE_DOZE_SUSPEND)
        log("Checking display: ${display.displayId}, state: ${display.state}, required: $disabledStates")
        return !disabledStates.contains(display.state)
    }

    private fun Context.initManagers() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            ?: throw NullPointerException("Unable to obtain audio service")
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager?
            ?: throw NullPointerException("Unable to obtain power service")
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
            ?: throw NullPointerException("Unable to obtain display service")
        vibrator = getVibrator()
    }

    private fun initPrefs() {
        prefs = SharedPreferencesUtil.prefs()
    }

    private fun Context.initControllers() {
        val context = this
        val classLoader = javaClass.classLoader
        val mediaSessionHelperClass = XposedHelpers.findClass(
            CLASS_MEDIA_SESSION_LEGACY_HELPER,
            classLoader
        )
        val helper = XposedHelpers.callStaticMethod(
            mediaSessionHelperClass,
            "getHelper",
            context
        )
        val mSessionManager = XposedHelpers.getObjectField(helper, "mSessionManager")
        val componentNameClass =
            XposedHelpers.findClass(CLASS_COMPONENT_NAME, classLoader)

        @Suppress("UNCHECKED_CAST")
        mediaControllers = XposedHelpers.callMethod(
            mSessionManager,
            "getActiveSessions",
            arrayOf(componentNameClass),
            null
        ) as List<MediaController>?
    }

    private fun MethodHookParam.doHook(event: KeyEvent) {
        val action = Action.entries.single { it.actionCode == event.action }
        val keyHelper = KeyHelper(event.keyCode)
        keyHelper.updateFlags(action)
        when (action) {
            Action.PRESSED -> handlePressedAction(keyHelper)
            Action.UNPRESSED -> handleUnpressedAction(keyHelper)
        }
        setResult(0)
    }

    private fun MethodHookParam.handlePressedAction(keyHelper: KeyHelper) {
        log("Volume pressed action received, down: $isDownPressed, up: $isUpPressed")
        isLongPress = false
        if (isUpPressed && isDownPressed) {
            log("Aborting delayed skip")
            abortSkip()
        } else {
            if (getMediaController().isMusicActive()) {
                log("Music is active, creating delayed skip")
                delay(keyHelper.mediaEvent)
            }
            log("Creating delayed play pause")
            delay(MediaEvent.PlayPause)
        }
    }

    private fun MethodHookParam.handleUnpressedAction(keyHelper: KeyHelper) {
        log("Volume unpressed action received, down: $isDownPressed, up: $isUpPressed")
        abortAll()
        if (!isLongPress && getMediaController().isMusicActive()) {
            log("Adjusting music volume")
            keyHelper.adjustStreamVolume(audioManager)
        }
    }

    private fun hasActiveMediaController(): Boolean {
        val first = getFirstMediaController()
        val active = getMediaController()
        if (first != active && first != null) {
            return !first.isMusicActive()
        }
        return active != null
    }

    private fun getFirstMediaController(): MediaController? {
        return mediaControllers?.firstOrNull()
            ?.also { log("First media controller: ${it.packageName}") }
    }

    private fun getMediaController(): MediaController? {
        val filterType = prefs.getAppFilterType()
        val apps = prefs.getApps(filterType)
        return mediaControllers?.find {
            when (filterType) {
                SharedPreferencesUtil.AppFilterType.DISABLED -> true
                SharedPreferencesUtil.AppFilterType.WHITE_LIST -> it.packageName in apps
                SharedPreferencesUtil.AppFilterType.BLACK_LIST -> it.packageName !in apps
            }
        }?.also { log("Chosen media controller: ${it.packageName}") }
    }

    private fun MediaController?.isMusicActive() = when (this?.playbackState?.state) {
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_BUFFERING -> true

        else -> false
    }

    private fun sendMediaButtonEventAndTriggerVibration(mediaEvent: MediaEvent) {
        getMediaController()?.also { controller ->
            val controls = controller.transportControls
            when (mediaEvent) {
                MediaEvent.PlayPause -> {
                    if (controller.isMusicActive()) controls.pause() else controls.play()
                }

                MediaEvent.Next -> controls.skipToNext()
                MediaEvent.Prev -> controls.skipToPrevious()
            }
            vibrator.triggerVibration()
        }
    }

    private fun MethodHookParam.delay(event: MediaEvent) {
        val handler = getHandler()
        handler.postDelayed(
            getRunnable(event.field),
            prefs.getLongPressDuration().toLong()
        )
    }

    private fun MethodHookParam.abortSkip() {
        log("Aborting skip")
        abortEvents(MediaEvent.Prev, MediaEvent.Next)
    }

    private fun MethodHookParam.abortAll() {
        log("Aborting all")
        abortEvents(*MediaEvent.entries.toTypedArray())
    }

    private fun MethodHookParam.abortEvents(vararg events: MediaEvent) {
        val handler = getHandler()
        events.forEach { event ->
            handler.removeCallbacks(getRunnable(event.field))
        }
    }

    private fun MethodHookParam.getRunnable(fieldName: String) =
        XposedHelpers.getAdditionalInstanceField(thisObject, fieldName) as Runnable

    private fun MethodHookParam.getContext() = getObjectField("mContext") as Context

    private fun MethodHookParam.getHandler() = getObjectField("mHandler") as Handler

    private fun MethodHookParam.getObjectField(fieldName: String) =
        XposedHelpers.getObjectField(thisObject, fieldName)

    private sealed class MediaEvent(val field: String) {

        open fun handle(context: Context) {
            log("Sending ${this::class.simpleName}")
            isLongPress = true
            sendMediaButtonEventAndTriggerVibration(this)
            HookNotifier.incrementLaunchCount(context)
        }

        object PlayPause : MediaEvent("mVolumeBothLongPress") {
            override fun handle(context: Context) {
                if (isUpPressed && isDownPressed) {
                    super.handle(context)
                } else {
                    log("Not sending ${this::class.simpleName}, down: $isDownPressed, up: $isUpPressed")
                }
            }
        }

        object Next : MediaEvent("mVolumeUpLongPress")

        object Prev : MediaEvent("mVolumeDownLongPress")

        companion object {
            val entries = listOf(PlayPause, Next, Prev)
        }
    }


    private class KeyHelper(keyCode: Int) {
        private val origKey = Key.entries.find { it.keyCode == keyCode }!!
        private val isSwap = prefs.isSwapButtons()
        private val key = if (isSwap) {
            when (origKey) {
                Key.UP -> Key.DOWN
                Key.DOWN -> Key.UP
            }
        } else {
            origKey
        }
        val mediaEvent = key.mediaEvent

        fun updateFlags(action: Action) {
            val pressed = action == Action.PRESSED
            when (key) {
                Key.UP -> isUpPressed = pressed
                Key.DOWN -> isDownPressed = pressed
            }
        }

        fun adjustStreamVolume(audioManager: AudioManager) {
            val direction = when (origKey) {
                Key.UP -> AudioManager.ADJUST_RAISE
                Key.DOWN -> AudioManager.ADJUST_LOWER
            }
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        }

        private companion object {
            private enum class Key(val keyCode: Int, val mediaEvent: MediaEvent) {
                UP(KeyEvent.KEYCODE_VOLUME_UP, MediaEvent.Next),
                DOWN(KeyEvent.KEYCODE_VOLUME_DOWN, MediaEvent.Prev),
            }
        }
    }

    private enum class Action(val actionCode: Int) {
        PRESSED(KeyEvent.ACTION_DOWN),
        UNPRESSED(KeyEvent.ACTION_UP);
    }
}
