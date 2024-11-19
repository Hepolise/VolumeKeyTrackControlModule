package ru.hepolise.volumekeytrackcontrolmodule

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import ru.hepolise.volumekeytrackcontrolmodule.LogHelper.log
import ru.hepolise.volumekeytrackcontrolmodule.VolumeKeyHandlers.handleConstructPhoneWindowManager
import ru.hepolise.volumekeytrackcontrolmodule.VolumeKeyHandlers.handleInterceptKeyBeforeQueueing
import ru.hepolise.volumekeytrackcontrolmodule.model.HookInfo

@Keep
class VolumeControlModule : IXposedHookLoadPackage {

    companion object {
        private const val CLASS_PHONE_WINDOW_MANAGER =
            "com.android.server.policy.PhoneWindowManager"
        private const val CLASS_IWINDOW_MANAGER = "android.view.IWindowManager"
        private const val CLASS_WINDOW_MANAGER_FUNCS =
            "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs"
    }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "android") {
            return
        }
        init(lpparam.classLoader)
    }

    private fun init(classLoader: ClassLoader) {
        val hookInfoList = listOf(
            // Android 14 & 15 signature
            // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r18/services/core/java/com/android/server/policy/PhoneWindowManager.java#2033
            // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/services/core/java/com/android/server/policy/PhoneWindowManager.java#2199
            HookInfo(
                params = arrayOf(Context::class.java, CLASS_WINDOW_MANAGER_FUNCS),
                logMessage = "Using Android 14 or 15 method signature"
            ),
            // Android 13 signature
            // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android13-dev/services/core/java/com/android/server/policy/PhoneWindowManager.java#1873
            HookInfo(
                params = arrayOf(
                    Context::class.java,
                    CLASS_IWINDOW_MANAGER,
                    CLASS_WINDOW_MANAGER_FUNCS
                ),
                logMessage = "Using Android 13 method signature"
            ),
            // HyperOS-specific signature
            HookInfo(
                params = arrayOf(
                    Context::class.java,
                    CLASS_WINDOW_MANAGER_FUNCS,
                    CLASS_IWINDOW_MANAGER
                ),
                logMessage = "Using HyperOS-specific method signature"
            ),
        )

        var foundMethod = false
        for (hookInfo in hookInfoList) {
            try {
                XposedHelpers.findAndHookMethod(
                    CLASS_PHONE_WINDOW_MANAGER, classLoader, "init",
                    *hookInfo.params, handleConstructPhoneWindowManager
                )
                foundMethod = true
                log(hookInfo.logMessage)
                break
            } catch (ignored: NoSuchMethodError) {
            }
        }
        if (foundMethod) {
            // https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r18/services/core/java/com/android/server/policy/PhoneWindowManager.java#4117
            XposedHelpers.findAndHookMethod(
                CLASS_PHONE_WINDOW_MANAGER,
                classLoader,
                "interceptKeyBeforeQueueing",
                KeyEvent::class.java,
                Int::class.javaPrimitiveType,
                handleInterceptKeyBeforeQueueing
            )
        } else {
            log("Method hook failed for init!")
        }
    }
}
