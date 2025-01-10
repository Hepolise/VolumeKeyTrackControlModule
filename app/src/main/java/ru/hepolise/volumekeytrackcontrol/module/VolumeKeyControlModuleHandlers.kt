package ru.hepolise.volumekeytrackcontrol.module

import android.content.Context
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
import ru.hepolise.volumekeytrackcontrol.module.util.LogHelper
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getLongPressDuration
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.getVibrator
import ru.hepolise.volumekeytrackcontrol.util.VibratorUtil.triggerVibration


object VolumeKeyControlModuleHandlers {

    private const val VOLUME_UP_LONG_PRESS = "mVolumeUpLongPress"
    private const val VOLUME_DOWN_LONG_PRESS = "mVolumeDownLongPress"
    private const val VOLUME_BOTH_LONG_PRESS = "mVolumeBothLongPress"

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

    private var mediaControllers: List<MediaController>? = null

    private fun log(text: String) = LogHelper.log(VolumeControlModule::class.java.simpleName, text)

    val handleInterceptKeyBeforeQueueing: XC_MethodHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val event = param.args[0] as KeyEvent
            val keyCode = event.keyCode
            initManagers(param)
            if (needHook(keyCode, event)) {
                doHook(keyCode, event, param)
            }
        }
    }

    val handleConstructPhoneWindowManager: XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            mapOf(
                VOLUME_UP_LONG_PRESS to Runnable {
                    log("sending next")
                    isLongPress = true
                    sendMediaButtonEventAndTriggerVibration(KeyEvent.KEYCODE_MEDIA_NEXT)
                },
                VOLUME_DOWN_LONG_PRESS to Runnable {
                    log("sending prev")
                    isLongPress = true
                    sendMediaButtonEventAndTriggerVibration(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                },
                VOLUME_BOTH_LONG_PRESS to Runnable {
                    if (isUpPressed && isDownPressed) {
                        log("sending play/pause")
                        isLongPress = true
                        getActiveMediaController()?.transportControls?.also {
                            sendMediaButtonEventAndTriggerVibration(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                        }
                    } else {
                        log("NOT sending play/pause, down: $isDownPressed, up: $isUpPressed")
                    }
                },
            ).forEach { (key, runnable) ->
                XposedHelpers.setAdditionalInstanceField(param.thisObject, key, runnable)
            }
        }
    }

    private fun needHook(keyCode: Int, event: KeyEvent): Boolean {
        log("========")
        log("current audio manager mode: ${audioManager.mode}, required: ${AudioManager.MODE_NORMAL}")
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
        log("displays count: ${displayManager.displays.size}")
        // TODO
        if (displayManager.displays.size > 1) {
            return true
        }
        val display = displayManager.displays[0]
        val disabledStates =
            listOf(Display.STATE_OFF, Display.STATE_DOZE, Display.STATE_DOZE_SUSPEND)
        log("checking display: ${display.displayId}, state: ${display.state}, required: $disabledStates")
        return !disabledStates.contains(display.state)
    }

    private fun initManagers(param: MethodHookParam) {
        val context = param.thisObject.getContext()
        with(context) {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                ?: throw NullPointerException("Unable to obtain audio service")
            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager?
                ?: throw NullPointerException("Unable to obtain power service")
            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
                ?: throw NullPointerException("Unable to obtain display service")
            vibrator = context.getVibrator()
        }

        val mediaSessionHelperClass = XposedHelpers.findClass(
            CLASS_MEDIA_SESSION_LEGACY_HELPER,
            param.thisObject.javaClass.classLoader
        )
        val helper = XposedHelpers.callStaticMethod(mediaSessionHelperClass, "getHelper", context)
        val mSessionManager = XposedHelpers.getObjectField(helper, "mSessionManager")
        val componentNameClass =
            XposedHelpers.findClass(CLASS_COMPONENT_NAME, param.thisObject.javaClass.classLoader)

        @Suppress("UNCHECKED_CAST")
        mediaControllers = XposedHelpers.callMethod(
            mSessionManager,
            "getActiveSessions",
            arrayOf(componentNameClass),
            null
        ) as List<MediaController>?
    }

    private fun doHook(keyCode: Int, event: KeyEvent, param: MethodHookParam) {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> handleDownAction(keyCode, param)
            KeyEvent.ACTION_UP -> handleUpAction(keyCode, param)
        }
        param.setResult(0)
    }

    private fun handleDownAction(keyCode: Int, param: MethodHookParam) {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> isDownPressed = true
            KeyEvent.KEYCODE_VOLUME_UP -> isUpPressed = true
        }
        log("down action received, down: $isDownPressed, up: $isUpPressed")
        isLongPress = false
        if (isUpPressed && isDownPressed) {
            log("aborting delayed skip")
            handleVolumeSkipPressAbort(param.thisObject)
        } else {
            // only one button pressed
            if (isMusicActive()) {
                log("music is active, creating delayed skip")
                handleVolumeSkipPress(param.thisObject, keyCode)
            }
            log("creating delayed play pause")
            handleVolumePlayPausePress(param.thisObject)
        }
    }

    private fun handleUpAction(keyCode: Int, param: MethodHookParam) {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> isDownPressed = false
            KeyEvent.KEYCODE_VOLUME_UP -> isUpPressed = false
        }
        log("up action received, down: $isDownPressed, up: $isUpPressed")
        handleVolumeAllPressAbort(param.thisObject)
        if (!isLongPress && isMusicActive()) {
            log("adjusting music volume")
            val direction = when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
                KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
                else -> return
            }
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        }
    }

    private fun hasActiveMediaController() = getActiveMediaController() != null

    private fun getActiveMediaController(): MediaController? {
        return mediaControllers?.firstOrNull()?.also { log("chosen media controller: ${it.packageName}") }
    }

    private fun isMusicActive() = getActiveMediaController()?.let {
        when (it.playbackState?.state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_BUFFERING -> true
            else -> false
        }
    } ?: false

    private fun sendMediaButtonEventAndTriggerVibration(keyCode: Int) {
        getActiveMediaController()?.transportControls?.also { controls ->
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    if (isMusicActive()) controls.pause() else controls.play()
                }

                KeyEvent.KEYCODE_MEDIA_NEXT -> controls.skipToNext()
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> controls.skipToPrevious()
                else -> return
            }
            vibrator.triggerVibration()
        }
    }

    private fun handleVolumePlayPausePress(instance: Any) {
        val handler = instance.getHandler()
        handler.postDelayed(
            getRunnable(instance, VOLUME_BOTH_LONG_PRESS),
            SharedPreferencesUtil.prefs().getLongPressDuration().toLong()
        )
    }

    private fun handleVolumeSkipPress(instance: Any, keyCode: Int) {
        val handler = instance.getHandler()
        handler.postDelayed(
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> getRunnable(instance, VOLUME_UP_LONG_PRESS)
                KeyEvent.KEYCODE_VOLUME_DOWN -> getRunnable(instance, VOLUME_DOWN_LONG_PRESS)
                else -> return
            },
            SharedPreferencesUtil.prefs().getLongPressDuration().toLong()
        )
    }

    private fun handleVolumeSkipPressAbort(instance: Any) {
        log("aborting skip")
        val handler = instance.getHandler()
        listOf(VOLUME_UP_LONG_PRESS, VOLUME_DOWN_LONG_PRESS).forEach {
            handler.removeCallbacks(getRunnable(instance, it))
        }
    }

    private fun handleVolumePlayPausePressAbort(instance: Any) {
        log("aborting play/pause")
        val handler = instance.getHandler()
        handler.removeCallbacks(getRunnable(instance, VOLUME_BOTH_LONG_PRESS))
    }

    private fun handleVolumeAllPressAbort(phoneWindowManager: Any) {
        log("aborting all")
        handleVolumePlayPausePressAbort(phoneWindowManager)
        handleVolumeSkipPressAbort(phoneWindowManager)
    }

    private fun getRunnable(instance: Any, fieldName: String): Runnable {
        return XposedHelpers.getAdditionalInstanceField(instance, fieldName) as Runnable
    }

    private fun Any.getContext(): Context {
        return getObjectField("mContext") as Context
    }

    private fun Any.getHandler(): Handler {
        return getObjectField("mHandler") as Handler
    }

    private fun Any.getObjectField(fieldName: String): Any {
        return XposedHelpers.getObjectField(this, fieldName)
    }
}
