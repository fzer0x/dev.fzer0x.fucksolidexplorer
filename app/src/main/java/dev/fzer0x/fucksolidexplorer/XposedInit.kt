package dev.fzer0x.fucksolidexplorer

import android.content.ContentResolver
import android.provider.Settings
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.UUID

class XposedInit : IXposedHookLoadPackage {
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

        if (lpparam.packageName == "pl.solidexplorer2") {
            val sessionUuid = UUID.randomUUID().toString()
            val fakeAndroidId = sessionUuid.replace("-", "").substring(0, 16)
            
            XposedBridge.log("FuckSolidExplorer: Patching pl.solidexplorer2 [Session: $fakeAndroidId]")

            val licenseClass = "pl.solidexplorer.licensing.SELicenseManager"
            try {
                val clazz = XposedHelpers.findClassIfExists(licenseClass, lpparam.classLoader)
                clazz?.declaredMethods?.forEach { method ->
                    val name = method.name.lowercase()
                    if (method.returnType == Boolean::class.java || method.returnType == java.lang.Boolean.TYPE) {
                        if (name.contains("license") || name.contains("premium") || name.contains("unlocked") || name.contains("fullversion") || name.contains("valid")) {
                            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true))
                        } else if (name.contains("trial") || name.contains("expired") || name.contains("ads")) {
                            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                        }
                    }
                    if (method.returnType == String::class.java && (name.contains("license") || name.contains("status"))) {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                param.result = "UNLOCKED BY fzer0x"
                            }
                        })
                    }
                }
            } catch (e: Throwable) {}

            try {
                val utilsClazz = XposedHelpers.findClassIfExists("pl.solidexplorer.util.Utils", lpparam.classLoader)
                if (utilsClazz != null) {
                    XposedBridge.hookAllMethods(utilsClazz, "getUniqueId", object : XC_MethodReplacement() {
                        override fun replaceHookedMethod(param: MethodHookParam): Any {
                            return sessionUuid
                        }
                    })
                }
            } catch (ignored: Throwable) {}

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
            } catch (ignored: Throwable) {}

            try {
                XposedHelpers.setStaticObjectField(android.os.Build::class.java, "SERIAL", fakeAndroidId)
                XposedHelpers.setStaticObjectField(android.os.Build::class.java, "ID", fakeAndroidId)
            } catch (ignored: Throwable) {}

            listOf("com.google.firebase.analytics.FirebaseAnalytics", "com.google.firebase.crashlytics.FirebaseCrashlytics").forEach { name ->
                try {
                    XposedHelpers.findClassIfExists(name, lpparam.classLoader)?.declaredMethods?.forEach { m ->
                        if (m.returnType.name == "void" && (m.name.startsWith("log") || m.name.startsWith("record"))) {
                            XposedBridge.hookMethod(m, XC_MethodReplacement.DO_NOTHING)
                        }
                    }
                } catch (ignored: Throwable) {}
            }
        }
    }
}
