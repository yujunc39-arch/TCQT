package com.test.tcqt.features.internal

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.InfraTask
import com.test.tcqt.core.action.ActionProcess
import com.test.tcqt.core.config.TCQTBrowserInterface
import com.test.tcqt.core.env.HookEnv
import com.test.tcqt.core.env.ModuleComponents
import com.test.tcqt.core.env.PlatformTools
import com.test.tcqt.core.env.Toasts
import com.test.tcqt.core.hook.MethodHookParam
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.sync.ModuleScope
import com.test.tcqt.host.QQInterfaces
import com.tencent.smtt.sdk.WebView
import java.net.URL

@RegisterAction
object WebJsBridge : InfraTask(
    key = "WebJsBridge",
    processes = setOf(ActionProcess.TOOL),
) {

    override fun install() {
        WebView::class.java.getMethod("loadUrl", String::class.java)
            .hookBefore { param ->
                val url = param.args[0] as String

                when {
                    isSettingPageUrl(url) -> handleSettingPageRedirect(param)
                    !PlatformTools.isHostWhitelisted(url) -> injectJavascriptInterface(
                        param,
                        hostApp
                    )
                }
            }
    }

    private fun isSettingPageUrl(url: String): Boolean =
        runCatching { URL(url).host == URL(SETTING_URL).host }.getOrDefault(false)

    private fun handleSettingPageRedirect(param: MethodHookParam) {
        param.result = Unit

        val context = QQInterfaces.topActivity ?: return
        runCatching {
            ModuleScope.launchMain {
                val latestLoader = System.getProperties()["tcqt.module_class_loader"] as? ClassLoader
                    ?: this.javaClass.classLoader
                val settingActivityClass =
                    latestLoader.loadClass(ModuleComponents.SETTINGS_ACTIVITY)
                val intent = Intent(context, settingActivityClass)
                context.startActivity(intent)
                context.finish()
                context.clearTransition()
            }
        }.onFailure {
            Toasts.error("需要重新启动${HookEnv.appName}")
        }
    }

    private fun injectJavascriptInterface(param: MethodHookParam, app: Application) {
        val webView = param.thisObject as WebView
        webView.addJavascriptInterface(TCQTBrowserInterface(app), "TCQTBrowser")
    }

    private fun Activity.clearTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                0, 0
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private const val SETTING_URL = "http://tcqt.qq.com/"
}
