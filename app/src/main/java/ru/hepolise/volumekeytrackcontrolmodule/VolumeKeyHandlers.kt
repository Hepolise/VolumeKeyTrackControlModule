package ru.hepolise.volumekeytrackcontrolmodule

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.view.ViewConfiguration
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers
import ru.hepolise.volumekeytrackcontrolmodule.extension.AudioManagerExtension.sendMediaButtonEvent
import ru.hepolise.volumekeytrackcontrolmodule.extension.VibratorExtension.triggerVibration

object VolumeKeyHandlers {

    private const val VOLUME_UP_LONG_PRESS = "mVolumeUpLongPress"
    private const val VOLUME_DOWN_LONG_PRESS = "mVolumeDownLongPress"
    private const val VOLUME_BOTH_LONG_PRESS = "mVolumeBothLongPress"

    private val TIMEOUT = ViewConfiguration.getLongPressTimeout().toLong()

    private var isLongPress = false
    private var isDownPressed = false
    private var isUpPressed = false

    private lateinit var audioManager: AudioManager
    private lateinit var powerManager: PowerManager
    private lateinit var vibrator: Vibrator

    private fun log(text: String) = LogHelper.log(VolumeControlModule::class.java.simpleName, text)

    val handleInterceptKeyBeforeQueueing: XC_MethodHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val event = param.args[0] as KeyEvent
            val keyCode = event.keyCode
            initManagers(param.thisObject.getObjectField("mContext") as Context)
            if (needHook(keyCode, event)) {
                doHook(keyCode, event, param)
            }
        }
    }

    val handleConstructPhoneWindowManager: XC_MethodHook = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val volumeUpLongPress = Runnable {
                log("sending next")
                KeyEvent::KEYCODE_MEDIA_PREVIOUS.name
                isLongPress = true
                sendMediaButtonEventAndTriggerVibration(KeyEvent.KEYCODE_MEDIA_NEXT)
            }
            val volumeDownLongPress = Runnable {
                log("sending prev")
                isLongPress = true
                sendMediaButtonEventAndTriggerVibration(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
            val volumeBothLongPress = Runnable {
                if (isUpPressed && isDownPressed) {
                    log("sending play/pause")
                    isLongPress = true
                    sendMediaButtonEventAndTriggerVibration(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                } else {
                    log("NOT sending play/pause, down: $isDownPressed, up: $isUpPressed")
                }
            }
            mapOf(
                VOLUME_UP_LONG_PRESS to volumeUpLongPress,
                VOLUME_DOWN_LONG_PRESS to volumeDownLongPress,
                VOLUME_BOTH_LONG_PRESS to volumeBothLongPress,
            ).forEach { (key, runnable) ->
                XposedHelpers.setAdditionalInstanceField(param.thisObject, key, runnable)
            }
        }
    }

    private fun needHook(keyCode: Int, event: KeyEvent): Boolean {
        log("========")
        log("current audio manager mode: ${audioManager.mode}, required: ${AudioManager.MODE_NORMAL}")
        log("keyCode: ${keyCode}, required: ${KeyEvent.KEYCODE_VOLUME_DOWN} or ${KeyEvent.KEYCODE_VOLUME_UP}")
        log("!powerManager.isInteractive: ${!powerManager.isInteractive}, required: true")
        log("isDownPressed: $isDownPressed")
        log("isUpPressed: $isUpPressed")
        val needHook =
            (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                    && event.flags and KeyEvent.FLAG_FROM_SYSTEM != 0
                    && (!powerManager.isInteractive || isDownPressed || isUpPressed)
                    && audioManager.mode == AudioManager.MODE_NORMAL
        log("needHook: $needHook")
        return needHook
    }

    private fun initManagers(ctx: Context) {
        with(ctx) {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                ?: throw NullPointerException("Unable to obtain audio service")
            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager?
                ?: throw NullPointerException("Unable to obtain power service")
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }
    }

    private fun doHook(keyCode: Int, event: KeyEvent, param: MethodHookParam) {
        if (event.action == KeyEvent.ACTION_DOWN) {
            handleDownAction(keyCode, param)
        } else {
            handleUpAction(keyCode, param)
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
            if (isMusicActive) {
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
        if (!isLongPress && isMusicActive) {
            log("adjusting music volume")
            val direction = when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
                KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
                else -> return
            }
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        }
    }

    private val isMusicActive: Boolean
        get() {
            // check local
            if (audioManager.isMusicActive) return true
            // check remote
            try {
                if (XposedHelpers.callMethod(
                        audioManager,
                        "isMusicActiveRemotely"
                    ) as Boolean
                ) return true
            } catch (t: Throwable) {
                t.localizedMessage?.let { Log.e("xposed", it) }
                t.printStackTrace()
            }
            return false
        }

    private fun sendMediaButtonEventAndTriggerVibration(code: Int) {
        audioManager.sendMediaButtonEvent(code)
        vibrator.triggerVibration()
    }

    private fun handleVolumePlayPausePress(instance: Any) {
        val handler = instance.getHandler()
        handler.postDelayed(getRunnable(instance, VOLUME_BOTH_LONG_PRESS), TIMEOUT)
    }

    private fun handleVolumeSkipPress(instance: Any, keyCode: Int) {
        val handler = instance.getHandler()
        handler.postDelayed(
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> getRunnable(instance, VOLUME_UP_LONG_PRESS)
                KeyEvent.KEYCODE_VOLUME_DOWN -> getRunnable(instance, VOLUME_DOWN_LONG_PRESS)
                else -> return
            },
            TIMEOUT
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

    private fun Any.getHandler(): Handler {
        return getObjectField("mHandler") as Handler
    }

    private fun Any.getObjectField(fieldName: String): Any {
        return XposedHelpers.getObjectField(this, fieldName)
    }
}