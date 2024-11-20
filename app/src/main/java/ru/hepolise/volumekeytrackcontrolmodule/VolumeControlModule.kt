package ru.hepolise.volumekeytrackcontrolmodule

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import ru.hepolise.volumekeytrackcontrolmodule.VolumeKeyControlModuleHandlers.handleConstructPhoneWindowManager
import ru.hepolise.volumekeytrackcontrolmodule.VolumeKeyControlModuleHandlers.handleInterceptKeyBeforeQueueing
import java.io.Serializable

@Keep
class VolumeControlModule : IXposedHookLoadPackage {

    companion object {
        private const val CLASS_PHONE_WINDOW_MANAGER =
            "com.android.server.policy.PhoneWindowManager"
        private const val CLASS_IWINDOW_MANAGER = "android.view.IWindowManager"
        private const val CLASS_WINDOW_MANAGER_FUNCS =
            "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs"

        private fun log(text: String) =
            LogHelper.log(VolumeControlModule::class.java.simpleName, text)
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "android") {
            return
        }
        init(lpparam.classLoader)
    }

    private val initMethodSignatures = mapOf(
        // Android 14 & 15 signature
        // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r18/services/core/java/com/android/server/policy/PhoneWindowManager.java#2033
        // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/services/core/java/com/android/server/policy/PhoneWindowManager.java#2199
        arrayOf(
            Context::class.java,
            CLASS_WINDOW_MANAGER_FUNCS
        ) to "Using Android 14 or 15 method signature",

        // Android 13 signature
        // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android13-dev/services/core/java/com/android/server/policy/PhoneWindowManager.java#1873
        arrayOf(
            Context::class.java,
            CLASS_IWINDOW_MANAGER,
            CLASS_WINDOW_MANAGER_FUNCS
        ) to "Using Android 13 method signature",

        // HyperOS-specific signature
        arrayOf(
            Context::class.java,
            CLASS_WINDOW_MANAGER_FUNCS,
            CLASS_IWINDOW_MANAGER
        ) to "Using HyperOS-specific method signature"
    )

    private fun init(classLoader: ClassLoader) {
        val foundMethod = initMethodSignatures.any { (params, logMessage) ->
            tryHookMethod(classLoader, params, logMessage)
        }

        if (!foundMethod) {
            log("Method hook failed for init!")
            return
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

    private fun tryHookMethod(
        classLoader: ClassLoader,
        params: Array<Serializable>,
        logMessage: String
    ): Boolean {
        return try {
            XposedHelpers.findAndHookMethod(
                CLASS_PHONE_WINDOW_MANAGER, classLoader, "init",
                *params, handleConstructPhoneWindowManager
            )
            log(logMessage)
            true
        } catch (ignored: NoSuchMethodError) {
            false
        }
    }
}
