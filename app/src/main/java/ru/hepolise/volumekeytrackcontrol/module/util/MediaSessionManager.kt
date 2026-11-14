package ru.hepolise.volumekeytrackcontrol.module.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.UserHandle
import android.view.Display
import android.view.KeyEvent
import java.lang.reflect.Method
import ru.hepolise.volumekeytrackcontrol.util.AppFilterType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getAppFilterType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getApps

class MediaSessionManager(private val context: Context) {
    companion object {
        private const val USER_ALL = -1

        // WindowManagerPolicyConstants.FLAG_INTERACTIVE (hidden)
        // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/view/WindowManagerPolicyConstants.java
        private const val FLAG_INTERACTIVE = 0x20000000

        private val SCREEN_OFF_STATES =
            setOf(Display.STATE_OFF, Display.STATE_DOZE, Display.STATE_DOZE_SUSPEND)
    }

    lateinit var audioManager: AudioManager
        private set
    private lateinit var displayManager: DisplayManager
    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var sessionHelper: Any
    private var mediaControllers: List<MediaController>? = null

    init {
        initManagers()
    }

    private fun initManagers() {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        mediaSessionManager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        sessionHelper = getMediaSessionLegacyHelper()
    }

    private fun getMediaSessionLegacyHelper(): Any {
        val helperClass =
            "android.media.session.MediaSessionLegacyHelper".toClass(context.classLoader)
        val method = helperClass.getMethod("getHelper", Context::class.java)
        return method.invoke(null, context)
            ?: throw NullPointerException("Unable to get MediaSessionLegacyHelper")
    }

    fun refreshControllers() {
        mediaControllers = getActiveControllers()
    }
    
    private val sessionsForUserId by lazy { findActiveSessionsForUser(Int::class.javaPrimitiveType!!) }
    private val sessionsForUserHandle by lazy { findActiveSessionsForUser(UserHandle::class.java) }

    @SuppressLint("BlockedPrivateApi")
    private fun findActiveSessionsForUser(userParamType: Class<*>): Method? = runCatching {
        MediaSessionManager::class.java.getDeclaredMethod(
            "getActiveSessionsForUser",
            ComponentName::class.java,
            userParamType
        ).apply { isAccessible = true }
    }.getOrNull()

    private val userHandleAll: UserHandle? by lazy { findUserHandleAll() }

    @Suppress("UNCHECKED_CAST")
    private fun getActiveControllers(): List<MediaController>? {
        sessionsForUserId?.let { method ->
            runCatching {
                return method.invoke(mediaSessionManager, null, USER_ALL) as List<MediaController>
            }
        }

        val userHandle = userHandleAll
        if (userHandle != null) {
            sessionsForUserHandle?.let { method ->
                runCatching {
                    return method.invoke(mediaSessionManager, null, userHandle) as List<MediaController>
                }
            }
        }

        return try {
            mediaSessionManager.getActiveSessions(null)
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("BlockedPrivateApi")
    private fun findUserHandleAll(): UserHandle? {
        val clazz = UserHandle::class.java
        val candidates: List<() -> Any?> = listOf(
            { clazz.getField("ALL").get(null) },
            { clazz.getConstructor(Int::class.javaPrimitiveType).newInstance(USER_ALL) },
            { clazz.getMethod("of", Int::class.javaPrimitiveType).invoke(null, USER_ALL) }
        )
        for (candidate in candidates) {
            try {
                (candidate() as? UserHandle)?.let { return it }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun getActiveMediaController(prefs: android.content.SharedPreferences): MediaController? {
        val filterType = prefs.getAppFilterType()
        val apps = prefs.getApps(filterType)
        val chosen = mediaControllers
            ?.sortedByDescending { isMusicActive(it) }
            ?.find { controller ->
                when (filterType) {
                    AppFilterType.DISABLED -> true
                    AppFilterType.WHITE_LIST -> controller.packageName in apps
                    AppFilterType.BLACK_LIST -> controller.packageName !in apps
                }
            }
        return chosen
    }

    fun isMusicActive(controller: MediaController?): Boolean {
        return when (controller?.playbackState?.state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_BUFFERING -> true

            else -> false
        }
    }

    fun isDisplayInteractive(policyFlags: Int): Boolean {
        if (policyFlags and FLAG_INTERACTIVE == 0) return false
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return true
        return display.state !in SCREEN_OFF_STATES
    }

    fun describeDisplayState(policyFlags: Int): String {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        return "policyInteractive=${policyFlags and FLAG_INTERACTIVE != 0}, " +
                "defaultDisplay=${display?.displayId}:${display?.state?.let(::displayStateName)}, " +
                "displays=${displayManager.displays.joinToString { "${it.displayId}:${displayStateName(it.state)}" }}"
    }

    private fun displayStateName(state: Int) = when (state) {
        Display.STATE_OFF -> "OFF"
        Display.STATE_ON -> "ON"
        Display.STATE_DOZE -> "DOZE"
        Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND"
        else -> state.toString()
    }

    fun adjustStreamVolume(keyCode: Int, handler: Handler, logger: (String) -> Unit) {
        try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val method = sessionHelper.javaClass.getMethod(
                "sendVolumeKeyEvent",
                KeyEvent::class.java,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(sessionHelper, downEvent, AudioManager.STREAM_MUSIC, false)
            handler.postSafely(20, logger) {
                val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
                method.invoke(sessionHelper, upEvent, AudioManager.STREAM_MUSIC, false)
            }
        } catch (e: Exception) {
            val direction = when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
                else -> AudioManager.ADJUST_LOWER
            }
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        }
    }
}
