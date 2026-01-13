package dev.fzer0x.fucksolidexplorer

import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.net.UnknownHostException
import java.util.UUID

class XposedInit : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "FuckSolidExplorer"
        private const val TARGET_PACKAGE = "pl.solidexplorer2"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "dev.fzer0x.fucksolidexplorer") {
            try {
                XposedHelpers.findAndHookMethod(
                    "dev.fzer0x.fucksolidexplorer.MainActivity",
                    lpparam.classLoader,
                    "isModuleActive",
                    XC_MethodReplacement.returnConstant(true)
                )
            } catch (ignored: Throwable) {}
            return
        }

        if (lpparam.packageName != TARGET_PACKAGE) return

        val sessionUuid = UUID.randomUUID().toString()
        val fakeAndroidId = sessionUuid.replace("-", "").substring(0, 16)
        
        XposedBridge.log("$TAG: Patching pl.solidexplorer2 for Android 10 (SDK 29)")

        try {
            val licenseClass = "pl.solidexplorer.licensing.SELicenseManager"
            val clazz = XposedHelpers.findClassIfExists(licenseClass, lpparam.classLoader)
            clazz?.declaredMethods?.forEach { method ->
                val name = method.name.lowercase()
                if (method.returnType == Boolean::class.java || method.returnType == java.lang.Boolean.TYPE) {
                    if (name.contains("license") || name.contains("premium") || name.contains("unlocked") || 
                        name.contains("fullversion") || name.contains("valid")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                    } else if (name.contains("trial") || name.contains("expired") || name.contains("ads")) {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                    }
                }
                if (method.returnType == String::class.java && (name.contains("license") || name.contains("status"))) {
                    XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = "PREMIUM UNLOCKED BY fzer0x"
                        }
                    })
                }
            }
        } catch (e: Throwable) {
            XposedBridge.log("$TAG: Licensing hook failed: ${e.message}")
        }

        try {
            XposedHelpers.findAndHookMethod(
                Settings.Secure::class.java,
                "getString",
                ContentResolver::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (param.args[1] == Settings.Secure.ANDROID_ID) {
                            param.result = fakeAndroidId
                        }
                    }
                }
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                XposedHelpers.findAndHookMethod(Build::class.java, "getSerial", XC_MethodReplacement.returnConstant(fakeAndroidId))
            }
            
            XposedHelpers.setStaticObjectField(Build::class.java, "SERIAL", fakeAndroidId)
        } catch (ignored: Throwable) {}

        try {
            XposedHelpers.findAndHookMethod(
                "java.net.InetAddress",
                lpparam.classLoader,
                "getAllByName",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val host = param.args[0] as? String ?: return
                        val blocked = listOf("adservice", "googleads", "doubleclick", "firebase", "crashlytics")
                        if (blocked.any { host.contains(it) }) {
                            param.throwable = UnknownHostException("Blocked by FuckSolidExplorer")
                        }
                    }
                }
            )
        } catch (ignored: Throwable) {}
    }
}
