package ru.hepolise.volumekeytrackcontrol.module

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import ru.hepolise.volumekeytrackcontrol.module.util.MediaSessionManager
import ru.hepolise.volumekeytrackcontrol.module.util.StateManager
import ru.hepolise.volumekeytrackcontrol.module.util.VolumeKeyHandler
import ru.hepolise.volumekeytrackcontrol.module.util.getContext
import ru.hepolise.volumekeytrackcontrol.module.util.getHandler
import ru.hepolise.volumekeytrackcontrol.util.SharedPreferencesUtil.SETTINGS_PREFS

class VolumeControlModule : XposedModule() {
    companion object {
        const val TAG = "VolumeControl"

        private const val CLASS_PHONE_WINDOW_MANAGER =
            "com.android.server.policy.PhoneWindowManager"
    }

    private lateinit var stateManager: StateManager
    private lateinit var prefs: android.content.SharedPreferences

    private var interceptHookHandle: XposedInterface.HookHandle? = null

    private data class Runtime(
        val context: Context,
        val handler: Handler,
        val mediaSessionManager: MediaSessionManager,
        val volumeKeyHandler: VolumeKeyHandler
    )

    private var runtime: Runtime? = null

    private fun log(msg: String) = log(Log.INFO, TAG, msg)

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        super.onSystemServerStarting(param)
        log("onSystemServerStarting")
        setupHooks(param.classLoader)
    }

    override fun onHotReloading(param: XposedModuleInterface.HotReloadingParam): Boolean {
        log(Log.INFO, TAG, "onHotReloading")
        return interceptHookHandle != null
    }

    override fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam) {
        log(
            Log.INFO,
            TAG,
            "onHotReloaded: ${param.processName}, ${param.oldHookHandles.size} old hooks"
        )

        for (oldHandle in param.oldHookHandles) {
            val executable = oldHandle.executable
            if (executable.name == "interceptKeyBeforeQueueing") {
                val newHooker = createInterceptHooker()
                interceptHookHandle = oldHandle.replaceHook(newHooker)
                log("Replaced interceptKeyBeforeQueueing hook")
            } else {
                oldHandle.unhook()
            }
        }

        stateManager = StateManager()
        prefs = getRemotePreferences(SETTINGS_PREFS)
    }

    private fun setupHooks(classLoader: ClassLoader) {
        log("Setting up hooks")

        stateManager = StateManager()
        prefs = getRemotePreferences(SETTINGS_PREFS)

        interceptHookHandle = hookInterceptKeyBeforeQueueing(classLoader)
    }

    @SuppressLint("PrivateApi")
    private fun hookInterceptKeyBeforeQueueing(classLoader: ClassLoader): XposedInterface.HookHandle? {
        return try {
            val clazz = Class.forName(CLASS_PHONE_WINDOW_MANAGER, true, classLoader)
            val method = clazz.getDeclaredMethod(
                "interceptKeyBeforeQueueing",
                KeyEvent::class.java,
                Int::class.javaPrimitiveType
            )

            val handle = hook(method).intercept(createInterceptHooker())
            log("Hooked interceptKeyBeforeQueueing")
            handle
        } catch (t: Throwable) {
            log("Failed to hook interceptKeyBeforeQueueing: ${t.message}")
            log(t.stackTraceToString())
            null
        }
    }

    private fun createInterceptHooker(): XposedInterface.Hooker {
        return XposedInterface.Hooker { chain ->
            val event = chain.args[0] as KeyEvent

            val context = try {
                chain.getContext()
            } catch (e: Throwable) {
                log("Failed to get context: ${e.message}")
                log(e.stackTraceToString())
                throw e
            }

            val runtime = chain.getRuntime(context)

            try {
                val volumeKeyHandler = runtime.volumeKeyHandler

                volumeKeyHandler.refreshControllers()
                if (volumeKeyHandler.shouldIntercept(event)) {
                    log("Intercepting key event: ${event.keyCode}")
                    volumeKeyHandler.handleKeyEvent(event)
                    return@Hooker null
                } else {
                    volumeKeyHandler.logInterceptDecision(event)
                }
            } catch (e: Throwable) {
                log("Error handling key event: ${e.message}")
                log(e.stackTraceToString())
            }

            chain.proceed()
        }
    }

    private fun XposedInterface.Chain.getRuntime(context: Context): Runtime {
        runtime?.let { existing ->
            if (existing.context === context) {
                return existing
            }

            log("Context changed, recreating runtime")
        }

        val mediaSessionManager = MediaSessionManager(context)

        val handler = try {
            getHandler()
        } catch (e: Throwable) {
            log("Failed to get handler: ${e.message}")
            log(e.stackTraceToString())
            throw e
        }

        val volumeKeyHandler = VolumeKeyHandler(
            context = context,
            handler = handler,
            stateManager = stateManager,
            mediaSessionManager = mediaSessionManager,
            prefs = prefs,
            logger = ::log
        )

        return Runtime(
            context = context,
            handler = handler,
            mediaSessionManager = mediaSessionManager,
            volumeKeyHandler = volumeKeyHandler
        ).also {
            runtime = it
        }
    }
}