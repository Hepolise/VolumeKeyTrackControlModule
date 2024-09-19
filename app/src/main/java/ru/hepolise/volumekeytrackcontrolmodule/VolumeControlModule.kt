package ru.hepolise.volumekeytrackcontrolmodule

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

@Keep
class VolumeControlModule : IXposedHookLoadPackage {
    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "android") {
            return
        }
        init(lpparam.classLoader)
    }

    private fun init(classLoader: ClassLoader) {
        try {
            // Try the HyperOS-specific signature
            XposedHelpers.findAndHookMethod(
                CLASS_PHONE_WINDOW_MANAGER, classLoader, "init",
                Context::class.java, CLASS_WINDOW_MANAGER_FUNCS, CLASS_IWINDOW_MANAGER,
                handleConstructPhoneWindowManager
            )
            log("Using HyperOS-specific method signature")
        } catch (e1: NoSuchMethodError) {
            try {
                // Try the Android 14 (Upside Down Cake) signature
                // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r18/services/core/java/com/android/server/policy/PhoneWindowManager.java#2033
                XposedHelpers.findAndHookMethod(
                    CLASS_PHONE_WINDOW_MANAGER, classLoader, "init",
                    Context::class.java, CLASS_WINDOW_MANAGER_FUNCS,
                    handleConstructPhoneWindowManager
                )
                log("Using Android 14 method signature")
            } catch (e2: NoSuchMethodError) {
                // Fallback to the Android 13 signature
                // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android13-dev/services/core/java/com/android/server/policy/PhoneWindowManager.java#1873
                XposedHelpers.findAndHookMethod(
                    CLASS_PHONE_WINDOW_MANAGER, classLoader, "init",
                    Context::class.java, CLASS_IWINDOW_MANAGER, CLASS_WINDOW_MANAGER_FUNCS,
                    handleConstructPhoneWindowManager
                )
                log("Using Android 13 method signature")
            }
        }

        // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r18/services/core/java/com/android/server/policy/PhoneWindowManager.java#4117
        XposedHelpers.findAndHookMethod(
            CLASS_PHONE_WINDOW_MANAGER,
            classLoader,
            "interceptKeyBeforeQueueing",
            KeyEvent::class.java,
            Int::class.javaPrimitiveType,
            handleInterceptKeyBeforeQueueing
        )
    }

    companion object {
        private const val CLASS_PHONE_WINDOW_MANAGER =
            "com.android.server.policy.PhoneWindowManager"
        private const val CLASS_IWINDOW_MANAGER = "android.view.IWindowManager"
        private const val CLASS_WINDOW_MANAGER_FUNCS =
            "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs"
        private var mIsLongPress = false
        private var mIsDownPressed = false
        private var mIsUpPressed = false

        //    private static int mButtonsPressed = 0;
        private lateinit var mAudioManager: AudioManager
        private lateinit var mPowerManager: PowerManager
        private fun log(text: String) {
            if (BuildConfig.DEBUG) XposedBridge.log(text)
        }

        private val handleInterceptKeyBeforeQueueing: XC_MethodHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val event = param.args[0] as KeyEvent
                val keyCode = event.keyCode
                initManagers(XposedHelpers.getObjectField(param.thisObject, "mContext") as Context)
                if (needHook(keyCode, event)) {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) mIsDownPressed = true
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) mIsUpPressed = true
                        log("down action received, down: $mIsDownPressed, up: $mIsUpPressed")
                        mIsLongPress = false
                        if (mIsUpPressed && mIsDownPressed) {
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
                    } else {
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) mIsDownPressed = false
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) mIsUpPressed = false
                        log("up action received, down: $mIsDownPressed, up: $mIsUpPressed")
                        handleVolumeAllPressAbort(param.thisObject)
                        if (!mIsLongPress && isMusicActive) {
                            log("adjusting music volume")
                            mAudioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                                0
                            )
                        }
                    }
                    param.setResult(0)
                }
            }
        }
        private val handleConstructPhoneWindowManager: XC_MethodHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val mVolumeUpLongPress = Runnable {
                    log("sending next")
                    mIsLongPress = true
                    sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                }
                val mVolumeDownLongPress = Runnable {
                    log("sending prev")
                    mIsLongPress = true
                    sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                }
                val mVolumeBothLongPress = Runnable {
                    if (mIsUpPressed && mIsDownPressed) {
                        log("sending play/pause")
                        mIsLongPress = true
                        sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    } else {
                        log("NOT sending play/pause, down: $mIsDownPressed, up: $mIsUpPressed")
                    }
                }
                XposedHelpers.setAdditionalInstanceField(
                    param.thisObject,
                    "mVolumeUpLongPress",
                    mVolumeUpLongPress
                )
                XposedHelpers.setAdditionalInstanceField(
                    param.thisObject,
                    "mVolumeDownLongPress",
                    mVolumeDownLongPress
                )
                XposedHelpers.setAdditionalInstanceField(
                    param.thisObject,
                    "mVolumeBothLongPress",
                    mVolumeBothLongPress
                )
            }
        }

        private fun needHook(keyCode: Int, event: KeyEvent): Boolean {
            log("========")
            log("current audio manager mode: ${mAudioManager.mode}, required: ${AudioManager.MODE_NORMAL}")
            log("keyCode: ${keyCode}, required: ${KeyEvent.KEYCODE_VOLUME_DOWN} or ${KeyEvent.KEYCODE_VOLUME_UP}")
            log("!mPowerManager.isInteractive: ${!mPowerManager.isInteractive}, required: true")
            log("mIsDownPressed: ${mIsDownPressed}")
            log("mIsUpPressed: ${mIsUpPressed}")
            log("needHook: ${(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                    && event.flags and KeyEvent.FLAG_FROM_SYSTEM != 0
                    && (!mPowerManager.isInteractive || mIsDownPressed || mIsUpPressed)
                    && mAudioManager.mode == AudioManager.MODE_NORMAL}")
            return (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                    && event.flags and KeyEvent.FLAG_FROM_SYSTEM != 0
                    && (!mPowerManager.isInteractive || mIsDownPressed || mIsUpPressed)
                    && mAudioManager.mode == AudioManager.MODE_NORMAL
        }

        private fun initManagers(ctx: Context) {
            mAudioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
                ?: throw NullPointerException("Unable to obtain audio service")
            mPowerManager = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager?
                ?: throw NullPointerException("Unable to obtain power service")
        }

        private val isMusicActive: Boolean
            get() {
                // check local
                if (mAudioManager.isMusicActive) return true
                // check remote
                try {
                    if (XposedHelpers.callMethod(
                            mAudioManager,
                            "isMusicActiveRemotely"
                        ) as Boolean
                    ) return true
                } catch (t: Throwable) {
                    t.localizedMessage?.let { Log.e("xposed", it) }
                    t.printStackTrace()
                }
                return false
            }

        private fun sendMediaButtonEvent(code: Int) {
            val eventTime = SystemClock.uptimeMillis()
            val keyIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null)
            var keyEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, code, 0)
            keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            dispatchMediaButtonEvent(keyEvent)
            keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP)
            keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            dispatchMediaButtonEvent(keyEvent)
        }

        private fun dispatchMediaButtonEvent(keyEvent: KeyEvent) {
            try {
                mAudioManager.dispatchMediaKeyEvent(keyEvent)
            } catch (t: Throwable) {
                t.localizedMessage?.let { Log.e("xposed", it) }
                t.printStackTrace()
            }
        }

        private fun handleVolumePlayPausePress(phoneWindowManager: Any) {
            val mHandler = XposedHelpers.getObjectField(phoneWindowManager, "mHandler") as Handler
            val mVolumeBothLongPress = XposedHelpers.getAdditionalInstanceField(
                phoneWindowManager,
                "mVolumeBothLongPress"
            ) as Runnable
            mHandler.postDelayed(
                mVolumeBothLongPress,
                ViewConfiguration.getLongPressTimeout().toLong()
            )
        }

        private fun handleVolumeSkipPress(phoneWindowManager: Any, keycode: Int) {
            val mHandler = XposedHelpers.getObjectField(phoneWindowManager, "mHandler") as Handler
            val mVolumeUpLongPress = XposedHelpers.getAdditionalInstanceField(
                phoneWindowManager,
                "mVolumeUpLongPress"
            ) as Runnable
            val mVolumeDownLongPress = XposedHelpers.getAdditionalInstanceField(
                phoneWindowManager,
                "mVolumeDownLongPress"
            ) as Runnable
            mHandler.postDelayed(
                if (keycode == KeyEvent.KEYCODE_VOLUME_UP) mVolumeUpLongPress else mVolumeDownLongPress,
                ViewConfiguration.getLongPressTimeout().toLong()
            )
        }

        private fun handleVolumeSkipPressAbort(phoneWindowManager: Any) {
            log("aborting skip")
            val mHandler = XposedHelpers.getObjectField(phoneWindowManager, "mHandler") as Handler
            val mVolumeUpLongPress = XposedHelpers.getAdditionalInstanceField(
                phoneWindowManager,
                "mVolumeUpLongPress"
            ) as Runnable
            val mVolumeDownLongPress = XposedHelpers.getAdditionalInstanceField(
                phoneWindowManager,
                "mVolumeDownLongPress"
            ) as Runnable
            mHandler.removeCallbacks(mVolumeUpLongPress)
            mHandler.removeCallbacks(mVolumeDownLongPress)
        }

        private fun handleVolumePlayPausePressAbort(phoneWindowManager: Any) {
            log("aborting play/pause")
            val mHandler = XposedHelpers.getObjectField(phoneWindowManager, "mHandler") as Handler
            val mVolumeBothLongPress = XposedHelpers.getAdditionalInstanceField(
                phoneWindowManager,
                "mVolumeBothLongPress"
            ) as Runnable
            mHandler.removeCallbacks(mVolumeBothLongPress)
        }

        private fun handleVolumeAllPressAbort(phoneWindowManager: Any) {
            log("aborting all")
            handleVolumePlayPausePressAbort(phoneWindowManager)
            handleVolumeSkipPressAbort(phoneWindowManager)
        }
    }
}
