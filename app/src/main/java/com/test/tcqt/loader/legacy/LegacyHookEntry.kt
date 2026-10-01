package com.test.tcqt.loader.legacy

import android.util.Log
import androidx.annotation.Keep
import com.test.tcqt.core.action.HookSteps
import com.test.tcqt.core.env.HostTypeEnum
import com.test.tcqt.core.hook.HookEngineManager
import com.test.tcqt.loader.InjectionGuard
import com.test.tcqt.loader.ModuleLoader
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

@Keep
class LegacyHookEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

    private lateinit var modulePath: String

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!InjectionGuard.tryAcquire(InjectionGuard.MODE_XPOSED)) {
            Log.w(
                "TCQT.LegacyHookEntry",
                "Xposed 注入被阻断：已由 Zygisk 模式接管（tcqt.injection.mode=zygisk）"
            )
            return
        }

        if (HostTypeEnum.contain(lpparam.packageName) && lpparam.isFirstApplication) {
            if (HookEngineManager.isInitialized) return
            HookEngineManager.engine = LegacyHookEngine()

            HookSteps.initHandleLoadPackage(lpparam)
            ModuleLoader.initialize(
                lpparam.classLoader,
                modulePath,
                lpparam.packageName,
                lpparam.processName
            )
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        if (!InjectionGuard.tryAcquire(InjectionGuard.MODE_XPOSED)) {
            Log.w(
                "TCQT.LegacyHookEntry",
                "Xposed 注入被阻断：已由 Zygisk 模式接管（tcqt.injection.mode=zygisk）"
            )
            return
        }

        modulePath = startupParam.modulePath
    }
}
