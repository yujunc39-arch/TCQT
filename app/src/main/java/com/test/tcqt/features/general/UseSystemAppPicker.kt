package com.test.tcqt.features.general

import android.app.Activity
import android.content.Context
import com.test.tcqt.annotations.RegisterAction
import com.test.tcqt.api.Feature
import com.test.tcqt.core.action.ActionProcess
import com.test.tcqt.core.env.HookEnv.toHostClass
import com.test.tcqt.core.hook.hookReplace
import com.test.tcqt.core.reflect.findMethod
import com.test.tcqt.core.reflect.invoke

@RegisterAction
object UseSystemAppPicker : Feature(
    key = "use_system_app_picker",
    name = "使用系统应用选择器",
    desc = "将文件的“其他应用打开”交给 Android 系统处理。",
    processes = setOf(ActionProcess.MAIN),
) {

    override fun install() {
        val targetClass = FILE_MANAGER_UTIL_IMPL.toHostClass()
        val systemOpenMethod = targetClass.findMethod {
            name = "openFileWithOtherAppWithSystem"
            paramTypes(Context::class.java, String::class.java)
            returnType = void
        }

        targetClass.findMethod {
            name = "openWithOtherApp"
            paramTypes(Activity::class.java, String::class.java)
            returnType = void
        }.hookReplace { param ->
            param.thisObject.invoke(systemOpenMethod, *param.args)
        }
    }

    private const val FILE_MANAGER_UTIL_IMPL =
        "com.tencent.mobileqq.filemanager.api.impl.FileManagerUtilImpl"
}
