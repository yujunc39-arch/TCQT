package com.test.tcqt.features.advanced

import android.view.Window
import android.view.WindowManager
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionProcess
import com.test.tcqt.core.hook.hookMethodBefore

@RegisterAction
object FlagSecureBypass : Feature(
    key = "flag_secure_bypass",
    name = "绕过FLAG_SECURE",
    desc = "绕过FlagSecure，允许截图和录屏。",
    processes = setOf(ActionProcess.ALL),
) {


    override fun install() {
        Window::class.java.hookMethodBefore(
            "setFlags",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        ) {
            val flag = it.args[0] as Int
            val mask = it.args[1] as Int
            if ((mask and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                it.args[0] = flag and WindowManager.LayoutParams.FLAG_SECURE.inv()
            }
        }

        Window::class.java.hookMethodBefore(
            "addFlags",
            Int::class.javaPrimitiveType
        ) {
            val flag = it.args[0] as Int
            if ((flag and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                it.args[0] = flag and WindowManager.LayoutParams.FLAG_SECURE.inv()
            }
        }
    }

}
