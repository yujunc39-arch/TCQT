package com.test.tcqt.features.general

import android.content.Context
import android.view.View
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionProcess
import com.test.tcqt.core.dexkit.DexKitTask
import com.test.tcqt.core.env.loadOrThrow
import com.test.tcqt.core.hook.hookBefore
import com.test.tcqt.core.hook.hookMethodBefore
import com.test.tcqt.core.reflect.allConstructors
import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.base.BaseMatcher

@RegisterAction
object SkipQRLoginWait : Feature(
    key = "skip_qr_login_wait",
    name = "跳过扫码登录等待",
    desc = "扫码登录时跳过倒计时。",
    processes = setOf(
        ActionProcess.MAIN,
        ActionProcess.OPENSDK,
    ),
), DexKitTask {

    override fun install() {
        if (currentProcess == ActionProcess.MAIN) {
            requireClass("skip_qr_login_wait").allConstructors().forEach {
                it.hookBefore { param ->
                    param.args[1] = 0L
                    param.args[2] = 0L
                }
            }
        }

        // 跳过对话框形式的倒计时等待
        if (currentProcess == ActionProcess.OPENSDK) {
            loadOrThrow("com.tencent.mobileqq.utils.DialogUtil").hookMethodBefore(
                "createCountdownDialog",
                Context::class.java,
                String::class.java,
                CharSequence::class.java,
                String::class.java,
                String::class.java,
                Boolean::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                View.OnClickListener::class.java,
                View.OnClickListener::class.java
            ) { param ->
                if (param.args.size == 10 && param.args[6] is Int) {
                    param.args[6] = 0
                }
            }
        }
    }

    override fun getQueryMap(): Map<String, BaseMatcher> = mapOf(
        "skip_qr_login_wait" to FindClass().apply {
            searchPackages("com.tencent.biz.qrcode.activity")
            matcher {
                superClass("android.os.CountDownTimer")
                addFieldForType("com.tencent.biz.qrcode.activity.QRLoginAuthActivity")
                methods {
                    add { name("onFinish") }
                    add { name("onTick") }
                }
            }
        }
    )
}
