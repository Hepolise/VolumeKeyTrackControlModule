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
import android.os.PowerManager
import android.os.UserHandle
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import ru.hepolise.volumekeytrackcontrol.util.AppFilterType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getAppFilterType
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.getApps

class MediaSessionManager(private val context: Context) {
    lateinit var audioManager: AudioManager
        private set
    private lateinit var powerManager: PowerManager
    private lateinit var displayManager: DisplayManager
    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var sessionHelper: Any
    private var mediaControllers: List<MediaController>? = null

    init {
        initManagers()
    }

    private fun initManagers() {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
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
    
    @SuppressLint("BlockedPrivateApi")
    private fun getActiveControllers(): List<MediaController>? {
        try {
            val legacy = MediaSessionManager::class.java.getDeclaredMethod(
                "getActiveSessionsForUser",
                ComponentName::class.java,
                Int::class.javaPrimitiveType
            )
            legacy.isAccessible = true
            val controllers = legacy.invoke(mediaSessionManager, null, USER_ID_ALL)
            @Suppress("UNCHECKED_CAST")
            return controllers as List<MediaController>
        } catch (_: Exception) {
        }

        getUserHandleAll()?.let { userHandle ->
            try {
                val perUser = MediaSessionManager::class.java.getDeclaredMethod(
                    "getActiveSessionsForUser",
                    ComponentName::class.java,
                    UserHandle::class.java
                )
                perUser.isAccessible = true
                val controllers = perUser.invoke(mediaSessionManager, null, userHandle)
                @Suppress("UNCHECKED_CAST")
                return controllers as List<MediaController>
            } catch (_: Exception) {
            }
        }

        return try {
            mediaSessionManager.getActiveSessions(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active media sessions", e)
            null
        }
    }

    @SuppressLint("BlockedPrivateApi")
    private fun getUserHandleAll(): UserHandle? {
        val clazz = UserHandle::class.java
        val candidates: List<() -> Any?> = listOf(
            { clazz.getField("ALL").get(null) },
            { clazz.getConstructor(Int::class.javaPrimitiveType).newInstance(USER_ID_ALL) },
            { clazz.getMethod("of", Int::class.javaPrimitiveType).invoke(null, USER_ID_ALL) }
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

    fun isDisplayInteractive(): Boolean {
        if (!powerManager.isInteractive) return false
        if (displayManager.displays.size > 1) return true
        val display = displayManager.displays[0]
        return display.state !in setOf(
            Display.STATE_OFF,
            Display.STATE_DOZE,
            Display.STATE_DOZE_SUSPEND
        )
    }

    fun adjustStreamVolume(keyCode: Int, handler: Handler) {
        try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val method = sessionHelper.javaClass.getMethod(
                "sendVolumeKeyEvent",
                KeyEvent::class.java,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            method.invoke(sessionHelper, downEvent, AudioManager.STREAM_MUSIC, false)
            handler.postDelayed({
                val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
                method.invoke(sessionHelper, upEvent, AudioManager.STREAM_MUSIC, false)
            }, 20)
        } catch (e: Exception) {
            val direction = when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
                else -> AudioManager.ADJUST_LOWER
            }
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        }
    }

    companion object {
        private const val TAG = "VolumeControl"

        private const val USER_ID_ALL = -1
    }
}
